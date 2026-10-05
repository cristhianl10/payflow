package com.payflow;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.*;
import com.payflow.auth.application.*;
import com.payflow.shared.domain.BusinessException;
import com.payflow.transfer.application.TransferService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "payflow.auth.requests-per-minute=1000")
@ActiveProfiles("test")
@Testcontainers
class CoreIT {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired AuthService auth;
    @Autowired TokenService tokens;
    @Autowired EmailVerificationService emailVerification;
    @Autowired PasswordRecoveryService passwordRecovery;
    @Autowired TransferService transfers;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired PlatformTransactionManager manager;
    @LocalServerPort int port;

    @BeforeEach void reset() {
        jdbc.execute("TRUNCATE users CASCADE");
        jdbc.update("INSERT INTO ledger_accounts(id, purpose) VALUES ('00000000-0000-0000-0000-000000000001', 'SANDBOX_ISSUANCE')");
    }

    @Test void registrationPostsBalancedGrantAndStoresOnlyPasswordAndTokenHashes() {
        var alice = register("alice");
        assertBalance(alice.userId(), "10000.00");
        assertEquals(1, count("users"));
        assertEquals(1, count("wallets"));
        assertEquals(1, count("journal_operations"));
        assertEquals(2, count("ledger_entries"));
        assertEquals("ROLE_USER", jdbc.queryForObject("SELECT role_name FROM user_roles", String.class));
        String hash = jdbc.queryForObject("SELECT password_hash FROM users", String.class);
        assertTrue(hash.startsWith("$2a$12$"));
        assertNotEquals(alice.refreshToken(), jdbc.queryForObject("SELECT token_hash FROM refresh_tokens", String.class));
        assertReconciled();
        var duplicate = assertThrows(BusinessException.class, () -> auth.register("Alice", "Example", "ALICE@example.com", "strong-password-2026"));
        assertEquals(409, duplicate.status());
        assertEquals(1, count("wallets"));
    }

    @Test void emailVerificationUsesHashedExpiringSingleUseTokens() {
        var alice = register("alice");
        assertFalse(jdbc.queryForObject("SELECT email_verified FROM users WHERE id = ?", Boolean.class, alice.userId()));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM email_verification_tokens
                WHERE user_id = ? AND consumed_at IS NULL
                """, Integer.class, alice.userId()));

        jdbc.update("DELETE FROM email_verification_tokens WHERE user_id = ?", alice.userId());
        String rawToken = "integration-test-verification-token";
        jdbc.update("""
                INSERT INTO email_verification_tokens(token_hash, user_id, expires_at)
                VALUES (?, ?, CURRENT_TIMESTAMP + INTERVAL '1 hour')
                """, TokenService.hash(rawToken), alice.userId());

        emailVerification.verify(rawToken);
        assertTrue(jdbc.queryForObject("SELECT email_verified FROM users WHERE id = ?", Boolean.class, alice.userId()));
        assertTrue(jdbc.queryForObject("""
                SELECT consumed_at IS NOT NULL FROM email_verification_tokens WHERE token_hash = ?
                """, Boolean.class, TokenService.hash(rawToken)));
        assertDoesNotThrow(() -> emailVerification.verify(rawToken));

        var invalid = assertThrows(BusinessException.class, () -> emailVerification.verify("unknown-token"));
        assertEquals("INVALID_VERIFICATION_TOKEN", invalid.code());
    }

    @Test void passwordRecoveryUsesSingleUseTokensAndRevokesExistingSessions() {
        var alice = register("alice");
        var secondSession = auth.login("alice@example.com", "strong-password-2026");
        passwordRecovery.request("ALICE@example.com");
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM password_reset_tokens
                WHERE user_id = ? AND consumed_at IS NULL
                """, Integer.class, alice.userId()));

        jdbc.update("DELETE FROM password_reset_tokens WHERE user_id = ?", alice.userId());
        String rawToken = "integration-test-password-reset-token";
        jdbc.update("""
                INSERT INTO password_reset_tokens(token_hash, user_id, expires_at)
                VALUES (?, ?, CURRENT_TIMESTAMP + INTERVAL '30 minutes')
                """, TokenService.hash(rawToken), alice.userId());

        passwordRecovery.reset(rawToken, "new-strong-password-2026");
        assertTrue(jdbc.queryForObject("""
                SELECT consumed_at IS NOT NULL FROM password_reset_tokens WHERE token_hash = ?
                """, Boolean.class, TokenService.hash(rawToken)));
        assertEquals(0, jdbc.queryForObject("""
                SELECT count(*) FROM auth_sessions
                WHERE user_id = ? AND revoked_at IS NULL
                """, Integer.class, alice.userId()));
        assertThrows(TokenService.RejectedSession.class, () -> tokens.refresh(secondSession.refreshToken()));
        assertThrows(BusinessException.class,
                () -> auth.login("alice@example.com", "strong-password-2026"));
        assertDoesNotThrow(() -> auth.login("alice@example.com", "new-strong-password-2026"));

        var reused = assertThrows(BusinessException.class,
                () -> passwordRecovery.reset(rawToken, "another-strong-password-2026"));
        assertEquals("INVALID_PASSWORD_RESET_TOKEN", reused.code());

        passwordRecovery.request("missing@example.com");
        assertEquals(1, count("password_reset_tokens"));
    }

    @Test void atomicTransferAndIdempotencyReturnIdenticalReceipt() throws Exception {
        var alice = register("alice"); register("bob");
        UUID key = UUID.randomUUID();
        String first = send(alice, "bob", "250.00", key);
        String retry = send(alice, "bob", "250.00", key);
        assertEquals(first, retry);
        assertEquals("COMPLETED", json.readTree(first).get("status").asText());
        assertBalance(alice.userId(), "9750.00");
        assertEquals(3, count("journal_operations"));
        assertEquals(6, count("ledger_entries"));
        var conflict = assertThrows(BusinessException.class, () -> send(alice, "bob", "100.00", key));
        assertEquals("IDEMPOTENCY_CONFLICT", conflict.code());
        assertReconciled();
    }

    @Test void invalidTransfersNeverMutateFinancialRecords() {
        var alice = register("alice"); var bob = register("bob");
        for (String amount : List.of("0", "-1", "0.001", "1e2", "1000000000000000", "20000")) {
            assertThrows(BusinessException.class, () -> send(alice, "bob", amount, UUID.randomUUID()));
        }
        assertThrows(BusinessException.class, () -> send(alice, "alice", "1", UUID.randomUUID()));
        assertThrows(BusinessException.class, () -> send(alice, "unknown", "1", UUID.randomUUID()));
        jdbc.update("UPDATE wallets SET status = 'BLOCKED' WHERE user_id = ?", bob.userId());
        assertThrows(BusinessException.class, () -> send(alice, "bob", "1", UUID.randomUUID()));
        assertBalance(alice.userId(), "10000");
        assertEquals(2, count("journal_operations"));
        assertEquals(0, count("transfer_requests"));
        assertReconciled();
    }

    @Test void simultaneousOverspendingAllowsOnlyOneTransfer() throws Exception {
        var alice = register("alice"); register("bob");
        var results = race(List.of(() -> send(alice, "bob", "8000", UUID.randomUUID()),
                () -> send(alice, "bob", "7000", UUID.randomUUID())));
        assertEquals(1, results.stream().filter(result -> result instanceof String).count());
        assertEquals(1, results.stream().filter(result -> result instanceof BusinessException e && e.code().equals("INSUFFICIENT_FUNDS")).count());
        assertEquals(3, count("journal_operations"));
        assertReconciled();
    }

    @Test void duplicateConcurrentRequestsCommitOnlyOnce() throws Exception {
        var alice = register("alice"); register("bob"); UUID key = UUID.randomUUID();
        var results = race(List.of(() -> send(alice, "bob", "500", key), () -> send(alice, "bob", "500", key)));
        assertInstanceOf(String.class, results.get(0));
        assertEquals(results.get(0), results.get(1));
        assertBalance(alice.userId(), "9500");
        assertEquals(1, count("transfer_requests"));
        assertReconciled();
    }

    @Test void expiredIdempotencyReferencesCanBeReusedWithoutChangingThePreviousTransfer() {
        var alice = register("alice"); register("bob"); UUID key = UUID.randomUUID();
        assertDoesNotThrow(() -> send(alice, "bob", "500", key));
        jdbc.update("UPDATE transfer_requests SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second' WHERE user_id = ?", alice.userId());
        assertDoesNotThrow(() -> send(alice, "bob", "300", key));
        assertBalance(alice.userId(), "9200");
        assertEquals(4, count("journal_operations"));
        assertReconciled();
    }

    @Test void opposingTransfersUseStableWalletLockOrder() throws Exception {
        var alice = register("alice"); var bob = register("bob");
        var results = race(List.of(() -> send(alice, "bob", "600", UUID.randomUUID()),
                () -> send(bob, "alice", "200", UUID.randomUUID())));
        results.forEach(result -> assertInstanceOf(String.class, result));
        assertBalance(alice.userId(), "9600");
        assertBalance(bob.userId(), "10400");
        assertReconciled();
    }

    @Test void failureAfterPostingRollsBackBalancesLedgerIdempotencyAndAudit() {
        var alice = register("alice"); register("bob");
        UUID key = UUID.randomUUID();
        jdbc.execute("""
                CREATE FUNCTION fail_transfer_audit() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN IF NEW.event_type = 'TRANSFER_COMPLETED' THEN RAISE EXCEPTION 'Injected test failure'; END IF;
                RETURN NEW; END; $$
                """);
        jdbc.execute("CREATE TRIGGER inject_failure BEFORE INSERT ON audit_logs FOR EACH ROW EXECUTE FUNCTION fail_transfer_audit()");
        try {
            assertThrows(RuntimeException.class, () -> send(alice, "bob", "250", key));
        } finally {
            jdbc.execute("DROP TRIGGER inject_failure ON audit_logs");
            jdbc.execute("DROP FUNCTION fail_transfer_audit()");
        }
        assertBalance(alice.userId(), "10000");
        assertEquals(2, count("journal_operations"));
        assertEquals(4, count("ledger_entries"));
        assertEquals(0, count("transfer_requests"));
        assertDoesNotThrow(() -> send(alice, "bob", "250", key));
        assertReconciled();
    }

    @Test void grantFailureRollsBackRegistration() {
        jdbc.execute("""
                CREATE FUNCTION fail_grant() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN RAISE EXCEPTION 'Injected grant failure'; END; $$
                """);
        jdbc.execute("CREATE TRIGGER inject_failure BEFORE INSERT ON ledger_entries FOR EACH ROW EXECUTE FUNCTION fail_grant()");
        try { assertThrows(RuntimeException.class, () -> register("alice")); }
        finally {
            jdbc.execute("DROP TRIGGER inject_failure ON ledger_entries");
            jdbc.execute("DROP FUNCTION fail_grant()");
        }
        assertEquals(0, count("users"));
        assertEquals(0, count("wallets"));
        assertEquals(0, count("journal_operations"));
        assertEquals(0, count("auth_sessions"));
    }

    @Test void databaseRejectsUnbalancedOperationsAndHistoricalMutation() {
        var alice = register("alice");
        UUID wallet = jdbc.queryForObject("SELECT id FROM wallets WHERE user_id = ?", UUID.class, alice.userId());
        assertThrows(RuntimeException.class, () -> new TransactionTemplate(manager).executeWithoutResult(status ->
                jdbc.update("INSERT INTO journal_operations(id, public_id, kind, receiver_wallet_id, amount) VALUES (?, ?, 'SANDBOX_GRANT', ?, 10)",
                        UUID.randomUUID(), "test-unbalanced", wallet)));
        assertThrows(RuntimeException.class, () -> jdbc.update("UPDATE ledger_entries SET amount = 5"));
        assertThrows(RuntimeException.class, () -> jdbc.update("DELETE FROM journal_operations"));
        assertEquals(1, count("journal_operations"));
        assertReconciled();
    }

    @Test void refreshRotationReplayAndLogoutRevokeTheSession() {
        var initial = register("alice");
        var renewed = tokens.refresh(initial.refreshToken());
        assertNotEquals(initial.refreshToken(), renewed.refreshToken());
        assertThrows(TokenService.RejectedSession.class, () -> tokens.refresh(initial.refreshToken()));
        assertThrows(TokenService.RejectedSession.class, () -> tokens.refresh(renewed.refreshToken()));
        var login = auth.login("alice@example.com", "strong-password-2026");
        tokens.logout(login.refreshToken());
        assertThrows(TokenService.RejectedSession.class, () -> tokens.refresh(login.refreshToken()));
    }

    @Test void httpFlowEnforcesCsrfAuthenticationAndOwnership() throws Exception {
        var browser = new Browser();
        assertEquals(403, browser.request("POST", "/auth/register", Map.of("firstName", "Alice"), null, null).statusCode());
        browser.csrf();
        var registered = browser.request("POST", "/auth/register", Map.of("firstName", "Alice", "lastName", "Example",
                "email", "alice@example.com", "password", "strong-password-2026"), null, null);
        assertEquals(201, registered.statusCode(), registered.body());
        assertTrue(registered.headers().allValues("Set-Cookie").stream().anyMatch(cookie -> cookie.contains("payflow_refresh=") && cookie.contains("HttpOnly")));
        JsonNode body = json.readTree(registered.body());
        assertFalse(body.has("refreshToken"));
        String access = body.get("accessToken").asText();
        assertEquals(200, browser.request("GET", "/wallets/me", null, access, null).statusCode());
        assertEquals(401, browser.request("GET", "/wallets/me", null, null, null).statusCode());
        assertEquals(401, browser.request("GET", "/wallets/me", null, access + "x", null).statusCode());
        var bob = register("bob");
        String bobWallet = jdbc.queryForObject("SELECT public_id FROM wallets WHERE user_id = ?", String.class, bob.userId());
        assertEquals(404, browser.request("GET", "/wallets/" + bobWallet, null, access, null).statusCode());
        assertEquals(403, browser.request("GET", "/admin/users", null, access, null).statusCode());
        UUID key = UUID.randomUUID();
        Map<String, String> transfer = Map.of("recipient", "bob@example.com", "amount", "125.00", "currency", "USD", "description", "Lunch");
        var receipt = browser.request("POST", "/transfers", transfer, access, key);
        assertEquals(201, receipt.statusCode(), receipt.body());
        assertEquals(receipt.body(), browser.request("POST", "/transfers", transfer, access, key).body());
        String reference = json.readTree(receipt.body()).get("publicId").asText();
        assertEquals(200, browser.request("GET", "/transactions/" + reference, null, access, null).statusCode());
        var outsider = register("outsider");
        assertEquals(404, browser.request("GET", "/transactions/" + reference, null, outsider.accessToken(), null).statusCode());
        assertEquals(400, browser.request("GET", "/transactions?size=1000", null, access, null).statusCode());
        // Switching the bearer identity above can invalidate the previous CSRF token.
        browser.csrf();
        assertEquals(204, browser.request("POST", "/auth/logout", null, null, null).statusCode());
        assertEquals(401, browser.request("GET", "/wallets/me", null, access, null).statusCode());
        assertReconciled();
    }

    private TokenService.Tokens register(String name) { return auth.register(name, "Example", name + "@example.com", "strong-password-2026"); }
    private String send(TokenService.Tokens sender, String recipient, String amount, UUID key) {
        return transfers.send(sender.userId(), key, recipient + "@example.com", amount, "USD", "Test transfer");
    }
    private int count(String table) { return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class); }
    private void assertBalance(UUID user, String amount) {
        assertEquals(0, jdbc.queryForObject("SELECT available_balance FROM wallets WHERE user_id = ?", BigDecimal.class, user).compareTo(new BigDecimal(amount)));
    }
    private void assertReconciled() {
        assertEquals(0, jdbc.queryForObject("""
                SELECT count(*) FROM (
                    SELECT w.id, w.available_balance, COALESCE(sum(CASE WHEN e.entry_type = 'CREDIT' THEN e.amount ELSE -e.amount END), 0) AS calculated
                    FROM wallets w JOIN ledger_accounts a ON a.wallet_id = w.id LEFT JOIN ledger_entries e ON e.account_id = a.id
                    GROUP BY w.id HAVING w.available_balance <> COALESCE(sum(CASE WHEN e.entry_type = 'CREDIT' THEN e.amount ELSE -e.amount END), 0)
                ) differences
                """, Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM wallets WHERE available_balance < 0", Integer.class));
    }
    private List<Object> race(List<Callable<String>> calls) throws Exception {
        var ready = new CountDownLatch(calls.size()); var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = calls.stream().map(call -> executor.submit(() -> {
                ready.countDown(); start.await();
                try { return (Object) call.call(); } catch (Exception exception) { return exception; }
            })).toList();
            assertTrue(ready.await(5, TimeUnit.SECONDS)); start.countDown();
            var results = new ArrayList<>();
            for (var future : futures) results.add(future.get(25, TimeUnit.SECONDS));
            return results;
        }
    }
    private class Browser {
        final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        String csrf;
        void csrf() throws Exception { csrf = json.readTree(request("GET", "/auth/csrf", null, null, null).body()).get("token").asText(); }
        HttpResponse<String> request(String method, String path, Object body, String access, UUID key) throws Exception {
            var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path)).timeout(Duration.ofSeconds(20));
            if (csrf != null) builder.header("X-CSRF-TOKEN", csrf);
            if (access != null) builder.header("Authorization", "Bearer " + access);
            if (key != null) builder.header("Idempotency-Key", key.toString());
            builder.header("Content-Type", "application/json");
            builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}

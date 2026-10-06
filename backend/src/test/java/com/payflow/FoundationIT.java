package com.payflow;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.payflow.auth.domain.Role;
import com.payflow.user.domain.UserStatus;
import com.payflow.wallet.domain.WalletStatus;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class FoundationIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired TestRestTemplate http;
    @LocalServerPort int port;

    @BeforeEach
    void clearAccounts() {
        jdbc.update("DELETE FROM wallets");
        jdbc.update("DELETE FROM user_roles");
        jdbc.update("DELETE FROM users");
    }

    @Test
    void startsAgainstPostgresAndExposesOnlyBasicHealth() {
        assertTrue(flyway.validateWithResult().validationSuccessful);
        assertEquals(9, flyway.info().applied().length);
        ResponseEntity<String> response = http.getForEntity("/actuator/health", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\":\"UP\"}", response.getBody());
        assertNotNull(response.getHeaders().getFirst("X-Correlation-ID"));
    }

    @Test
    void protectsFinancialRoutesAndUsesSafeErrorResponses() {
        ResponseEntity<String> response = http.getForEntity("/api/v1/wallets", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().contains("\"code\":\"UNAUTHENTICATED\""));
        assertTrue(response.getBody().contains(response.getHeaders().getFirst("X-Correlation-ID")));
        assertFalse(response.getBody().contains("Exception"));
        assertEquals(HttpStatus.UNAUTHORIZED, http.getForEntity("/actuator/env", String.class).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, http.postForEntity("/api/v1/transfers", null, String.class).getStatusCode());
    }

    @Test
    void allowsOnlyConfiguredCorsOrigins() throws Exception {
        // JDK client sends a genuine preflight including Origin (some clients strip it).
        var client = java.net.http.HttpClient.newHttpClient();
        var allowed = client.send(preflight("http://localhost:5173"), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, allowed.statusCode());
        assertEquals("http://localhost:5173", allowed.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
        assertEquals("true", allowed.headers().firstValue("Access-Control-Allow-Credentials").orElseThrow());
        var denied = client.send(preflight("https://untrusted.example"), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(403, denied.statusCode());
        assertTrue(denied.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
    }

    @Test
    void enforcesUniqueNormalizedEmailAndValidRoles() {
        UUID userId = createUser("alice@example.com");
        assertThrows(DataIntegrityViolationException.class, () -> createUser("alice@example.com"));
        assertThrows(DataIntegrityViolationException.class, () -> createUser("ALICE@example.com"));
        assertThrows(DataIntegrityViolationException.class, () -> createUser(" alice@example.com "));
        for (Role role : Role.values()) {
            jdbc.update("INSERT INTO user_roles (user_id, role_name) VALUES (?, ?)", userId, role.name());
        }
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO user_roles (user_id, role_name) VALUES (?, ?)", userId, "ROLE_UNKNOWN"));
    }

    @Test
    void enforcesOneWalletAndItsOwner() {
        UUID userId = createUser("alice@example.com");
        createWallet(userId, "USD", "0");
        assertThrows(DataIntegrityViolationException.class, () -> createWallet(userId, "USD", "0"));
        assertThrows(DataIntegrityViolationException.class, () -> createWallet(UUID.randomUUID(), "USD", "0"));
        assertEquals(0, jdbc.queryForObject("SELECT available_balance FROM wallets", BigDecimal.class).signum());
    }

    @Test
    void rejectsNegativeBalancesInvalidPrecisionAndUnsupportedCurrency() {
        UUID userId = createUser("alice@example.com");
        for (String amount : List.of("-0.01", "0.001", "NaN", "1000000000000000")) {
            assertThrows(DataIntegrityViolationException.class, () -> createWallet(userId, "USD", amount), amount);
        }
        assertThrows(DataIntegrityViolationException.class, () -> createWallet(userId, "EUR", "0"));
        createWallet(userId, "USD", "10.25");
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE wallets SET available_balance = -1 WHERE user_id = ?", userId));
    }

    @Test
    void keepsDomainStatusesConsistentWithSchemaAndRollsBackChanges() {
        UUID userId = createUser("alice@example.com");
        createWallet(userId, "USD", "0");
        for (UserStatus status : UserStatus.values()) {
            jdbc.update("UPDATE users SET status = ? WHERE id = ?", status.name(), userId);
        }
        for (WalletStatus status : WalletStatus.values()) {
            jdbc.update("UPDATE wallets SET status = ? WHERE user_id = ?", status.name(), userId);
        }
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE wallets SET status = 'UNKNOWN' WHERE user_id = ?", userId));
        var transactionManager = new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource());
        var template = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        assertThrows(IllegalStateException.class, () -> template.executeWithoutResult(status -> {
            createUser("rollback@example.com");
            throw new IllegalStateException("Simulated failure");
        }));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM users WHERE email = 'rollback@example.com'", Integer.class));
    }

    private UUID createUser(String email) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, public_id, first_name, last_name, email, password_hash)
                VALUES (?, ?, 'Test', 'User', ?, 'test-only-hash')
                """, id, "PF-USR-" + id, email);
        return id;
    }

    private void createWallet(UUID userId, String currency, String amount) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO wallets (id, public_id, user_id, currency, available_balance)
                VALUES (?, ?, ?, ?, CAST(? AS numeric))
                """, id, "PF-WLT-" + id, userId, currency, amount);
    }

    private java.net.http.HttpRequest preflight(String origin) {
        return java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + "/api/v1/transfers"))
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type, Idempotency-Key")
                .method("OPTIONS", java.net.http.HttpRequest.BodyPublishers.noBody()).build();
    }
}

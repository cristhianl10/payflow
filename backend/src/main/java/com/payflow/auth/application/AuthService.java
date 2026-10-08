package com.payflow.auth.application;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;
import com.payflow.audit.AuditLog;
import com.payflow.ledger.application.LedgerService;
import com.payflow.notification.application.NotificationPublisher;
import com.payflow.notification.domain.NotificationEvent;
import com.payflow.shared.domain.*;
import com.payflow.user.domain.UserStatus;
import com.payflow.user.infrastructure.*;
import com.payflow.wallet.infrastructure.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final WalletRepository wallets;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final EmailVerificationService emailVerification;
    private final LedgerService ledger;
    private final AuditLog audit;
    private final NotificationPublisher notifications;
    private final MfaService mfa;
    private final JdbcTemplate jdbc;
    private final Money grant;
    private final String dummyHash;

    public AuthService(UserRepository users, WalletRepository wallets, PasswordEncoder passwords,
            TokenService tokens, EmailVerificationService emailVerification, LedgerService ledger, AuditLog audit,
            NotificationPublisher notifications, MfaService mfa, JdbcTemplate jdbc,
            @Value("${payflow.default-initial-balance}") BigDecimal initialBalance) {
        this.users = users; this.wallets = wallets; this.passwords = passwords;
        this.tokens = tokens; this.emailVerification = emailVerification; this.ledger = ledger; this.audit = audit;
        this.notifications = notifications; this.mfa = mfa; this.jdbc = jdbc;
        grant = new Money(initialBalance, Currency.getInstance("USD"));
        if (grant.amount().compareTo(new BigDecimal("999999999999999.99")) > 0) {
            throw new IllegalArgumentException("Opening balance exceeds wallet precision");
        }
        dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public TokenService.Tokens register(String firstName, String lastName, String email, String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(400, "PASSWORD_TOO_LONG", "Use a password of at most 72 UTF-8 bytes.");
        }
        String normalized = normalizeEmail(email);
        // Serialize duplicate registrations without turning a unique-constraint violation into a 500.
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", rs -> {}, normalized);
        if (users.findByEmail(normalized).isPresent()) {
            throw new BusinessException(409, "EMAIL_UNAVAILABLE", "An account already uses this email. Try signing in.");
        }
        UserEntity user = users.saveAndFlush(new UserEntity(firstName.strip(), lastName.strip(), normalized, passwords.encode(password)));
        jdbc.update("INSERT INTO user_roles(user_id, role_name) VALUES (?, 'ROLE_USER')", user.id());
        WalletEntity wallet = wallets.saveAndFlush(new WalletEntity(user.id()));
        ledger.openWallet(wallet, grant);
        audit.record("USER_REGISTERED", user.id(), user.id());
        emailVerification.createAndSend(user.id());
        return tokens.start(user.id());
    }

    @Transactional
    public TokenService.Tokens login(String email, String password) {
        UserEntity user = credentials(email, password);
        return successfulLogin(user);
    }

    @Transactional
    public LoginAttempt beginLogin(String email, String password) {
        UserEntity user = credentials(email, password);
        if (mfa.enabled(user.id())) {
            return new LoginAttempt(null, mfa.challenge(user.id()));
        }
        return new LoginAttempt(successfulLogin(user), null);
    }

    @Transactional
    public TokenService.Tokens completeMfa(UUID challengeId, String code) {
        UUID userId = mfa.verifyChallenge(challengeId, code);
        UserEntity user = users.findById(userId).orElseThrow(() ->
                new BusinessException(401, "UNAUTHENTICATED", "Please sign in again."));
        return successfulLogin(user);
    }

    private UserEntity credentials(String email, String password) {
        var found = users.findByEmail(normalizeEmail(email));
        boolean matches = passwords.matches(password, found.map(UserEntity::passwordHash).orElse(dummyHash));
        if (!matches || found.isEmpty() || found.get().status() != UserStatus.ACTIVE) {
            throw new BusinessException(401, "INVALID_CREDENTIALS",
                    "The email or password is incorrect, or this account is unavailable.");
        }
        return found.get();
    }

    private TokenService.Tokens successfulLogin(UserEntity user) {
        audit.record("LOGIN_SUCCESS", user.id(), user.id());
        notifications.publish(new NotificationEvent(
                user.id(), user.email(), "SECURITY_LOGIN", "New sign-in",
                "A new PayFlow session was created for your account.", "/app/account",
                null, null));
        return tokens.start(user.id());
    }

    @Transactional(readOnly = true)
    public UserView me(UUID id) {
        UserEntity user = users.findById(id).orElseThrow(() -> new BusinessException(401, "UNAUTHENTICATED", "Please sign in again."));
        return new UserView(user.publicId(), user.firstName(), user.lastName(), user.email(), user.emailVerified());
    }

    @Transactional
    public UserView updateProfile(UUID id, String firstName, String lastName) {
        UserEntity user = users.findById(id).orElseThrow(() -> new BusinessException(401, "UNAUTHENTICATED", "Please sign in again."));
        user.updateProfile(firstName.strip(), lastName.strip());
        audit.record("PROFILE_UPDATED", id, id);
        return new UserView(user.publicId(), user.firstName(), user.lastName(), user.email(), user.emailVerified());
    }

    @Transactional
    public void changePassword(UUID id, String currentPassword, String newPassword) {
        if (newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(400, "PASSWORD_TOO_LONG", "Use a password of at most 72 UTF-8 bytes.");
        }
        UserEntity user = users.findById(id).orElseThrow(() -> new BusinessException(401, "UNAUTHENTICATED", "Please sign in again."));
        if (!passwords.matches(currentPassword, user.passwordHash())) {
            throw new BusinessException(401, "INVALID_CREDENTIALS", "Your current password is incorrect.");
        }
        user.changePassword(passwords.encode(newPassword));
        jdbc.update("UPDATE auth_sessions SET revoked_at = COALESCE(revoked_at, CURRENT_TIMESTAMP) WHERE user_id = ?", id);
        audit.record("PASSWORD_CHANGED", id, id);
        notifications.publish(new NotificationEvent(
                user.id(), user.email(), "SECURITY_PASSWORD_CHANGED", "Password changed",
                "Your PayFlow password was changed and existing sessions were signed out.", "/app/account",
                "Your PayFlow password was changed",
                "Your PayFlow password was changed. Existing sessions were signed out for security. If this was not you, reset your password immediately."));
    }

    public static String normalizeEmail(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    public record UserView(String publicId, String firstName, String lastName, String email, boolean emailVerified) {}
    public record LoginAttempt(TokenService.Tokens tokens, UUID challengeId) {
        public boolean mfaRequired() { return challengeId != null; }
    }
}

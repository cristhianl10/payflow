package com.payflow.auth.application;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import com.payflow.audit.AuditLog;
import com.payflow.shared.domain.BusinessException;
import com.payflow.user.domain.UserStatus;
import com.payflow.user.infrastructure.UserEntity;
import com.payflow.user.infrastructure.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordRecoveryService {
    private static final Logger log = LoggerFactory.getLogger(PasswordRecoveryService.class);
    private final UserRepository users;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    private final EmailDeliveryService email;
    private final AuditLog audit;
    private final SecureRandom random = new SecureRandom();
    private final long tokenSeconds;
    private final String frontendUrl;

    public PasswordRecoveryService(UserRepository users, JdbcTemplate jdbc, PasswordEncoder passwords,
            EmailDeliveryService email, AuditLog audit,
            @Value("${payflow.email.password-reset-token-seconds:1800}") long tokenSeconds,
            @Value("${payflow.email.frontend-url:http://localhost:5173}") String frontendUrl) {
        if (tokenSeconds < 300 || tokenSeconds > 86400) {
            throw new IllegalArgumentException("Password reset token lifetime must be between 5 minutes and 24 hours");
        }
        this.users = users;
        this.jdbc = jdbc;
        this.passwords = passwords;
        this.email = email;
        this.audit = audit;
        this.tokenSeconds = tokenSeconds;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Transactional
    public void request(String rawEmail) {
        String normalized = AuthService.normalizeEmail(rawEmail);
        var found = users.findByEmail(normalized);
        if (found.isEmpty() || found.get().status() != UserStatus.ACTIVE) return;

        UserEntity user = found.get();
        jdbc.update("""
                UPDATE password_reset_tokens
                SET consumed_at = COALESCE(consumed_at, CURRENT_TIMESTAMP)
                WHERE user_id = ? AND consumed_at IS NULL
                """, user.id());

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO password_reset_tokens(token_hash, user_id, created_at, expires_at)
                VALUES (?, ?, ?, ?)
                """, TokenService.hash(token), user.id(), Timestamp.from(now),
                Timestamp.from(now.plusSeconds(tokenSeconds)));

        String link = frontendUrl + "/reset-password?token=" + token;
        try {
            email.send(user.email(), "Reset your PayFlow password", """
                    A password reset was requested for your PayFlow account.

                    Set a new password using the link below:
                    %s

                    This link expires in %d minutes. If you did not request this, you can ignore this message.
                    """.formatted(link, tokenSeconds / 60), "password reset");
            audit.record("PASSWORD_RESET_REQUESTED", user.id(), user.id());
        } catch (RuntimeException exception) {
            log.error("Password reset email delivery failed for user {}", user.id(), exception);
        }
    }

    @Transactional
    public void reset(String token, String newPassword) {
        if (token == null || token.isBlank() || token.length() > 128) throw invalid();
        validatePassword(newPassword);

        var matches = jdbc.query("""
                SELECT t.user_id, t.expires_at, t.consumed_at
                FROM password_reset_tokens t
                JOIN users u ON u.id = t.user_id
                WHERE t.token_hash = ? AND u.status = 'ACTIVE'
                FOR UPDATE OF t, u
                """, (rs, row) -> new ResetToken(
                rs.getObject("user_id", UUID.class),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getTimestamp("consumed_at") != null), TokenService.hash(token));

        if (matches.isEmpty()) throw invalid();
        ResetToken reset = matches.getFirst();
        if (reset.consumed || !reset.expiresAt.isAfter(Instant.now())) throw invalid();

        UserEntity user = users.findById(reset.userId).orElseThrow(this::invalid);
        user.changePassword(passwords.encode(newPassword));
        jdbc.update("""
                UPDATE password_reset_tokens
                SET consumed_at = COALESCE(consumed_at, CURRENT_TIMESTAMP)
                WHERE user_id = ? AND consumed_at IS NULL
                """, reset.userId);
        jdbc.update("""
                UPDATE auth_sessions
                SET revoked_at = COALESCE(revoked_at, CURRENT_TIMESTAMP)
                WHERE user_id = ?
                """, reset.userId);
        audit.record("PASSWORD_RESET_COMPLETED", reset.userId, reset.userId);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 10) {
            throw new BusinessException(400, "INVALID_PASSWORD", "Use a password of at least 10 characters.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(400, "PASSWORD_TOO_LONG", "Use a password of at most 72 UTF-8 bytes.");
        }
    }

    private BusinessException invalid() {
        return new BusinessException(400, "INVALID_PASSWORD_RESET_TOKEN",
                "This password reset link is invalid or has expired. Request a new one.");
    }

    private record ResetToken(UUID userId, Instant expiresAt, boolean consumed) {}
}

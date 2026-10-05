package com.payflow.auth.application;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import com.payflow.audit.AuditLog;
import com.payflow.shared.domain.BusinessException;
import com.payflow.user.infrastructure.UserEntity;
import com.payflow.user.infrastructure.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private final UserRepository users;
    private final JdbcTemplate jdbc;
    private final AuditLog audit;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final SecureRandom random = new SecureRandom();
    private final long tokenSeconds;
    private final String frontendUrl;
    private final String deliveryMode;
    private final String from;

    public EmailVerificationService(UserRepository users, JdbcTemplate jdbc, AuditLog audit, ObjectProvider<JavaMailSender> mailSender,
            @Value("${payflow.email.verification-token-seconds:3600}") long tokenSeconds,
            @Value("${payflow.email.frontend-url:http://localhost:5173}") String frontendUrl,
            @Value("${payflow.email.delivery-mode:log}") String deliveryMode,
            @Value("${payflow.email.from:no-reply@payflow.local}") String from) {
        if (tokenSeconds < 300 || tokenSeconds > 86400) {
            throw new IllegalArgumentException("Email verification token lifetime must be between 5 minutes and 24 hours");
        }
        this.users = users;
        this.jdbc = jdbc;
        this.audit = audit;
        this.mailSender = mailSender;
        this.tokenSeconds = tokenSeconds;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
        this.deliveryMode = deliveryMode;
        this.from = from;
    }

    @Transactional
    public void createAndSend(UUID userId) {
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "USER_NOT_FOUND", "The account could not be found."));
        if (user.emailVerified()) return;

        jdbc.update("""
                UPDATE email_verification_tokens
                SET consumed_at = COALESCE(consumed_at, CURRENT_TIMESTAMP)
                WHERE user_id = ? AND consumed_at IS NULL
                """, userId);

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(tokenSeconds);
        jdbc.update("""
                INSERT INTO email_verification_tokens(token_hash, user_id, created_at, expires_at)
                VALUES (?, ?, ?, ?)
                """, TokenService.hash(token), userId, Timestamp.from(now), Timestamp.from(expires));

        String link = frontendUrl + "/verify-email?token=" + token;
        deliver(user.email(), link);
        audit.record("EMAIL_VERIFICATION_SENT", userId, userId);
    }

    @Transactional
    public void verify(String token) {
        if (token == null || token.isBlank() || token.length() > 128) throw invalid();

        var matches = jdbc.query("""
                SELECT t.user_id, t.expires_at, t.consumed_at, u.email_verified
                FROM email_verification_tokens t
                JOIN users u ON u.id = t.user_id
                WHERE t.token_hash = ?
                FOR UPDATE OF t, u
                """, (rs, row) -> new Verification(
                rs.getObject("user_id", UUID.class),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getTimestamp("consumed_at") != null,
                rs.getBoolean("email_verified")), TokenService.hash(token));

        if (matches.isEmpty()) throw invalid();
        Verification verification = matches.getFirst();

        if (verification.emailVerified) return;
        if (verification.consumed || !verification.expiresAt.isAfter(Instant.now())) throw invalid();

        jdbc.update("UPDATE users SET email_verified = TRUE, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                verification.userId);
        jdbc.update("UPDATE email_verification_tokens SET consumed_at = CURRENT_TIMESTAMP WHERE token_hash = ?",
                TokenService.hash(token));
        jdbc.update("""
                UPDATE email_verification_tokens
                SET consumed_at = COALESCE(consumed_at, CURRENT_TIMESTAMP)
                WHERE user_id = ? AND consumed_at IS NULL
                """, verification.userId);
        audit.record("EMAIL_VERIFIED", verification.userId, verification.userId);
    }

    private void deliver(String recipient, String link) {
        if ("log".equalsIgnoreCase(deliveryMode)) {
            log.info("PayFlow email verification for {}: {}", recipient, link);
            return;
        }
        if (!"smtp".equalsIgnoreCase(deliveryMode)) {
            throw new IllegalStateException("Unsupported payflow.email.delivery-mode");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Verify your PayFlow email");
        message.setText("""
                Welcome to PayFlow.

                Verify your email address using the link below:
                %s

                This link expires in %d minutes. If you did not create this account, you can ignore this message.
                """.formatted(link, tokenSeconds / 60));
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("SMTP delivery is enabled but no JavaMailSender is configured");
        }
        sender.send(message);
    }

    private BusinessException invalid() {
        return new BusinessException(400, "INVALID_VERIFICATION_TOKEN",
                "This verification link is invalid or has expired. Request a new one.");
    }

    private record Verification(UUID userId, Instant expiresAt, boolean consumed, boolean emailVerified) {}
}

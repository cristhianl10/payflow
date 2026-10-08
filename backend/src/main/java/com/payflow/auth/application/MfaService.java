package com.payflow.auth.application;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.payflow.audit.AuditLog;
import com.payflow.notification.application.NotificationPublisher;
import com.payflow.notification.domain.NotificationEvent;
import com.payflow.shared.domain.BusinessException;
import com.payflow.user.infrastructure.UserRepository;
import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MfaService {
    private static final Base32 BASE32 = new Base32();
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final AuditLog audit;
    private final NotificationPublisher notifications;
    private final SecureRandom random = new SecureRandom();
    private final byte[] encryptionKey;

    public MfaService(JdbcTemplate jdbc, UserRepository users, PasswordEncoder passwords, AuditLog audit,
            NotificationPublisher notifications,
            @Value("${payflow.mfa.encryption-key:${payflow.auth.secret:development-only-mfa-key-change-me}}")
            String keyMaterial) {
        this.jdbc = jdbc;
        this.users = users;
        this.passwords = passwords;
        this.audit = audit;
        this.notifications = notifications;
        this.encryptionKey = sha256(keyMaterial.getBytes(StandardCharsets.UTF_8));
    }

    @Transactional(readOnly = true)
    public Status status(UUID userId) {
        Boolean enabled = jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM user_mfa WHERE user_id = ? AND enabled_at IS NOT NULL)",
                Boolean.class, userId);
        return new Status(Boolean.TRUE.equals(enabled));
    }

    @Transactional
    public Setup begin(UUID userId) {
        var user = user(userId);
        byte[] secret = new byte[20];
        random.nextBytes(secret);
        String base32 = BASE32.encodeToString(secret).replace("=", "");
        String encrypted = encrypt(secret);
        jdbc.update("""
                INSERT INTO user_mfa(user_id, secret_ciphertext, enabled_at)
                VALUES (?, ?, NULL)
                ON CONFLICT (user_id) DO UPDATE
                SET secret_ciphertext = EXCLUDED.secret_ciphertext,
                    enabled_at = NULL,
                    updated_at = CURRENT_TIMESTAMP
                """, userId, encrypted);
        jdbc.update("DELETE FROM mfa_recovery_codes WHERE user_id = ?", userId);
        String uri = "otpauth://totp/PayFlow:" + enc(user.email())
                + "?secret=" + base32 + "&issuer=PayFlow&algorithm=SHA1&digits=6&period=30";
        return new Setup(base32, uri, qr(uri));
    }

    @Transactional
    public Enabled confirm(UUID userId, String code) {
        byte[] secret = pendingSecret(userId);
        if (!validTotp(secret, code)) {
            throw new BusinessException(400, "INVALID_MFA_CODE", "Enter the current 6-digit authenticator code.");
        }
        jdbc.update("UPDATE user_mfa SET enabled_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?",
                userId);
        jdbc.update("DELETE FROM mfa_recovery_codes WHERE user_id = ?", userId);
        List<String> recovery = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String raw = recoveryCode();
            recovery.add(raw);
            jdbc.update("INSERT INTO mfa_recovery_codes(user_id, code_hash) VALUES (?, ?)",
                    userId, TokenService.hash(normalizeRecovery(raw)));
        }
        var user = user(userId);
        audit.record("MFA_ENABLED", userId, userId);
        notifications.publish(new NotificationEvent(userId, user.email(), "SECURITY_MFA_ENABLED",
                "Two-factor authentication enabled",
                "Authenticator-based two-factor authentication is now protecting your PayFlow account.",
                "/app/account", "PayFlow two-factor authentication enabled",
                "Authenticator-based two-factor authentication was enabled on your PayFlow account."));
        return new Enabled(recovery);
    }

    @Transactional
    public void disable(UUID userId, String currentPassword, String code) {
        var user = user(userId);
        if (!passwords.matches(currentPassword, user.passwordHash())) {
            throw new BusinessException(401, "INVALID_CREDENTIALS", "Your current password is incorrect.");
        }
        if (!verifyUserCode(userId, code)) {
            throw new BusinessException(400, "INVALID_MFA_CODE", "Enter a valid authenticator or recovery code.");
        }
        jdbc.update("DELETE FROM user_mfa WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM mfa_recovery_codes WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM mfa_login_challenges WHERE user_id = ?", userId);
        audit.record("MFA_DISABLED", userId, userId);
        notifications.publish(new NotificationEvent(userId, user.email(), "SECURITY_MFA_DISABLED",
                "Two-factor authentication disabled",
                "Two-factor authentication was removed from your PayFlow account.", "/app/account",
                "PayFlow two-factor authentication disabled",
                "Two-factor authentication was disabled on your PayFlow account."));
    }

    @Transactional(readOnly = true)
    public boolean enabled(UUID userId) {
        return status(userId).enabled();
    }

    @Transactional
    public UUID challenge(UUID userId) {
        jdbc.update("DELETE FROM mfa_login_challenges WHERE user_id = ? AND expires_at < CURRENT_TIMESTAMP", userId);
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO mfa_login_challenges(id, user_id, expires_at)
                VALUES (?, ?, CURRENT_TIMESTAMP + INTERVAL '5 minutes')
                """, id, userId);
        return id;
    }

    @Transactional
    public UUID verifyChallenge(UUID challengeId, String code) {
        var rows = jdbc.query("""
                SELECT user_id, expires_at, consumed_at
                FROM mfa_login_challenges
                WHERE id = ?
                FOR UPDATE
                """, (rs, row) -> new Challenge(
                rs.getObject("user_id", UUID.class),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getTimestamp("consumed_at") != null), challengeId);
        if (rows.isEmpty() || rows.getFirst().consumed()
                || !rows.getFirst().expiresAt().isAfter(Instant.now())) {
            throw new BusinessException(401, "MFA_CHALLENGE_EXPIRED", "Start the sign-in process again.");
        }
        UUID userId = rows.getFirst().userId();
        if (!verifyUserCode(userId, code)) {
            throw new BusinessException(401, "INVALID_MFA_CODE", "The authenticator or recovery code is invalid.");
        }
        jdbc.update("UPDATE mfa_login_challenges SET consumed_at = CURRENT_TIMESTAMP WHERE id = ?", challengeId);
        audit.record("MFA_CHALLENGE_COMPLETED", userId, challengeId);
        return userId;
    }

    private boolean verifyUserCode(UUID userId, String code) {
        if (code == null) return false;
        if (code.matches("\\d{6}")) {
            var values = jdbc.query("""
                    SELECT secret_ciphertext FROM user_mfa
                    WHERE user_id = ? AND enabled_at IS NOT NULL
                    """, (rs, row) -> rs.getString(1), userId);
            return !values.isEmpty() && validTotp(decrypt(values.getFirst()), code);
        }
        String hash = TokenService.hash(normalizeRecovery(code));
        int used = jdbc.update("""
                UPDATE mfa_recovery_codes SET used_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND code_hash = ? AND used_at IS NULL
                """, userId, hash);
        return used == 1;
    }

    private byte[] pendingSecret(UUID userId) {
        var values = jdbc.query("""
                SELECT secret_ciphertext FROM user_mfa
                WHERE user_id = ? AND enabled_at IS NULL
                """, (rs, row) -> rs.getString(1), userId);
        if (values.isEmpty()) throw new BusinessException(404, "MFA_SETUP_NOT_FOUND", "Start MFA setup again.");
        return decrypt(values.getFirst());
    }

    private boolean validTotp(byte[] secret, String code) {
        if (code == null || !code.matches("\\d{6}")) return false;
        long step = Instant.now().getEpochSecond() / 30;
        for (long offset = -1; offset <= 1; offset++) {
            if (totp(secret, step + offset).equals(code)) return true;
        }
        return false;
    }

    private String totp(byte[] secret, long counter) {
        try {
            byte[] data = java.nio.ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            return String.format(Locale.ROOT, "%06d", binary % 1_000_000);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String encrypt(byte[] value) {
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(value);
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private byte[] decrypt(String value) {
        try {
            byte[] combined = Base64.getDecoder().decode(value);
            byte[] iv = Arrays.copyOfRange(combined, 0, 12);
            byte[] encrypted = Arrays.copyOfRange(combined, 12, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(128, iv));
            return cipher.doFinal(encrypted);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not decrypt MFA secret", exception);
        }
    }

    private String qr(String uri) {
        try {
            var matrix = new QRCodeWriter().encode(uri, BarcodeFormat.QR_CODE, 260, 260);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate MFA QR code", exception);
        }
    }

    private String recoveryCode() {
        byte[] bytes = new byte[6];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).toUpperCase(Locale.ROOT);
        return raw.substring(0, 4) + "-" + raw.substring(4, 8);
    }

    private static String normalizeRecovery(String value) {
        return value == null ? "" : value.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
    }

    private static byte[] sha256(byte[] value) {
        try { return MessageDigest.getInstance("SHA-256").digest(value); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private com.payflow.user.infrastructure.UserEntity user(UUID id) {
        return users.findById(id).orElseThrow(() ->
                new BusinessException(401, "UNAUTHENTICATED", "Please sign in again."));
    }

    private record Challenge(UUID userId, Instant expiresAt, boolean consumed) {}
    public record Status(boolean enabled) {}
    public record Setup(String secret, String otpauthUri, String qrDataUrl) {}
    public record Enabled(List<String> recoveryCodes) {}
}

package com.payflow.auth.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import com.payflow.shared.domain.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {
    private final JdbcTemplate jdbc;
    private final JwtEncoder encoder;
    private final long accessSeconds;
    private final long refreshSeconds;
    private final SecureRandom random = new SecureRandom();

    public TokenService(JdbcTemplate jdbc, JwtEncoder encoder,
            @Value("${payflow.auth.access-seconds:900}") long accessSeconds,
            @Value("${payflow.auth.refresh-seconds:604800}") long refreshSeconds) {
        if (accessSeconds < 30 || accessSeconds > 3600 || refreshSeconds < accessSeconds) {
            throw new IllegalArgumentException("Invalid token lifetimes");
        }
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.accessSeconds = accessSeconds;
        this.refreshSeconds = refreshSeconds;
    }

    @Transactional
    public Tokens start(UUID user) {
        UUID session = UUID.randomUUID();
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(refreshSeconds);
        jdbc.update("INSERT INTO auth_sessions(id, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)",
                session, user, Timestamp.from(now), Timestamp.from(expiry));
        return issue(user, session, expiry);
    }

    @Transactional(noRollbackFor = RejectedSession.class)
    public Tokens refresh(String token) {
        if (token == null || token.length() > 128) throw rejected();
        String hash = hash(token);
        var sessions = jdbc.query("""
                SELECT s.id, s.user_id, s.expires_at, s.revoked_at, r.consumed_at, u.status
                FROM refresh_tokens r JOIN auth_sessions s ON s.id = r.session_id
                JOIN users u ON u.id = s.user_id WHERE r.token_hash = ? FOR UPDATE OF s
                """, (rs, row) -> new Session(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                    rs.getTimestamp("expires_at").toInstant(), rs.getTimestamp("revoked_at") != null,
                    rs.getTimestamp("consumed_at") != null, rs.getString("status")), hash);
        if (sessions.isEmpty()) throw rejected();
        Session session = sessions.getFirst();
        // Re-read after acquiring the session lock: a concurrent rotation may have consumed this token.
        boolean used = Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT consumed_at IS NOT NULL FROM refresh_tokens WHERE token_hash = ?", Boolean.class, hash));
        if (used || session.revoked || !session.expiresAt.isAfter(Instant.now()) || !session.status.equals("ACTIVE")) {
            revoke(session.id);
            throw rejected();
        }
        jdbc.update("UPDATE refresh_tokens SET consumed_at = CURRENT_TIMESTAMP WHERE token_hash = ?", hash);
        return issue(session.user, session.id, session.expiresAt);
    }

    @Transactional
    public void logout(String token) {
        if (token == null || token.length() > 128) return;
        jdbc.query("SELECT session_id FROM refresh_tokens WHERE token_hash = ?",
                (rs, row) -> rs.getObject(1, UUID.class), hash(token)).forEach(this::revoke);
    }

    public AbstractAuthenticationToken authenticate(Jwt jwt) {
        try {
            UUID user = UUID.fromString(jwt.getSubject());
            UUID session = UUID.fromString(jwt.getClaimAsString("sid"));
            Integer valid = jdbc.queryForObject("""
                    SELECT count(*) FROM auth_sessions s JOIN users u ON u.id = s.user_id
                    WHERE s.id = ? AND s.user_id = ? AND s.revoked_at IS NULL
                    AND s.expires_at > CURRENT_TIMESTAMP AND u.status = 'ACTIVE'
                    """, Integer.class, session, user);
            if (valid == null || valid != 1) throw new IllegalArgumentException();
            var roles = jdbc.query("SELECT role_name FROM user_roles WHERE user_id = ?",
                    (rs, row) -> new SimpleGrantedAuthority(rs.getString(1)), user);
            return new JwtAuthenticationToken(jwt, roles, user.toString());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new OAuth2AuthenticationException("invalid_token");
        }
    }

    private Tokens issue(UUID user, UUID session, Instant sessionExpiry) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = Instant.now();
        jdbc.update("INSERT INTO refresh_tokens(token_hash, session_id, created_at) VALUES (?, ?, ?)",
                hash(refresh), session, Timestamp.from(now));
        Instant expiry = now.plusSeconds(accessSeconds);
        if (expiry.isAfter(sessionExpiry)) expiry = sessionExpiry;
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("payflow").audience(List.of("payflow-web"))
                .subject(user.toString()).claim("sid", session.toString()).id(UUID.randomUUID().toString())
                .issuedAt(now).expiresAt(expiry).build();
        String access = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new Tokens(access, refresh, expiry, sessionExpiry, user);
    }

    private void revoke(UUID session) {
        jdbc.update("UPDATE auth_sessions SET revoked_at = COALESCE(revoked_at, CURRENT_TIMESTAMP) WHERE id = ?", session);
    }

    public static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    private RejectedSession rejected() { return new RejectedSession(); }
    public static class RejectedSession extends BusinessException {
        public RejectedSession() { super(401, "SESSION_EXPIRED", "Your session has expired. Please sign in again."); }
    }
    private record Session(UUID id, UUID user, Instant expiresAt, boolean revoked, boolean consumed, String status) {}
    public record Tokens(String accessToken, String refreshToken, Instant expiresAt, Instant sessionExpiresAt, UUID userId) {}
}

package com.payflow.beneficiary.application;

import java.util.List;
import java.util.UUID;

import com.payflow.audit.AuditLog;
import com.payflow.auth.application.AuthService;
import com.payflow.shared.domain.BusinessException;
import com.payflow.user.domain.UserStatus;
import com.payflow.user.infrastructure.UserEntity;
import com.payflow.user.infrastructure.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BeneficiaryService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final AuditLog audit;

    public BeneficiaryService(JdbcTemplate jdbc, UserRepository users, AuditLog audit) {
        this.jdbc = jdbc;
        this.users = users;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryView> list(UUID owner) {
        return jdbc.query("""
                SELECT b.public_id, b.alias, u.email, u.first_name, u.last_name, b.created_at
                FROM beneficiaries b
                JOIN users u ON u.id = b.beneficiary_user_id
                WHERE b.owner_user_id = ? AND u.status = 'ACTIVE'
                ORDER BY COALESCE(NULLIF(b.alias, ''), u.first_name), u.last_name, b.created_at
                """, (rs, row) -> new BeneficiaryView(
                rs.getString("public_id"),
                rs.getString("alias"),
                rs.getString("email"),
                rs.getString("first_name") + " " + rs.getString("last_name"),
                rs.getTimestamp("created_at").toInstant()), owner);
    }

    @Transactional
    public BeneficiaryView create(UUID owner, String email, String alias) {
        UserEntity recipient = recipient(owner, email);
        String cleanAlias = normalizeAlias(alias);
        UUID id = UUID.randomUUID();
        String publicId = "PF-BEN-" + UUID.randomUUID();
        try {
            jdbc.update("""
                    INSERT INTO beneficiaries(id, public_id, owner_user_id, beneficiary_user_id, alias)
                    VALUES (?, ?, ?, ?, ?)
                    """, id, publicId, owner, recipient.id(), cleanAlias);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(409, "BENEFICIARY_EXISTS",
                    "This PayFlow user is already in your beneficiaries.");
        }
        audit.record("BENEFICIARY_CREATED", owner, id);
        return byPublicId(owner, publicId);
    }

    @Transactional
    public BeneficiaryView update(UUID owner, String publicId, String alias) {
        UUID id = jdbc.query("""
                SELECT id FROM beneficiaries WHERE owner_user_id = ? AND public_id = ?
                """, rs -> rs.next() ? rs.getObject("id", UUID.class) : null, owner, publicId);
        if (id == null) throw notFound();

        jdbc.update("""
                UPDATE beneficiaries
                SET alias = ?, updated_at = CURRENT_TIMESTAMP
                WHERE owner_user_id = ? AND public_id = ?
                """, normalizeAlias(alias), owner, publicId);
        audit.record("BENEFICIARY_UPDATED", owner, id);
        return byPublicId(owner, publicId);
    }

    @Transactional
    public void delete(UUID owner, String publicId) {
        UUID id = jdbc.query("""
                SELECT id FROM beneficiaries WHERE owner_user_id = ? AND public_id = ?
                """, rs -> rs.next() ? rs.getObject("id", UUID.class) : null, owner, publicId);
        if (id == null) throw notFound();

        jdbc.update("DELETE FROM beneficiaries WHERE owner_user_id = ? AND public_id = ?", owner, publicId);
        audit.record("BENEFICIARY_DELETED", owner, id);
    }

    @Transactional(readOnly = true)
    public BeneficiaryView byPublicId(UUID owner, String publicId) {
        var rows = jdbc.query("""
                SELECT b.public_id, b.alias, u.email, u.first_name, u.last_name, b.created_at
                FROM beneficiaries b
                JOIN users u ON u.id = b.beneficiary_user_id
                WHERE b.owner_user_id = ? AND b.public_id = ? AND u.status = 'ACTIVE'
                """, (rs, row) -> new BeneficiaryView(
                rs.getString("public_id"),
                rs.getString("alias"),
                rs.getString("email"),
                rs.getString("first_name") + " " + rs.getString("last_name"),
                rs.getTimestamp("created_at").toInstant()), owner, publicId);
        if (rows.isEmpty()) throw notFound();
        return rows.getFirst();
    }

    private UserEntity recipient(UUID owner, String email) {
        UserEntity user = users.findByEmail(AuthService.normalizeEmail(email))
                .orElseThrow(this::unavailable);
        if (user.id().equals(owner)) {
            throw new BusinessException(422, "SELF_BENEFICIARY",
                    "You cannot add your own PayFlow account as a beneficiary.");
        }
        if (user.status() != UserStatus.ACTIVE) throw unavailable();
        return user;
    }

    private String normalizeAlias(String alias) {
        if (alias == null) return null;
        String clean = alias.strip();
        if (clean.isEmpty()) return null;
        if (clean.length() > 100) {
            throw new BusinessException(400, "INVALID_ALIAS", "Use an alias of at most 100 characters.");
        }
        return clean;
    }

    private BusinessException unavailable() {
        return new BusinessException(422, "RECIPIENT_UNAVAILABLE",
                "This recipient is not available. Check their PayFlow email.");
    }

    private BusinessException notFound() {
        return new BusinessException(404, "BENEFICIARY_NOT_FOUND", "This beneficiary could not be found.");
    }

    public record BeneficiaryView(String publicId, String alias, String email, String displayName,
            java.time.Instant createdAt) {}
}

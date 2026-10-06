package com.payflow.scheduled.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ScheduledTransferStore {
    private final JdbcTemplate jdbc;

    public ScheduledTransferStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public List<UUID> claimDue(int limit) {
        return jdbc.query("""
                WITH due AS (
                    SELECT id
                    FROM scheduled_transfers
                    WHERE status = 'SCHEDULED' AND execute_at <= CURRENT_TIMESTAMP
                    ORDER BY execute_at, id
                    FOR UPDATE SKIP LOCKED
                    LIMIT ?
                )
                UPDATE scheduled_transfers s
                SET status = 'PROCESSING', updated_at = CURRENT_TIMESTAMP
                FROM due
                WHERE s.id = due.id
                RETURNING s.id
                """, (rs, row) -> rs.getObject("id", UUID.class), limit);
    }

    @Transactional
    public int recoverStale() {
        return jdbc.update("""
                UPDATE scheduled_transfers
                SET status = 'SCHEDULED', updated_at = CURRENT_TIMESTAMP
                WHERE status = 'PROCESSING'
                  AND updated_at < CURRENT_TIMESTAMP - INTERVAL '10 minutes'
                """);
    }

    @Transactional(readOnly = true)
    public Claimed loadProcessing(UUID id) {
        return jdbc.query("""
                SELECT id, public_id, user_id, recipient_email, amount, currency,
                       description, reference, execute_at
                FROM scheduled_transfers
                WHERE id = ? AND status = 'PROCESSING'
                """, rs -> rs.next() ? new Claimed(
                rs.getObject("id", UUID.class),
                rs.getString("public_id"),
                rs.getObject("user_id", UUID.class),
                rs.getString("recipient_email"),
                rs.getBigDecimal("amount").setScale(2).toPlainString(),
                rs.getString("currency"),
                rs.getString("description"),
                rs.getString("reference"),
                rs.getTimestamp("execute_at").toInstant()) : null, id);
    }

    @Transactional
    public void markCompleted(UUID id, String operationPublicId) {
        jdbc.update("""
                UPDATE scheduled_transfers
                SET status = 'COMPLETED',
                    operation_id = (SELECT id FROM journal_operations WHERE public_id = ?),
                    failure_code = NULL,
                    failure_message = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND status = 'PROCESSING'
                """, operationPublicId, id);
    }

    @Transactional
    public void markFailed(UUID id, String code, String message) {
        jdbc.update("""
                UPDATE scheduled_transfers
                SET status = 'FAILED', failure_code = ?, failure_message = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND status = 'PROCESSING'
                """, code, message, id);
    }

    public record Claimed(UUID id, String publicId, UUID userId, String recipientEmail,
            String amount, String currency, String description, String reference, Instant executeAt) {}
}

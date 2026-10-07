package com.payflow.dashboard.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardQueries {
    private final JdbcTemplate jdbc;

    public DashboardQueries(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public DashboardSummary summary(UUID userId) {
        UUID walletId = jdbc.queryForObject(
                "SELECT id FROM wallets WHERE user_id = ?",
                UUID.class, userId);

        BigDecimal sent = amount("""
                SELECT COALESCE(sum(amount), 0)
                FROM journal_operations
                WHERE kind = 'TRANSFER' AND sender_wallet_id = ?
                """, walletId);
        BigDecimal received = amount("""
                SELECT COALESCE(sum(amount), 0)
                FROM journal_operations
                WHERE kind = 'TRANSFER' AND receiver_wallet_id = ?
                """, walletId);
        Integer transferCount = jdbc.queryForObject("""
                SELECT count(*)
                FROM journal_operations
                WHERE kind = 'TRANSFER'
                  AND (sender_wallet_id = ? OR receiver_wallet_id = ?)
                """, Integer.class, walletId, walletId);

        var trend = jdbc.query("""
                WITH days AS (
                    SELECT generate_series(
                        CURRENT_DATE - INTERVAL '6 days',
                        CURRENT_DATE,
                        INTERVAL '1 day'
                    )::date AS day
                )
                SELECT d.day,
                       COALESCE(sum(CASE
                           WHEN j.sender_wallet_id = ? AND j.kind = 'TRANSFER' THEN j.amount
                           ELSE 0 END), 0) AS sent,
                       COALESCE(sum(CASE
                           WHEN j.receiver_wallet_id = ? AND j.kind = 'TRANSFER' THEN j.amount
                           ELSE 0 END), 0) AS received
                FROM days d
                LEFT JOIN journal_operations j
                  ON j.created_at >= d.day
                 AND j.created_at < d.day + INTERVAL '1 day'
                 AND (j.sender_wallet_id = ? OR j.receiver_wallet_id = ?)
                GROUP BY d.day
                ORDER BY d.day
                """, (rs, row) -> new TrendPoint(
                rs.getObject("day", LocalDate.class),
                rs.getBigDecimal("sent").setScale(2).toPlainString(),
                rs.getBigDecimal("received").setScale(2).toPlainString()),
                walletId, walletId, walletId, walletId);

        var topRecipients = jdbc.query("""
                SELECT u.first_name, u.last_name, u.email,
                       count(*) AS transfers,
                       sum(j.amount) AS total
                FROM journal_operations j
                JOIN wallets rw ON rw.id = j.receiver_wallet_id
                JOIN users u ON u.id = rw.user_id
                WHERE j.kind = 'TRANSFER' AND j.sender_wallet_id = ?
                GROUP BY u.id, u.first_name, u.last_name, u.email
                ORDER BY total DESC, transfers DESC
                LIMIT 5
                """, (rs, row) -> new TopRecipient(
                rs.getString("first_name") + " " + rs.getString("last_name"),
                rs.getString("email"),
                rs.getInt("transfers"),
                rs.getBigDecimal("total").setScale(2).toPlainString()),
                walletId);

        var scheduled = jdbc.query("""
                SELECT public_id, recipient_email, amount, execute_at, status
                FROM scheduled_transfers
                WHERE user_id = ? AND status IN ('SCHEDULED', 'PROCESSING')
                ORDER BY execute_at ASC
                LIMIT 5
                """, (rs, row) -> new UpcomingTransfer(
                rs.getString("public_id"),
                rs.getString("recipient_email"),
                rs.getBigDecimal("amount").setScale(2).toPlainString(),
                rs.getTimestamp("execute_at").toInstant(),
                rs.getString("status")),
                userId);

        StatusSummary statuses = jdbc.query("""
                SELECT
                    count(*) FILTER (WHERE status = 'COMPLETED') AS completed,
                    count(*) FILTER (WHERE status = 'FAILED') AS failed,
                    count(*) FILTER (WHERE status IN ('SCHEDULED', 'PROCESSING')) AS scheduled
                FROM scheduled_transfers
                WHERE user_id = ?
                """, rs -> {
            rs.next();
            return new StatusSummary(
                    rs.getLong("completed"),
                    rs.getLong("failed"),
                    rs.getLong("scheduled"));
        }, userId);

        return new DashboardSummary(
                sent.setScale(2).toPlainString(),
                received.setScale(2).toPlainString(),
                transferCount == null ? 0 : transferCount,
                trend,
                topRecipients,
                scheduled,
                statuses);
    }

    private BigDecimal amount(String sql, Object... args) {
        BigDecimal value = jdbc.queryForObject(sql, BigDecimal.class, args);
        return value == null ? BigDecimal.ZERO : value;
    }

    public record DashboardSummary(
            String totalSent,
            String totalReceived,
            int transferCount,
            List<TrendPoint> trend,
            List<TopRecipient> topRecipients,
            List<UpcomingTransfer> upcomingTransfers,
            StatusSummary scheduledStatus) {}

    public record TrendPoint(LocalDate day, String sent, String received) {}
    public record TopRecipient(String displayName, String email, int transfers, String total) {}
    public record UpcomingTransfer(String publicId, String recipientEmail, String amount,
            java.time.Instant executeAt, String status) {}
    public record StatusSummary(long completed, long failed, long scheduled) {}
}

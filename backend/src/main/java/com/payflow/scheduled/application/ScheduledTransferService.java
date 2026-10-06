package com.payflow.scheduled.application;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.payflow.auth.application.AuthService;
import com.payflow.notification.application.NotificationPublisher;
import com.payflow.notification.domain.NotificationEvent;
import com.payflow.shared.domain.BusinessException;
import com.payflow.transfer.application.TransferService;
import com.payflow.user.infrastructure.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduledTransferService {
    private static final Duration MIN_DELAY = Duration.ofMinutes(1);
    private static final Duration MAX_DELAY = Duration.ofDays(365);

    private final JdbcTemplate jdbc;
    private final TransferService transfers;
    private final UserRepository users;
    private final NotificationPublisher notifications;

    public ScheduledTransferService(JdbcTemplate jdbc, TransferService transfers, UserRepository users,
            NotificationPublisher notifications) {
        this.jdbc = jdbc;
        this.transfers = transfers;
        this.users = users;
        this.notifications = notifications;
    }

    @Transactional
    public ScheduledTransferView create(UUID userId, String recipient, String amount, String currency,
            String description, String reference, Instant executeAt) {
        if (!"USD".equals(currency)) {
            throw new BusinessException(400, "UNSUPPORTED_CURRENCY", "Scheduled transfers currently support USD only.");
        }
        if (amount == null || !amount.matches("(?:0|[1-9][0-9]{0,14})(?:\\.[0-9]{1,2})?")) {
            throw new BusinessException(400, "INVALID_AMOUNT", "Enter a valid USD amount.");
        }
        BigDecimal parsed = new BigDecimal(amount);
        if (parsed.signum() <= 0) {
            throw new BusinessException(400, "INVALID_AMOUNT", "Enter an amount greater than zero.");
        }
        BigDecimal max = new BigDecimal(transfers.rules().maxPerOperation());
        if (parsed.compareTo(max) > 0) {
            throw new BusinessException(422, "TRANSFER_LIMIT_EXCEEDED",
                    "This transfer exceeds the per-operation limit of $" + max.setScale(2) + " USD.");
        }
        String email = AuthService.normalizeEmail(recipient);
        transfers.recipient(userId, email);

        String note = description == null ? "" : description.strip();
        String cleanReference = reference == null ? "" : reference.strip();
        if (note.length() > 240 || cleanReference.length() > 80) {
            throw new BusinessException(400, "INVALID_TRANSFER_DETAILS", "Check the description and reference length.");
        }

        Instant now = Instant.now();
        if (executeAt == null || executeAt.isBefore(now.plus(MIN_DELAY))
                || executeAt.isAfter(now.plus(MAX_DELAY))) {
            throw new BusinessException(400, "INVALID_SCHEDULE",
                    "Choose a time at least one minute from now and within the next 365 days.");
        }

        UUID id = UUID.randomUUID();
        String publicId = "PF-SCH-" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO scheduled_transfers(
                    id, public_id, user_id, recipient_email, amount, currency,
                    description, reference, execute_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, publicId, userId, email, parsed, currency, note,
                cleanReference.isEmpty() ? null : cleanReference,
                java.sql.Timestamp.from(executeAt));

        var user = users.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "UNAUTHENTICATED", "Please sign in again."));
        notifications.publish(new NotificationEvent(
                userId, user.email(), "SCHEDULED_TRANSFER_CREATED", "Transfer scheduled",
                "Your transfer of $" + parsed.setScale(2) + " USD to " + email + " is scheduled.",
                "/app/scheduled-transfers", null, null));
        return detail(userId, publicId);
    }

    @Transactional(readOnly = true)
    public List<ScheduledTransferView> list(UUID userId) {
        return jdbc.query("""
                SELECT s.public_id, s.recipient_email, s.amount, s.currency, s.description,
                       s.reference, s.execute_at, s.status, s.failure_code, s.failure_message,
                       j.public_id AS operation_public_id, s.created_at
                FROM scheduled_transfers s
                LEFT JOIN journal_operations j ON j.id = s.operation_id
                WHERE s.user_id = ?
                ORDER BY
                    CASE WHEN s.status IN ('SCHEDULED','PROCESSING') THEN 0 ELSE 1 END,
                    s.execute_at ASC,
                    s.created_at DESC
                """, (rs, row) -> map(rs), userId);
    }

    @Transactional(readOnly = true)
    public ScheduledTransferView detail(UUID userId, String publicId) {
        var rows = jdbc.query("""
                SELECT s.public_id, s.recipient_email, s.amount, s.currency, s.description,
                       s.reference, s.execute_at, s.status, s.failure_code, s.failure_message,
                       j.public_id AS operation_public_id, s.created_at
                FROM scheduled_transfers s
                LEFT JOIN journal_operations j ON j.id = s.operation_id
                WHERE s.user_id = ? AND s.public_id = ?
                """, (rs, row) -> map(rs), userId, publicId);
        if (rows.isEmpty()) {
            throw new BusinessException(404, "SCHEDULED_TRANSFER_NOT_FOUND",
                    "This scheduled transfer could not be found.");
        }
        return rows.getFirst();
    }

    @Transactional
    public void cancel(UUID userId, String publicId) {
        var user = users.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "UNAUTHENTICATED", "Please sign in again."));
        int updated = jdbc.update("""
                UPDATE scheduled_transfers
                SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND public_id = ? AND status = 'SCHEDULED'
                """, userId, publicId);
        if (updated == 0) {
            throw new BusinessException(409, "SCHEDULED_TRANSFER_NOT_CANCELLABLE",
                    "This scheduled transfer can no longer be cancelled.");
        }
        notifications.publish(new NotificationEvent(
                userId, user.email(), "SCHEDULED_TRANSFER_CANCELLED", "Scheduled transfer cancelled",
                "A scheduled PayFlow transfer was cancelled.", "/app/scheduled-transfers",
                null, null));
    }

    private ScheduledTransferView map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ScheduledTransferView(
                rs.getString("public_id"),
                rs.getString("recipient_email"),
                rs.getBigDecimal("amount").setScale(2).toPlainString(),
                rs.getString("currency"),
                rs.getString("description"),
                rs.getString("reference"),
                rs.getTimestamp("execute_at").toInstant(),
                rs.getString("status"),
                rs.getString("failure_code"),
                rs.getString("failure_message"),
                rs.getString("operation_public_id"),
                rs.getTimestamp("created_at").toInstant());
    }

    public record ScheduledTransferView(String publicId, String recipientEmail, String amount, String currency,
            String description, String reference, Instant executeAt, String status, String failureCode,
            String failureMessage, String operationPublicId, Instant createdAt) {}
}

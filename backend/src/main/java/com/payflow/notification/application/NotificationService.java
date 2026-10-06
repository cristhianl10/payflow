package com.payflow.notification.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.payflow.auth.application.EmailDeliveryService;
import com.payflow.notification.domain.NotificationEvent;
import com.payflow.shared.domain.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JdbcTemplate jdbc;
    private final EmailDeliveryService email;

    public NotificationService(JdbcTemplate jdbc, EmailDeliveryService email) {
        this.jdbc = jdbc;
        this.email = email;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(NotificationEvent event) {
        UUID id = UUID.randomUUID();
        String publicId = "PF-NTF-" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO notifications(id, public_id, user_id, type, title, message, action_url)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, publicId, event.userId(), event.type(), event.title(), event.message(), event.actionUrl());

        if (event.emailSubject() == null || event.emailBody() == null) return;
        try {
            email.send(event.email(), event.emailSubject(), event.emailBody(), "notification");
        } catch (RuntimeException exception) {
            log.error("Notification email delivery failed for user {} and type {}",
                    event.userId(), event.type(), exception);
        }
    }

    @Transactional(readOnly = true)
    public Page list(UUID userId, int page, int size) {
        if (page < 0 || page > 100000 || size < 1 || size > 50) {
            throw new BusinessException(400, "INVALID_FILTER", "Choose a valid notification page.");
        }
        var content = jdbc.query("""
                SELECT public_id, type, title, message, action_url, read_at, created_at
                FROM notifications
                WHERE user_id = ?
                ORDER BY created_at DESC, id DESC
                LIMIT ? OFFSET ?
                """, (rs, row) -> new NotificationView(
                rs.getString("public_id"),
                rs.getString("type"),
                rs.getString("title"),
                rs.getString("message"),
                rs.getString("action_url"),
                rs.getTimestamp("read_at") == null ? null : rs.getTimestamp("read_at").toInstant(),
                rs.getTimestamp("created_at").toInstant()), userId, size, page * size);
        Long total = jdbc.queryForObject("SELECT count(*) FROM notifications WHERE user_id = ?",
                Long.class, userId);
        return new Page(content, page, size, total == null ? 0 : total);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM notifications WHERE user_id = ? AND read_at IS NULL",
                Long.class, userId);
        return count == null ? 0 : count;
    }

    @Transactional
    public void markRead(UUID userId, String publicId) {
        int updated = jdbc.update("""
                UPDATE notifications
                SET read_at = COALESCE(read_at, CURRENT_TIMESTAMP)
                WHERE user_id = ? AND public_id = ?
                """, userId, publicId);
        if (updated == 0) {
            throw new BusinessException(404, "NOTIFICATION_NOT_FOUND", "This notification could not be found.");
        }
    }

    @Transactional
    public void markAllRead(UUID userId) {
        jdbc.update("""
                UPDATE notifications
                SET read_at = COALESCE(read_at, CURRENT_TIMESTAMP)
                WHERE user_id = ? AND read_at IS NULL
                """, userId);
    }

    public record NotificationView(String publicId, String type, String title, String message,
            String actionUrl, Instant readAt, Instant createdAt) {}
    public record Page(List<NotificationView> content, int page, int size, long totalElements) {}
}

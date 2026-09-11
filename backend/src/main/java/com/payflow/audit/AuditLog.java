package com.payflow.audit;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditLog {
    private final JdbcTemplate jdbc;
    public AuditLog(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String event, UUID user, UUID entity) {
        jdbc.update("INSERT INTO audit_logs(id, event_type, actor_user_id, entity_id) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), event, user, entity);
    }
}

package com.payflow.transaction.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.payflow.shared.domain.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TransactionQueries {
    private static final String SELECT = """
            SELECT j.*, sw.user_id AS sender_user_id,
              su.first_name AS sender_first_name, su.last_name AS sender_last_name,
              ru.first_name AS receiver_first_name, ru.last_name AS receiver_last_name
            FROM journal_operations j
            LEFT JOIN wallets sw ON sw.id = j.sender_wallet_id
            LEFT JOIN users su ON su.id = sw.user_id
            JOIN wallets rw ON rw.id = j.receiver_wallet_id
            JOIN users ru ON ru.id = rw.user_id
            """;
    private final JdbcTemplate jdbc;
    public TransactionQueries(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Page history(UUID user, int page, int size, String direction) {
        validateDirection(direction);
        if (page < 0 || page > 100000 || size < 1 || size > 50) {
            throw new BusinessException(400, "INVALID_FILTER", "Choose a valid page and activity filter.");
        }
        String condition = condition(direction);
        Object[] owners = direction.equals("all") ? new Object[]{user, user} : new Object[]{user};
        Object[] arguments = direction.equals("all") ? new Object[]{user, user, size, page * size}
                : new Object[]{user, size, page * size};
        var items = jdbc.query(SELECT + " WHERE " + condition + " ORDER BY j.created_at DESC, j.id DESC LIMIT ? OFFSET ?",
                (rs, row) -> map(rs, user), arguments);
        Long count = jdbc.queryForObject("SELECT count(*) FROM (" + SELECT + " WHERE " + condition + ") matches", Long.class, owners);
        return new Page(items, page, size, count == null ? 0 : count);
    }

    public String exportCsv(UUID user, String direction) {
        validateDirection(direction);
        String condition = condition(direction);
        Object[] owners = direction.equals("all") ? new Object[]{user, user} : new Object[]{user};
        var items = jdbc.query(SELECT + " WHERE " + condition + " ORDER BY j.created_at DESC, j.id DESC",
                (rs, row) -> map(rs, user), owners);
        var csv = new StringBuilder("transaction_id,kind,status,direction,amount,currency,counterparty,sender,receiver,description,reference,created_at\n");
        for (var item : items) {
            csv.append(csv(item.publicId())).append(',')
                    .append(csv(item.kind())).append(',')
                    .append(csv(item.status())).append(',')
                    .append(csv(item.direction())).append(',')
                    .append(csv(item.amount())).append(',')
                    .append(csv(item.currency())).append(',')
                    .append(csv(item.counterparty())).append(',')
                    .append(csv(item.sender())).append(',')
                    .append(csv(item.receiver())).append(',')
                    .append(csv(item.description())).append(',')
                    .append(csv(item.reference())).append(',')
                    .append(csv(item.createdAt().toString())).append('\n');
        }
        return csv.toString();
    }

    private void validateDirection(String direction) {
        if (!List.of("all", "sent", "received").contains(direction)) {
            throw new BusinessException(400, "INVALID_FILTER", "Choose a valid activity filter.");
        }
    }

    private String condition(String direction) {
        return switch (direction) {
            case "sent" -> "sw.user_id = ?";
            case "received" -> "rw.user_id = ?";
            default -> "(sw.user_id = ? OR rw.user_id = ?)";
        };
    }

    public TransactionView detail(UUID user, String publicId) {
        return jdbc.query(SELECT + " WHERE j.public_id = ? AND (sw.user_id = ? OR rw.user_id = ?)",
                (rs, row) -> map(rs, user), publicId, user, user).stream().findFirst()
                .orElseThrow(() -> new BusinessException(404, "TRANSACTION_NOT_FOUND", "This transaction is not available."));
    }

    private TransactionView map(ResultSet rs, UUID user) throws SQLException {
        boolean sent = user.equals(rs.getObject("sender_user_id", UUID.class));
        String kind = rs.getString("kind");
        String sender = kind.equals("SANDBOX_GRANT") ? "PayFlow Sandbox"
                : rs.getString("sender_first_name") + " " + rs.getString("sender_last_name").substring(0, 1) + ".";
        String receiver = rs.getString("receiver_first_name") + " " + rs.getString("receiver_last_name").substring(0, 1) + ".";
        return new TransactionView(rs.getString("public_id"), kind, rs.getString("status"),
                sent ? "sent" : "received",
                rs.getBigDecimal("amount").setScale(2).toPlainString(), rs.getString("currency"),
                sent ? receiver : sender, sender, receiver, rs.getString("description"),
                rs.getString("reference"), rs.getTimestamp("created_at").toInstant());
    }

    private String csv(String value) {
        if (value == null) return "";
        String quote = String.valueOf('"');
        return quote + value.replace(quote, quote + quote) + quote;
    }

    public record TransactionView(String publicId, String kind, String status, String direction, String amount,
            String currency, String counterparty, String sender, String receiver, String description, String reference,
            Instant createdAt) {}
    public record Page(List<TransactionView> content, int page, int size, long totalElements) {}
}

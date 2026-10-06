package com.payflow.transaction.application;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.payflow.shared.domain.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TransactionQueries {
    private static final String SELECT = """
            SELECT j.*, sw.user_id AS sender_user_id,
              su.first_name AS sender_first_name, su.last_name AS sender_last_name, su.email AS sender_email,
              ru.first_name AS receiver_first_name, ru.last_name AS receiver_last_name, ru.email AS receiver_email
            FROM journal_operations j
            LEFT JOIN wallets sw ON sw.id = j.sender_wallet_id
            LEFT JOIN users su ON su.id = sw.user_id
            JOIN wallets rw ON rw.id = j.receiver_wallet_id
            JOIN users ru ON ru.id = rw.user_id
            """;

    private final JdbcTemplate jdbc;

    public TransactionQueries(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Page history(UUID user, int page, int size, Filters filters) {
        if (page < 0 || page > 100000 || size < 1 || size > 50) {
            throw invalidFilter();
        }
        Query query = query(user, filters);
        var arguments = new ArrayList<>(query.arguments());
        arguments.add(size);
        arguments.add(page * size);

        var items = jdbc.query(
                SELECT + " WHERE " + query.where() + " ORDER BY " + query.orderBy() + " LIMIT ? OFFSET ?",
                (rs, row) -> map(rs, user), arguments.toArray());
        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM (" + SELECT + " WHERE " + query.where() + ") matches",
                Long.class, query.arguments().toArray());
        return new Page(items, page, size, count == null ? 0 : count);
    }

    public String exportCsv(UUID user, Filters filters) {
        Query query = query(user, filters);
        var items = jdbc.query(
                SELECT + " WHERE " + query.where() + " ORDER BY " + query.orderBy(),
                (rs, row) -> map(rs, user), query.arguments().toArray());

        var csv = new StringBuilder(
                "transaction_id,kind,status,direction,amount,currency,counterparty,sender,receiver,description,reference,created_at\n");
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

    public TransactionView detail(UUID user, String publicId) {
        return jdbc.query(SELECT + " WHERE j.public_id = ? AND (sw.user_id = ? OR rw.user_id = ?)",
                (rs, row) -> map(rs, user), publicId, user, user).stream().findFirst()
                .orElseThrow(() -> new BusinessException(404, "TRANSACTION_NOT_FOUND",
                        "This transaction is not available."));
    }

    private Query query(UUID user, Filters filters) {
        String direction = normalized(filters.direction(), "all");
        String kind = normalized(filters.kind(), "all");
        String status = normalized(filters.status(), "all");
        String sort = normalized(filters.sort(), "newest");

        if (!List.of("all", "sent", "received").contains(direction)
                || !List.of("all", "transfer", "grant").contains(kind)
                || !List.of("all", "completed").contains(status)
                || !List.of("newest", "oldest", "amount_desc", "amount_asc").contains(sort)) {
            throw invalidFilter();
        }

        var conditions = new ArrayList<String>();
        var arguments = new ArrayList<Object>();

        switch (direction) {
            case "sent" -> {
                conditions.add("sw.user_id = ?");
                arguments.add(user);
            }
            case "received" -> {
                conditions.add("rw.user_id = ?");
                arguments.add(user);
            }
            default -> {
                conditions.add("(sw.user_id = ? OR rw.user_id = ?)");
                arguments.add(user);
                arguments.add(user);
            }
        }

        if (kind.equals("transfer")) conditions.add("j.kind = 'TRANSFER'");
        if (kind.equals("grant")) conditions.add("j.kind = 'SANDBOX_GRANT'");
        if (status.equals("completed")) conditions.add("j.status = 'COMPLETED'");

        String search = filters.search() == null ? "" : filters.search().strip().toLowerCase(Locale.ROOT);
        if (!search.isEmpty()) {
            if (search.length() > 100) throw invalidFilter();
            conditions.add("""
                    (LOWER(j.public_id) LIKE ? OR LOWER(COALESCE(j.reference, '')) LIKE ?
                     OR LOWER(j.description) LIKE ? OR LOWER(COALESCE(su.email, '')) LIKE ?
                     OR LOWER(ru.email) LIKE ?
                     OR LOWER(CONCAT_WS(' ', su.first_name, su.last_name, ru.first_name, ru.last_name)) LIKE ?)
                    """);
            String pattern = "%" + search + "%";
            for (int i = 0; i < 6; i++) arguments.add(pattern);
        }

        LocalDate from = parseDate(filters.from());
        LocalDate to = parseDate(filters.to());
        if (from != null && to != null && from.isAfter(to)) throw invalidFilter();
        if (from != null) {
            conditions.add("j.created_at >= ?");
            arguments.add(Timestamp.from(from.atStartOfDay(ZoneOffset.UTC).toInstant()));
        }
        if (to != null) {
            conditions.add("j.created_at < ?");
            arguments.add(Timestamp.from(to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        }

        BigDecimal min = parseAmount(filters.minAmount());
        BigDecimal max = parseAmount(filters.maxAmount());
        if (min != null && max != null && min.compareTo(max) > 0) throw invalidFilter();
        if (min != null) {
            conditions.add("j.amount >= ?");
            arguments.add(min);
        }
        if (max != null) {
            conditions.add("j.amount <= ?");
            arguments.add(max);
        }

        String orderBy = switch (sort) {
            case "oldest" -> "j.created_at ASC, j.id ASC";
            case "amount_desc" -> "j.amount DESC, j.created_at DESC, j.id DESC";
            case "amount_asc" -> "j.amount ASC, j.created_at DESC, j.id DESC";
            default -> "j.created_at DESC, j.id DESC";
        };

        return new Query(String.join(" AND ", conditions), arguments, orderBy);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw invalidFilter();
        }
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) return null;
        if (!value.matches("(?:0|[1-9][0-9]{0,14})(?:\\.[0-9]{1,2})?")) throw invalidFilter();
        BigDecimal amount = new BigDecimal(value);
        if (amount.signum() < 0) throw invalidFilter();
        return amount;
    }

    private String normalized(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip().toLowerCase(Locale.ROOT);
    }

    private BusinessException invalidFilter() {
        return new BusinessException(400, "INVALID_FILTER", "Choose valid activity filters.");
    }

    private TransactionView map(ResultSet rs, UUID user) throws SQLException {
        boolean sent = user.equals(rs.getObject("sender_user_id", UUID.class));
        String kind = rs.getString("kind");
        String sender = kind.equals("SANDBOX_GRANT") ? "PayFlow Sandbox"
                : rs.getString("sender_first_name") + " " + rs.getString("sender_last_name").substring(0, 1) + ".";
        String receiver = rs.getString("receiver_first_name") + " "
                + rs.getString("receiver_last_name").substring(0, 1) + ".";
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

    private record Query(String where, List<Object> arguments, String orderBy) {}

    public record Filters(String direction, String kind, String status, String search, String from, String to,
            String minAmount, String maxAmount, String sort) {}

    public record TransactionView(String publicId, String kind, String status, String direction, String amount,
            String currency, String counterparty, String sender, String receiver, String description, String reference,
            Instant createdAt) {}

    public record Page(List<TransactionView> content, int page, int size, long totalElements) {}
}

package com.payflow.report.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.payflow.shared.domain.BusinessException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StatementReportService {
    private final JdbcTemplate jdbc;

    public StatementReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Statement statement(UUID userId, LocalDate from, LocalDate to) {
        validateRange(from, to);

        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        UUID walletId = jdbc.queryForObject(
                "SELECT id FROM wallets WHERE user_id = ?",
                UUID.class, userId);
        String walletPublicId = jdbc.queryForObject(
                "SELECT public_id FROM wallets WHERE user_id = ?",
                String.class, userId);
        String email = jdbc.queryForObject(
                "SELECT email FROM users WHERE id = ?",
                String.class, userId);

        BigDecimal opening = balanceAt(walletId, start);
        BigDecimal closing = balanceAt(walletId, end);

        var rows = jdbc.query("""
                SELECT j.public_id, j.kind, j.amount, j.currency, j.description, j.reference, j.created_at,
                       sw.user_id AS sender_user_id,
                       COALESCE(su.first_name || ' ' || LEFT(su.last_name, 1) || '.', 'PayFlow Sandbox') AS sender_name,
                       ru.first_name || ' ' || LEFT(ru.last_name, 1) || '.' AS receiver_name
                FROM journal_operations j
                LEFT JOIN wallets sw ON sw.id = j.sender_wallet_id
                LEFT JOIN users su ON su.id = sw.user_id
                JOIN wallets rw ON rw.id = j.receiver_wallet_id
                JOIN users ru ON ru.id = rw.user_id
                WHERE (j.sender_wallet_id = ? OR j.receiver_wallet_id = ?)
                  AND j.created_at >= ?
                  AND j.created_at < ?
                ORDER BY j.created_at ASC, j.id ASC
                """, (rs, row) -> {
            boolean sent = userId.equals(rs.getObject("sender_user_id", UUID.class));
            String kind = rs.getString("kind");
            String direction = kind.equals("SANDBOX_GRANT") ? "received" : (sent ? "sent" : "received");
            String counterparty = kind.equals("SANDBOX_GRANT")
                    ? "PayFlow Sandbox"
                    : (sent ? rs.getString("receiver_name") : rs.getString("sender_name"));
            return new StatementRow(
                    rs.getString("public_id"),
                    direction,
                    rs.getBigDecimal("amount").setScale(2).toPlainString(),
                    rs.getString("currency"),
                    counterparty,
                    rs.getString("description"),
                    rs.getString("reference"),
                    rs.getTimestamp("created_at").toInstant());
        }, walletId, walletId, Timestamp.from(start), Timestamp.from(end));

        BigDecimal sent = BigDecimal.ZERO;
        BigDecimal received = BigDecimal.ZERO;
        for (var row : rows) {
            BigDecimal amount = new BigDecimal(row.amount());
            if (row.direction().equals("sent")) sent = sent.add(amount);
            else if (!row.counterparty().equals("PayFlow Sandbox")) received = received.add(amount);
        }

        return new Statement(
                walletPublicId,
                email,
                from,
                to,
                opening.setScale(2).toPlainString(),
                closing.setScale(2).toPlainString(),
                sent.setScale(2).toPlainString(),
                received.setScale(2).toPlainString(),
                rows);
    }

    public byte[] generatePdf(UUID userId, LocalDate from, LocalDate to) {
        Statement statement = statement(userId, from, to);
        try (PDDocument document = new PDDocument()) {
            PdfWriter writer = new PdfWriter(document);
            writer.title("PayFlow Statement");
            writer.line("Account: " + statement.email(), 11);
            writer.line("Wallet: " + statement.walletPublicId(), 10);
            writer.line("Period: " + statement.from() + " to " + statement.to(), 10);
            writer.spacer(8);
            writer.line("Opening balance: $" + statement.openingBalance() + " USD", 11);
            writer.line("Closing balance: $" + statement.closingBalance() + " USD", 11);
            writer.line("Total sent: $" + statement.totalSent() + " USD", 10);
            writer.line("Total received: $" + statement.totalReceived() + " USD", 10);
            writer.spacer(10);
            writer.heading("Movements");

            if (statement.movements().isEmpty()) {
                writer.line("No movements in this period.", 10);
            } else {
                for (var row : statement.movements()) {
                    writer.line(row.createdAt().toString().substring(0, 10)
                            + " | " + row.direction().toUpperCase()
                            + " | $" + row.amount() + " " + row.currency(), 10);
                    writer.line("  " + row.counterparty()
                            + " | " + fallback(row.reference(), row.publicId()), 9);
                    if (row.description() != null && !row.description().isBlank()) {
                        writer.line("  " + row.description(), 9);
                    }
                    writer.spacer(4);
                }
            }

            writer.spacer(8);
            writer.line("Generated: " + Instant.now(), 8);
            writer.line("PayFlow educational sandbox - no real money is processed.", 8);
            writer.close();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate statement PDF", exception);
        }
    }

    private BigDecimal balanceAt(UUID walletId, Instant before) {
        BigDecimal value = jdbc.queryForObject("""
                SELECT COALESCE(sum(CASE WHEN e.entry_type = 'CREDIT' THEN e.amount ELSE -e.amount END), 0)
                FROM ledger_entries e
                JOIN ledger_accounts a ON a.id = e.account_id
                JOIN journal_operations j ON j.id = e.operation_id
                WHERE a.wallet_id = ? AND j.created_at < ?
                """, BigDecimal.class, walletId, Timestamp.from(before));
        return value == null ? BigDecimal.ZERO : value;
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to) || from.plusYears(1).isBefore(to)) {
            throw new BusinessException(400, "INVALID_REPORT_RANGE",
                    "Choose a valid statement range of up to one year.");
        }
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record Statement(String walletPublicId, String email, LocalDate from, LocalDate to,
            String openingBalance, String closingBalance, String totalSent, String totalReceived,
            List<StatementRow> movements) {}

    public record StatementRow(String publicId, String direction, String amount, String currency,
            String counterparty, String description, String reference, Instant createdAt) {}

    private static final class PdfWriter {
        private final PDDocument document;
        private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        PdfWriter(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        void title(String text) throws IOException {
            write(text, bold, 18);
            spacer(8);
        }

        void heading(String text) throws IOException {
            write(text, bold, 12);
            spacer(4);
        }

        void line(String text, float size) throws IOException {
            write(text, regular, size);
        }

        void spacer(float points) {
            y -= points;
        }

        private void write(String text, PDType1Font font, float size) throws IOException {
            if (y < 55) newPage();
            String safe = text == null ? "" : text.replace("\n", " ").replace("\r", " ");
            if (safe.length() > 105) safe = safe.substring(0, 102) + "...";
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(50, y);
            stream.showText(safe);
            stream.endText();
            y -= size + 5;
        }

        private void newPage() throws IOException {
            if (stream != null) stream.close();
            page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = 740;
        }

        void close() throws IOException {
            if (stream != null) stream.close();
        }
    }
}

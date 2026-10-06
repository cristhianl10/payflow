package com.payflow.transfer.application;

import java.math.BigDecimal;
import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.audit.AuditLog;
import com.payflow.auth.application.AuthService;
import com.payflow.auth.application.TokenService;
import com.payflow.ledger.application.LedgerService;
import com.payflow.notification.application.NotificationPublisher;
import com.payflow.notification.domain.NotificationEvent;
import com.payflow.shared.domain.*;
import com.payflow.transaction.application.TransactionQueries;
import com.payflow.user.domain.UserStatus;
import com.payflow.user.infrastructure.*;
import com.payflow.wallet.infrastructure.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {
    private final WalletRepository wallets;
    private final UserRepository users;
    private final LedgerService ledger;
    private final TransactionQueries transactions;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final AuditLog audit;
    private final NotificationPublisher notifications;
    private final BigDecimal maxPerOperation;
    private final BigDecimal dailyLimit;

    public TransferService(WalletRepository wallets, UserRepository users, LedgerService ledger,
            TransactionQueries transactions, JdbcTemplate jdbc, ObjectMapper json, AuditLog audit,
            NotificationPublisher notifications,
            @Value("${payflow.transfer.max-per-operation:7500.00}") BigDecimal maxPerOperation,
            @Value("${payflow.transfer.daily-limit:15000.00}") BigDecimal dailyLimit) {
        this.wallets = wallets; this.users = users; this.ledger = ledger;
        this.transactions = transactions; this.jdbc = jdbc; this.json = json; this.audit = audit;
        this.notifications = notifications;
        if (maxPerOperation.signum() <= 0 || dailyLimit.signum() <= 0 || dailyLimit.compareTo(maxPerOperation) < 0) {
            throw new IllegalArgumentException("Transfer limits must be positive and daily limit must cover one operation");
        }
        this.maxPerOperation = maxPerOperation;
        this.dailyLimit = dailyLimit;
    }

    @Transactional(readOnly = true)
    public Recipient recipient(UUID sender, String email) {
        var user = recipientUser(sender, email);
        var wallet = wallets.findByUserId(user.id()).orElseThrow(this::unavailable);
        wallet.requireActive();
        return new Recipient(user.firstName() + " " + user.lastName().substring(0, 1) + ".", wallet.publicId(), wallet.currency());
    }

    @Transactional(timeout = 15)
    public String send(UUID sender, UUID key, String recipient, String amount, String currency, String description) {
        return send(sender, key, recipient, amount, currency, description, null);
    }

    @Transactional(timeout = 15)
    public String send(UUID sender, UUID key, String recipient, String amount, String currency, String description,
            String reference) {
        if (!"USD".equals(currency)) throw new BusinessException(400, "UNSUPPORTED_CURRENCY", "Transfers currently support USD only.");
        if (amount == null || !amount.matches("(?:0|[1-9][0-9]{0,14})(?:\\.[0-9]{1,2})?")
                || new BigDecimal(amount).signum() <= 0) {
            throw new BusinessException(400, "INVALID_AMOUNT", "Enter a positive USD amount with at most two decimal places.");
        }
        if (description != null && description.length() > 240) {
            throw new BusinessException(400, "INVALID_DESCRIPTION", "Use a description of at most 240 characters.");
        }
        Money money = new Money(new BigDecimal(amount), Currency.getInstance("USD"));
        if (money.amount().compareTo(maxPerOperation) > 0) {
            throw new BusinessException(422, "TRANSFER_LIMIT_EXCEEDED",
                    "This transfer exceeds the per-operation limit of $" + maxPerOperation.setScale(2) + " USD.");
        }
        String email = AuthService.normalizeEmail(recipient);
        String note = description == null ? "" : description.strip();
        String cleanReference = reference == null ? "" : reference.strip();
        if (cleanReference.length() > 80) {
            throw new BusinessException(400, "INVALID_REFERENCE", "Use a reference of at most 80 characters.");
        }
        String hash = TokenService.hash(serialize(
                List.of(email, money.amount().toPlainString(), currency, note, cleanReference)));
        jdbc.update("DELETE FROM transfer_requests WHERE user_id = ? AND idempotency_key = ? AND expires_at <= CURRENT_TIMESTAMP",
                sender, key);
        // ON CONFLICT waits for an in-flight identical key; rollback releases the reservation.
        int inserted = jdbc.update("""
                INSERT INTO transfer_requests(user_id, idempotency_key, request_hash, expires_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP + INTERVAL '7 days') ON CONFLICT DO NOTHING
                """, sender, key, hash);
        if (inserted == 0) {
            var previous = jdbc.queryForMap("SELECT request_hash, response_body FROM transfer_requests WHERE user_id = ? AND idempotency_key = ?", sender, key);
            if (!hash.equals(previous.get("request_hash"))) {
                throw new BusinessException(409, "IDEMPOTENCY_CONFLICT", "This request reference was already used for different transfer details.");
            }
            if (previous.get("response_body") == null) {
                throw new BusinessException(409, "TRANSFER_IN_PROGRESS", "This transfer is still processing. Retry with the same reference.");
            }
            return (String) previous.get("response_body");
        }
        var receiver = recipientUser(sender, email);
        UUID senderWallet = wallets.findIdByUserId(sender).orElseThrow(this::unavailable);
        UUID receiverWallet = wallets.findIdByUserId(receiver.id()).orElseThrow(this::unavailable);
        var ordered = new ArrayList<>(List.of(senderWallet, receiverWallet));
        ordered.sort(Comparator.comparing(UUID::toString));
        var first = wallets.lockById(ordered.get(0)).orElseThrow(this::unavailable);
        var second = wallets.lockById(ordered.get(1)).orElseThrow(this::unavailable);
        WalletEntity from = first.id().equals(senderWallet) ? first : second;
        WalletEntity to = first.id().equals(receiverWallet) ? first : second;
        Integer active = jdbc.queryForObject("SELECT count(*) FROM users WHERE id IN (?, ?) AND status = 'ACTIVE'", Integer.class, sender, receiver.id());
        if (active == null || active != 2) throw unavailable();

        BigDecimal sentToday = jdbc.queryForObject("""
                SELECT COALESCE(SUM(amount), 0)
                FROM journal_operations
                WHERE sender_wallet_id = ? AND kind = 'TRANSFER' AND status = 'COMPLETED'
                  AND created_at >= date_trunc('day', CURRENT_TIMESTAMP)
                """, BigDecimal.class, from.id());
        if (sentToday == null) sentToday = BigDecimal.ZERO;
        if (sentToday.add(money.amount()).compareTo(dailyLimit) > 0) {
            throw new BusinessException(422, "DAILY_TRANSFER_LIMIT_EXCEEDED",
                    "This transfer would exceed today's $" + dailyLimit.setScale(2) + " USD transfer limit.");
        }

        from.debit(money);
        to.credit(money);
        var operation = ledger.postTransfer(from, to, money, note,
                cleanReference.isEmpty() ? null : cleanReference);
        wallets.flush();
        String result = serialize(transactions.detail(sender, operation.publicId()));
        jdbc.update("""
                UPDATE transfer_requests SET operation_id = ?, response_body = ?, response_status = 201
                WHERE user_id = ? AND idempotency_key = ?
                """, operation.id(), result, sender, key);
        audit.record("TRANSFER_COMPLETED", sender, operation.id());

        UserEntity senderUser = users.findById(sender).orElseThrow(this::unavailable);
        String amountLabel = "$" + money.amount().setScale(2).toPlainString() + " USD";
        notifications.publish(new NotificationEvent(
                senderUser.id(), senderUser.email(), "TRANSFER_SENT", "Transfer sent",
                "You sent " + amountLabel + " to " + receiver.firstName() + " " + receiver.lastName().substring(0, 1) + ".",
                "/app/transactions/" + operation.publicId(),
                "PayFlow transfer sent",
                "Your transfer of " + amountLabel + " to " + receiver.firstName() + " was completed successfully."));
        notifications.publish(new NotificationEvent(
                receiver.id(), receiver.email(), "TRANSFER_RECEIVED", "Money received",
                "You received " + amountLabel + " from " + senderUser.firstName() + " "
                        + senderUser.lastName().substring(0, 1) + ".",
                "/app/transactions/" + operation.publicId(),
                "You received money in PayFlow",
                "You received " + amountLabel + " from " + senderUser.firstName() + " in PayFlow."));

        return result;
    }

    private UserEntity recipientUser(UUID sender, String email) {
        var user = users.findByEmail(AuthService.normalizeEmail(email)).orElseThrow(this::unavailable);
        if (user.id().equals(sender)) throw new BusinessException(422, "SELF_TRANSFER", "Choose another PayFlow user as the recipient.");
        if (user.status() != UserStatus.ACTIVE) throw unavailable();
        return user;
    }

    private BusinessException unavailable() {
        return new BusinessException(422, "RECIPIENT_UNAVAILABLE", "This recipient is not available. Check their PayFlow email.");
    }

    private String serialize(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException(exception); }
    }

    public TransferRules rules() {
        return new TransferRules(maxPerOperation.setScale(2).toPlainString(), dailyLimit.setScale(2).toPlainString(),
                "USD");
    }

    public record Recipient(String displayName, String walletPublicId, String currency) {}
    public record TransferRules(String maxPerOperation, String dailyLimit, String currency) {}
}

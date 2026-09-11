package com.payflow.transfer.application;

import java.math.BigDecimal;
import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.audit.AuditLog;
import com.payflow.auth.application.AuthService;
import com.payflow.auth.application.TokenService;
import com.payflow.ledger.application.LedgerService;
import com.payflow.shared.domain.*;
import com.payflow.transaction.application.TransactionQueries;
import com.payflow.user.domain.UserStatus;
import com.payflow.user.infrastructure.*;
import com.payflow.wallet.infrastructure.*;
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

    public TransferService(WalletRepository wallets, UserRepository users, LedgerService ledger,
            TransactionQueries transactions, JdbcTemplate jdbc, ObjectMapper json, AuditLog audit) {
        this.wallets = wallets; this.users = users; this.ledger = ledger;
        this.transactions = transactions; this.jdbc = jdbc; this.json = json; this.audit = audit;
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
        if (!"USD".equals(currency)) throw new BusinessException(400, "UNSUPPORTED_CURRENCY", "Transfers currently support USD only.");
        if (amount == null || !amount.matches("(?:0|[1-9][0-9]{0,14})(?:\\.[0-9]{1,2})?")
                || new BigDecimal(amount).signum() <= 0) {
            throw new BusinessException(400, "INVALID_AMOUNT", "Enter a positive USD amount with at most two decimal places.");
        }
        if (description != null && description.length() > 240) {
            throw new BusinessException(400, "INVALID_DESCRIPTION", "Use a description of at most 240 characters.");
        }
        Money money = new Money(new BigDecimal(amount), Currency.getInstance("USD"));
        String email = AuthService.normalizeEmail(recipient);
        String note = description == null ? "" : description.strip();
        String hash = TokenService.hash(serialize(List.of(email, money.amount().toPlainString(), currency, note)));
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
        from.debit(money);
        to.credit(money);
        var operation = ledger.postTransfer(from, to, money, note);
        wallets.flush();
        String result = serialize(transactions.detail(sender, operation.publicId()));
        jdbc.update("""
                UPDATE transfer_requests SET operation_id = ?, response_body = ?, response_status = 201
                WHERE user_id = ? AND idempotency_key = ?
                """, operation.id(), result, sender, key);
        audit.record("TRANSFER_COMPLETED", sender, operation.id());
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

    public record Recipient(String displayName, String walletPublicId, String currency) {}
}

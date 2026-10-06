package com.payflow.ledger.application;

import java.util.UUID;
import com.payflow.shared.domain.Money;
import com.payflow.wallet.infrastructure.WalletEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class LedgerService {
    private static final UUID ISSUANCE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final JdbcTemplate jdbc;

    public LedgerService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void openWallet(WalletEntity wallet, Money grant) {
        UUID account = UUID.randomUUID();
        jdbc.update("INSERT INTO ledger_accounts(id, wallet_id, purpose) VALUES (?, ?, 'WALLET')", account, wallet.id());
        if (grant.amount().signum() > 0) {
            post("SANDBOX_GRANT", null, wallet.id(), ISSUANCE, account, grant,
                    "Your opening sandbox funds", null);
            wallet.credit(grant);
        }
    }

    public Operation postTransfer(WalletEntity sender, WalletEntity receiver, Money amount, String description,
            String reference) {
        return post("TRANSFER", sender.id(), receiver.id(), account(sender.id()), account(receiver.id()), amount,
                description, reference);
    }

    private UUID account(UUID wallet) {
        return jdbc.queryForObject("SELECT id FROM ledger_accounts WHERE wallet_id = ?", UUID.class, wallet);
    }

    private Operation post(String kind, UUID sender, UUID receiver, UUID debit, UUID credit, Money money,
            String description, String reference) {
        UUID id = UUID.randomUUID();
        String publicId = "PF-TX-" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO journal_operations(id, public_id, kind, sender_wallet_id, receiver_wallet_id, amount,
                        description, reference)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, publicId, kind, sender, receiver, money.amount(), description, reference);
        jdbc.update("""
                INSERT INTO ledger_entries(id, operation_id, account_id, entry_type, amount)
                VALUES (?, ?, ?, 'DEBIT', ?), (?, ?, ?, 'CREDIT', ?)
                """, UUID.randomUUID(), id, debit, money.amount(), UUID.randomUUID(), id, credit, money.amount());
        return new Operation(id, publicId);
    }

    public record Operation(UUID id, String publicId) {}
}

package com.payflow.wallet.application;

import java.math.BigDecimal;
import java.util.UUID;
import com.payflow.shared.domain.BusinessException;
import com.payflow.wallet.infrastructure.WalletRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WalletQueries {
    private final WalletRepository wallets;
    private final JdbcTemplate jdbc;
    public WalletQueries(WalletRepository wallets, JdbcTemplate jdbc) { this.wallets = wallets; this.jdbc = jdbc; }

    public WalletView mine(UUID user) {
        var wallet = wallets.findByUserId(user).orElseThrow(() ->
                new BusinessException(404, "WALLET_NOT_FOUND", "Your wallet is not available."));
        return new WalletView(wallet.publicId(), wallet.currency(), wallet.balance().amount().toPlainString(),
                wallet.status().name(), total(wallet.id(), true), total(wallet.id(), false));
    }

    public WalletView owned(UUID user, String publicId) {
        var wallet = mine(user);
        if (!wallet.publicId.equals(publicId)) {
            throw new BusinessException(404, "WALLET_NOT_FOUND", "This wallet is not available.");
        }
        return wallet;
    }

    private String total(UUID wallet, boolean sent) {
        BigDecimal value = jdbc.queryForObject("SELECT COALESCE(sum(amount), 0) FROM journal_operations WHERE kind = 'TRANSFER' AND "
                + (sent ? "sender_wallet_id" : "receiver_wallet_id") + " = ?", BigDecimal.class, wallet);
        return value.setScale(2).toPlainString();
    }

    public record WalletView(String publicId, String currency, String availableBalance, String status,
            String totalSent, String totalReceived) {}
}

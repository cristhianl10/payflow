package com.payflow.wallet.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;
import jakarta.persistence.*;
import com.payflow.shared.domain.BusinessException;
import com.payflow.shared.domain.Money;
import com.payflow.wallet.domain.WalletStatus;

@Entity
@Table(name = "wallets")
public class WalletEntity {
    @Id private UUID id;
    @Column(nullable = false, length = 64) private String publicId;
    @Column(nullable = false) private UUID userId;
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.CHAR)
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal availableBalance;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private WalletStatus status;
    @Version private long version;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected WalletEntity() {}

    public WalletEntity(UUID userId) {
        id = UUID.randomUUID();
        publicId = "PF-WLT-" + UUID.randomUUID();
        this.userId = userId;
        currency = "USD";
        availableBalance = BigDecimal.ZERO;
        status = WalletStatus.ACTIVE;
        createdAt = updatedAt = Instant.now();
    }

    public void credit(Money money) {
        requireActive();
        BigDecimal next = balance().add(money).amount();
        if (next.compareTo(new BigDecimal("999999999999999.99")) > 0) {
            throw new BusinessException(422, "BALANCE_LIMIT", "This transfer would exceed the wallet balance limit.");
        }
        availableBalance = next;
        updatedAt = Instant.now();
    }

    public void debit(Money money) {
        requireActive();
        if (balance().compareTo(money) < 0) {
            throw new BusinessException(422, "INSUFFICIENT_FUNDS", "Your balance is too low for this transfer.");
        }
        availableBalance = balance().subtract(money).amount();
        updatedAt = Instant.now();
    }

    public void requireActive() {
        if (status != WalletStatus.ACTIVE) {
            throw new BusinessException(422, "WALLET_UNAVAILABLE", "This wallet cannot make or receive transfers.");
        }
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public String publicId() { return publicId; }
    public String currency() { return currency; }
    public WalletStatus status() { return status; }
    public Money balance() { return new Money(availableBalance, Currency.getInstance(currency)); }
}

package com.payflow.shared.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MoneyTest {
    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void normalizesScaleForEqualityAndHashing() {
        Money whole = usd("10");
        assertEquals(usd("10.00"), whole);
        assertEquals(usd("10.0000").hashCode(), whole.hashCode());
        assertEquals(2, whole.amount().scale());
    }

    @Test
    void performsExactArithmetic() {
        assertEquals(usd("0.30"), usd("0.10").add(usd("0.20")));
        assertEquals(usd("750.00"), usd("1000").subtract(usd("250")));
        assertEquals(0, usd("1.00").compareTo(usd("1")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "-10", "0.001", "1.999"})
    void rejectsNegativeOrFractionalCents(String amount) {
        assertThrows(IllegalArgumentException.class, () -> usd(amount));
    }

    @Test
    void rejectsOverspending() {
        assertThrows(IllegalArgumentException.class, () -> usd("100").subtract(usd("100.01")));
    }

    @Test
    void rejectsCrossCurrencyOperations() {
        Money eur = new Money(BigDecimal.ONE, Currency.getInstance("EUR"));
        assertThrows(IllegalArgumentException.class, () -> usd("1").add(eur));
        assertThrows(IllegalArgumentException.class, () -> usd("1").subtract(eur));
        assertThrows(IllegalArgumentException.class, () -> usd("1").compareTo(eur));
    }

    @Test
    void supportsOtherMinorUnitsWithoutEnablingOtherWalletCurrencies() {
        assertEquals(0, new Money(BigDecimal.TEN, Currency.getInstance("JPY")).amount().scale());
        assertEquals(3, new Money(new BigDecimal("1.123"), Currency.getInstance("KWD")).amount().scale());
        assertThrows(IllegalArgumentException.class,
                () -> new Money(new BigDecimal("0.1"), Currency.getInstance("JPY")));
    }

    @Test
    void rejectsMissingValuesAndCurrenciesWithoutMinorUnits() {
        assertThrows(NullPointerException.class, () -> new Money(null, USD));
        assertThrows(NullPointerException.class, () -> new Money(BigDecimal.ZERO, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Money(BigDecimal.ZERO, Currency.getInstance("XXX")));
    }

    private Money usd(String amount) {
        return new Money(new BigDecimal(amount), USD);
    }
}

package br.com.sigla.domain.billing;

import br.com.sigla.domain.shared.DomainException;

import java.math.BigDecimal;
import java.util.Objects;

public final class BillingLedger {

    private final String accountId;
    private BigDecimal balance;

    public BillingLedger(String accountId, BigDecimal openingBalance) {
        this.accountId = Objects.requireNonNull(accountId, "accountId is required");
        this.balance = Objects.requireNonNull(openingBalance, "openingBalance is required");
    }

    public String accountId() {
        return accountId;
    }

    public BigDecimal balance() {
        return balance;
    }

    public void recordDebit(BigDecimal value) {
        validatePositive(value);
        balance = balance.subtract(value);
    }

    public void recordCredit(BigDecimal value) {
        validatePositive(value);
        balance = balance.add(value);
    }

    private static void validatePositive(BigDecimal value) {
        if (Objects.requireNonNull(value, "value is required").signum() <= 0) {
            throw new DomainException("Value must be greater than zero");
        }
    }
}

package br.com.sigla.application.billing.dto;

import java.math.BigDecimal;

public record BillingEntryCommand(
        String accountId,
        BigDecimal amount
) {
}

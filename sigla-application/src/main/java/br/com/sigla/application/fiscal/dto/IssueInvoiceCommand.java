package br.com.sigla.application.fiscal.dto;

import java.math.BigDecimal;

public record IssueInvoiceCommand(
        String operationId,
        String customerId,
        BigDecimal amount
) {
}

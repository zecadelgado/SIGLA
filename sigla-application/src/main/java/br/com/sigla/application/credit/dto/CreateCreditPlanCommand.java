package br.com.sigla.application.credit.dto;

import java.util.List;

public record CreateCreditPlanCommand(
        String planId,
        String customerId,
        List<InstallmentInput> installments
) {
    public record InstallmentInput(
            int number,
            String dueDate,
            boolean paid
    ) {
    }
}

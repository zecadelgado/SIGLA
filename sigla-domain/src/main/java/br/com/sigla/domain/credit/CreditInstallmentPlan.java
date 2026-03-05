package br.com.sigla.domain.credit;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record CreditInstallmentPlan(
        String planId,
        String customerId,
        List<Installment> installments
) {
    public CreditInstallmentPlan {
        Objects.requireNonNull(planId, "planId is required");
        Objects.requireNonNull(customerId, "customerId is required");
        installments = List.copyOf(installments);
    }

    public record Installment(
            int number,
            LocalDate dueDate,
            boolean paid
    ) {
        public Installment {
            Objects.requireNonNull(dueDate, "dueDate is required");
        }
    }
}

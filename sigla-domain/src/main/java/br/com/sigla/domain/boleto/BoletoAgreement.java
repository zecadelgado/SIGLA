package br.com.sigla.domain.boleto;

import java.time.LocalDate;
import java.util.Objects;

public record BoletoAgreement(
        String agreementId,
        LocalDate dueDate,
        boolean paid
) {
    public BoletoAgreement {
        Objects.requireNonNull(agreementId, "agreementId is required");
        Objects.requireNonNull(dueDate, "dueDate is required");
    }
}

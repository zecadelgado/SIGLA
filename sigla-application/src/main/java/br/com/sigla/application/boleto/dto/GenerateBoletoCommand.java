package br.com.sigla.application.boleto.dto;

import java.time.LocalDate;

public record GenerateBoletoCommand(
        String agreementId,
        LocalDate dueDate
) {
}

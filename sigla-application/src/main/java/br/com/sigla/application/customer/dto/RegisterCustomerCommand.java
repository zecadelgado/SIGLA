package br.com.sigla.application.customer.dto;

import java.time.LocalDate;

public record RegisterCustomerCommand(
        String customerId,
        String fullName,
        LocalDate birthDate,
        boolean hasPrescription,
        String glassesType,
        boolean paymentUpToDate
) {
}

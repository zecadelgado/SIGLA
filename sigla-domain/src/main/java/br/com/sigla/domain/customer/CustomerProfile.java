package br.com.sigla.domain.customer;

import java.time.LocalDate;
import java.util.Objects;

public record CustomerProfile(
        String customerId,
        String fullName,
        LocalDate birthDate,
        boolean hasPrescription,
        String glassesType,
        boolean paymentUpToDate
) {
    public CustomerProfile {
        Objects.requireNonNull(customerId, "customerId is required");
        Objects.requireNonNull(fullName, "fullName is required");
        Objects.requireNonNull(birthDate, "birthDate is required");
        Objects.requireNonNull(glassesType, "glassesType is required");
    }
}

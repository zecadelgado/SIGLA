package br.com.sigla.dominio.certificados;

import java.time.LocalDate;
import java.util.Objects;

public record Certificado(
        String id,
        String serviceProvidedId,
        LocalDate issuedOn,
        LocalDate validUntil,
        CertificadoStatus status,
        int renewalAlertDays
) {
    public Certificado {
        id = requireText(id, "id");
        serviceProvidedId = requireText(serviceProvidedId, "serviceProvidedId");
        issuedOn = Objects.requireNonNull(issuedOn, "issuedOn is required");
        validUntil = Objects.requireNonNull(validUntil, "validUntil is required");
        status = Objects.requireNonNull(status, "status is required");
        if (validUntil.isBefore(issuedOn)) {
            throw new IllegalArgumentException("validUntil must not be before issuedOn");
        }
        if (renewalAlertDays < 0) {
            throw new IllegalArgumentException("renewalAlertDays must not be negative");
        }
    }

    public boolean isExpiringWithin(LocalDate referenceDate) {
        Objects.requireNonNull(referenceDate, "referenceDate is required");
        if (status != CertificadoStatus.ACTIVE) {
            return false;
        }
        LocalDate limitDate = referenceDate.plusDays(renewalAlertDays);
        return !validUntil.isBefore(referenceDate) && !validUntil.isAfter(limitDate);
    }

    public enum CertificadoStatus {
        ACTIVE,
        EXPIRED,
        REPLACED
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}


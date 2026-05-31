package br.com.sigla.dominio.certificados;

import java.time.LocalDate;
import java.util.Objects;

public record Certificado(
        String id,
        String customerId,
        String serviceProvidedId,
        String orderId,
        String description,
        LocalDate issuedOn,
        LocalDate validUntil,
        int intervalMonths,
        boolean alertActive,
        CertificadoStatus status,
        int renewalAlertDays,
        String notes
) {
    public Certificado {
        id = requireText(id, "id");
        customerId = requireText(customerId, "customerId");
        serviceProvidedId = normalizeOptional(serviceProvidedId);
        orderId = normalizeOptional(orderId);
        description = normalizeOptional(description);
        issuedOn = Objects.requireNonNull(issuedOn, "issuedOn is required");
        intervalMonths = intervalMonths <= 0 ? 6 : intervalMonths;
        validUntil = validUntil == null ? issuedOn.plusMonths(intervalMonths) : validUntil;
        status = Objects.requireNonNull(status, "status is required");
        if (validUntil.isBefore(issuedOn)) {
            throw new IllegalArgumentException("validUntil must not be before issuedOn");
        }
        if (renewalAlertDays < 0) {
            throw new IllegalArgumentException("renewalAlertDays must not be negative");
        }
        renewalAlertDays = renewalAlertDays == 0 ? 15 : renewalAlertDays;
        notes = normalizeOptional(notes);
    }

    public Certificado(
            String id,
            String serviceProvidedId,
            LocalDate issuedOn,
            LocalDate validUntil,
            CertificadoStatus status,
            int renewalAlertDays
    ) {
        this(id, serviceProvidedId, "", "", "Certificado de higiene", issuedOn, validUntil, 6, true, status, renewalAlertDays, "");
    }

    public boolean isExpiringWithin(LocalDate referenceDate) {
        Objects.requireNonNull(referenceDate, "referenceDate is required");
        if (status == CertificadoStatus.REPLACED || isExpired(referenceDate)) {
            return false;
        }
        LocalDate limitDate = referenceDate.plusDays(renewalAlertDays);
        return !validUntil.isBefore(referenceDate) && !validUntil.isAfter(limitDate);
    }

    public boolean shouldNotify(LocalDate referenceDate) {
        Objects.requireNonNull(referenceDate, "referenceDate is required");
        return alertActive
                && status != CertificadoStatus.REPLACED
                && !referenceDate.isBefore(validUntil.minusDays(renewalAlertDays));
    }

    public boolean isExpired(LocalDate referenceDate) {
        Objects.requireNonNull(referenceDate, "referenceDate is required");
        return status == CertificadoStatus.EXPIRED || validUntil.isBefore(referenceDate);
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

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }
}


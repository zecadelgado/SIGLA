package br.com.sigla.dominio.contratos;

import java.time.LocalDate;
import java.util.Objects;

public record Contrato(
        String id,
        String customerId,
        LocalDate startDate,
        LocalDate endDate,
        ContratoType type,
        ServiceFrequency serviceFrequency,
        ContratoStatus status,
        RenewalRule renewalRule,
        int alertDaysBeforeEnd
) {
    public Contrato {
        id = requireText(id, "id");
        customerId = requireText(customerId, "customerId");
        startDate = Objects.requireNonNull(startDate, "startDate is required");
        endDate = Objects.requireNonNull(endDate, "endDate is required");
        type = Objects.requireNonNull(type, "type is required");
        serviceFrequency = Objects.requireNonNull(serviceFrequency, "serviceFrequency is required");
        status = Objects.requireNonNull(status, "status is required");
        renewalRule = Objects.requireNonNull(renewalRule, "renewalRule is required");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        if (alertDaysBeforeEnd < 0) {
            throw new IllegalArgumentException("alertDaysBeforeEnd must not be negative");
        }
    }

    public boolean isExpiringWithin(LocalDate referenceDate) {
        Objects.requireNonNull(referenceDate, "referenceDate is required");
        if (status != ContratoStatus.ACTIVE) {
            return false;
        }
        LocalDate limitDate = referenceDate.plusDays(alertDaysBeforeEnd);
        return !endDate.isBefore(referenceDate) && !endDate.isAfter(limitDate);
    }

    public enum ContratoType {
        MONTHLY,
        QUINZENAL,
        AVULSO,
        CORPORATE
    }

    public enum ServiceFrequency {
        MONTHLY,
        BIWEEKLY,
        ONE_OFF
    }

    public enum ContratoStatus {
        DRAFT,
        ACTIVE,
        EXPIRED,
        CANCELLED
    }

    public enum RenewalRule {
        MANUAL,
        AUTO_RENEW
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}


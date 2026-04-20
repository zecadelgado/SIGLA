package br.com.sigla.dominio.agenda;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public record VisitaAgendada(
        String id,
        String customerId,
        String contractId,
        VisitType type,
        LocalDate scheduledDate,
        String title,
        String serviceType,
        String internalResponsible,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean allDay,
        VisitStatus status,
        String notes
) {
    public VisitaAgendada {
        id = requireText(id, "id");
        customerId = requireText(customerId, "customerId");
        contractId = normalizeOptional(contractId);
        type = Objects.requireNonNull(type, "type is required");
        scheduledDate = Objects.requireNonNull(scheduledDate, "scheduledDate is required");
        title = normalizeOptional(title);
        serviceType = normalizeOptional(serviceType);
        internalResponsible = normalizeOptional(internalResponsible);
        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("endAt must not be before startAt");
        }
        status = Objects.requireNonNull(status, "status is required");
        notes = normalizeOptional(notes);
    }

    public VisitaAgendada(
            String id,
            String customerId,
            String contractId,
            VisitType type,
            LocalDate scheduledDate,
            VisitStatus status,
            String notes
    ) {
        this(
                id,
                customerId,
                contractId,
                type,
                scheduledDate,
                "",
                "",
                "",
                scheduledDate.atStartOfDay(),
                scheduledDate.atStartOfDay(),
                true,
                status,
                notes
        );
    }

    public boolean isUpcomingWithin(LocalDate referenceDate, int days) {
        Objects.requireNonNull(referenceDate, "referenceDate is required");
        if (status != VisitStatus.SCHEDULED) {
            return false;
        }
        LocalDate limitDate = referenceDate.plusDays(days);
        return !scheduledDate.isBefore(referenceDate) && !scheduledDate.isAfter(limitDate);
    }

    public boolean isOverdue(LocalDate referenceDate) {
        Objects.requireNonNull(referenceDate, "referenceDate is required");
        return status == VisitStatus.SCHEDULED && scheduledDate.isBefore(referenceDate);
    }

    public enum VisitType {
        MONTHLY,
        BIWEEKLY,
        ONE_OFF
    }

    public enum VisitStatus {
        SCHEDULED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        MISSED
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}


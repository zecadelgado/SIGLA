package br.com.sigla.dominio.agenda;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record VisitaAgendada(
        String id,
        String customerId,
        String orderId,
        String contractId,
        String certificateId,
        VisitType type,
        Recurrence recurrence,
        LocalDate scheduledDate,
        String title,
        String serviceType,
        String internalResponsible,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean allDay,
        VisitStatus status,
        VisitPriority priority,
        String responsibleId,
        boolean reminderActive,
        int reminderDaysBefore,
        String notes
) {
    public VisitaAgendada {
        id = requireText(id, "id");
        customerId = requireText(customerId, "customerId");
        orderId = normalizeOptional(orderId);
        contractId = normalizeOptional(contractId);
        certificateId = normalizeOptional(certificateId);
        type = Objects.requireNonNull(type, "type is required");
        recurrence = Objects.requireNonNullElse(recurrence, recurrenceFromType(type));
        scheduledDate = Objects.requireNonNull(scheduledDate, "scheduledDate is required");
        title = normalizeOptional(title);
        serviceType = normalizeOptional(serviceType);
        internalResponsible = normalizeOptional(internalResponsible);
        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("endAt must not be before startAt");
        }
        status = Objects.requireNonNull(status, "status is required");
        priority = Objects.requireNonNullElse(priority, VisitPriority.NORMAL);
        responsibleId = normalizeOptional(responsibleId);
        if (reminderDaysBefore < 0) {
            throw new IllegalArgumentException("reminderDaysBefore must not be negative");
        }
        notes = normalizeOptional(notes);
    }

    public VisitaAgendada(
            String id,
            String customerId,
            String contractId,
            String certificateId,
            VisitType type,
            LocalDate scheduledDate,
            String title,
            String serviceType,
            String internalResponsible,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean allDay,
            VisitStatus status,
            VisitPriority priority,
            String responsibleId,
            String notes
    ) {
        this(
                id,
                customerId,
                "",
                contractId,
                certificateId,
                type,
                recurrenceFromType(type),
                scheduledDate,
                title,
                serviceType,
                internalResponsible,
                startAt,
                endAt,
                allDay,
                status,
                priority,
                responsibleId,
                false,
                0,
                notes
        );
    }

    public VisitaAgendada(
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
        this(
                id,
                customerId,
                "",
                contractId,
                "",
                type,
                recurrenceFromType(type),
                scheduledDate,
                title,
                serviceType,
                internalResponsible,
                startAt,
                endAt,
                allDay,
                status,
                VisitPriority.NORMAL,
                "",
                false,
                0,
                notes
        );
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
                "",
                contractId,
                "",
                type,
                recurrenceFromType(type),
                scheduledDate,
                "",
                "",
                "",
                scheduledDate.atStartOfDay(),
                scheduledDate.atStartOfDay(),
                true,
                status,
                VisitPriority.NORMAL,
                "",
                false,
                0,
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

    public boolean isOperational() {
        String eventType = serviceType == null ? "" : serviceType;
        return (contractId == null || contractId.isBlank())
                && (certificateId == null || certificateId.isBlank())
                && !eventType.equalsIgnoreCase("contrato_vencimento")
                && !eventType.equalsIgnoreCase("certificado_vencimento");
    }

    public boolean conflictsWith(VisitaAgendada other) {
        Objects.requireNonNull(other, "other is required");
        if (id.equals(other.id) || status == VisitStatus.CANCELLED || other.status == VisitStatus.CANCELLED) {
            return false;
        }
        if (responsibleId == null || responsibleId.isBlank() || !responsibleId.equals(other.responsibleId)) {
            return false;
        }
        if (!isOperational() || !other.isOperational()) {
            return false;
        }
        LocalDateTime thisStart = effectiveStart();
        LocalDateTime thisEnd = effectiveEnd();
        LocalDateTime otherStart = other.effectiveStart();
        LocalDateTime otherEnd = other.effectiveEnd();
        return thisStart.isBefore(otherEnd) && thisEnd.isAfter(otherStart);
    }

    public List<VisitaAgendada> occurrencesBetween(LocalDate start, LocalDate end) {
        Objects.requireNonNull(start, "start is required");
        Objects.requireNonNull(end, "end is required");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end must not be before start");
        }
        List<VisitaAgendada> occurrences = new java.util.ArrayList<>();
        LocalDate current = scheduledDate;
        int guard = 0;
        while (current.isBefore(start)) {
            current = nextOccurrence(current);
            guard++;
            if (guard > 520) {
                return occurrences;
            }
        }
        while (!current.isAfter(end)) {
            occurrences.add(onDate(current));
            current = nextOccurrence(current);
            if (recurrence == Recurrence.NONE) {
                break;
            }
        }
        return occurrences;
    }

    private VisitaAgendada onDate(LocalDate occurrenceDate) {
        LocalDateTime start = shift(startAt, occurrenceDate);
        LocalDateTime end = shift(endAt, occurrenceDate);
        return new VisitaAgendada(
                occurrenceDate.equals(scheduledDate) ? id : id + "#" + occurrenceDate,
                customerId,
                orderId,
                contractId,
                certificateId,
                type,
                recurrence,
                occurrenceDate,
                title,
                serviceType,
                internalResponsible,
                start,
                end,
                allDay,
                status,
                priority,
                responsibleId,
                reminderActive,
                reminderDaysBefore,
                notes
        );
    }

    private LocalDateTime shift(LocalDateTime value, LocalDate occurrenceDate) {
        if (value == null) {
            return occurrenceDate.atStartOfDay();
        }
        return LocalDateTime.of(occurrenceDate, value.toLocalTime());
    }

    private LocalDate nextOccurrence(LocalDate current) {
        return switch (recurrence) {
            case MONTHLY -> current.plusMonths(1);
            case BIWEEKLY -> current.plusDays(15);
            case NONE -> current.plusYears(200);
        };
    }

    private LocalDateTime effectiveStart() {
        return startAt == null ? scheduledDate.atStartOfDay() : startAt;
    }

    private LocalDateTime effectiveEnd() {
        if (endAt != null) {
            return endAt;
        }
        return effectiveStart().plusHours(1);
    }

    public enum VisitType {
        MONTHLY,
        BIWEEKLY,
        ONE_OFF
    }

    public enum Recurrence {
        NONE,
        MONTHLY,
        BIWEEKLY
    }

    public enum VisitStatus {
        SCHEDULED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        MISSED
    }

    public enum VisitPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
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
            return "";
        }
        return value.trim();
    }

    private static Recurrence recurrenceFromType(VisitType type) {
        return switch (Objects.requireNonNullElse(type, VisitType.ONE_OFF)) {
            case MONTHLY -> Recurrence.MONTHLY;
            case BIWEEKLY -> Recurrence.BIWEEKLY;
            case ONE_OFF -> Recurrence.NONE;
        };
    }
}


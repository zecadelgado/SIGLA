package br.com.sigla.aplicacao.agenda.porta.entrada;

import br.com.sigla.dominio.agenda.VisitaAgendada;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CasoDeUsoAgenda {

    void schedule(ScheduleVisitCommand command);

    void update(ScheduleVisitCommand command);

    void reschedule(RescheduleVisitCommand command);

    void cancel(ChangeVisitStatusCommand command);

    void complete(ChangeVisitStatusCommand command);

    List<VisitaAgendada> listAll();

    List<VisitaAgendada> listBetween(LocalDate start, LocalDate end);

    List<VisitaAgendada> upcomingVisits(LocalDate referenceDate, int days);

    List<VisitaAgendada> overdueVisits(LocalDate referenceDate);

    record ScheduleVisitCommand(
            String id,
            String customerId,
            String orderId,
            String contractId,
            String certificateId,
            VisitaAgendada.VisitType type,
            VisitaAgendada.Recurrence recurrence,
            LocalDate scheduledDate,
            String title,
            String serviceType,
            String internalResponsible,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean allDay,
            VisitaAgendada.VisitStatus status,
            VisitaAgendada.VisitPriority priority,
            String responsibleId,
            boolean reminderActive,
            int reminderDaysBefore,
            String notes
    ) {
        public ScheduleVisitCommand(
                String id,
                String customerId,
                String contractId,
                String certificateId,
                VisitaAgendada.VisitType type,
                LocalDate scheduledDate,
                String title,
                String serviceType,
                String internalResponsible,
                LocalDateTime startAt,
                LocalDateTime endAt,
                boolean allDay,
                VisitaAgendada.VisitStatus status,
                VisitaAgendada.VisitPriority priority,
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

        public ScheduleVisitCommand(
                String id,
                String customerId,
                String contractId,
                VisitaAgendada.VisitType type,
                LocalDate scheduledDate,
                String title,
                String serviceType,
                String internalResponsible,
                LocalDateTime startAt,
                LocalDateTime endAt,
                boolean allDay,
                VisitaAgendada.VisitStatus status,
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
                    VisitaAgendada.VisitPriority.NORMAL,
                    "",
                    false,
                    0,
                    notes
            );
        }

        public ScheduleVisitCommand(
                String id,
                String customerId,
                String contractId,
                VisitaAgendada.VisitType type,
                LocalDate scheduledDate,
                String title,
                String serviceType,
                String internalResponsible,
                LocalDateTime startAt,
                LocalDateTime endAt,
                boolean allDay,
                VisitaAgendada.VisitStatus status,
                VisitaAgendada.VisitPriority priority,
                String responsibleId,
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
                    priority,
                    responsibleId,
                    false,
                    0,
                    notes
            );
        }

        public ScheduleVisitCommand(
                String id,
                String customerId,
                String contractId,
                VisitaAgendada.VisitType type,
                LocalDate scheduledDate,
                VisitaAgendada.VisitStatus status,
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
                    VisitaAgendada.VisitPriority.NORMAL,
                    "",
                    false,
                    0,
                    notes
            );
        }

        private static VisitaAgendada.Recurrence recurrenceFromType(VisitaAgendada.VisitType type) {
            return switch (type == null ? VisitaAgendada.VisitType.ONE_OFF : type) {
                case MONTHLY -> VisitaAgendada.Recurrence.MONTHLY;
                case BIWEEKLY -> VisitaAgendada.Recurrence.BIWEEKLY;
                case ONE_OFF -> VisitaAgendada.Recurrence.NONE;
            };
        }
    }

    record RescheduleVisitCommand(
            String id,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
    }

    record ChangeVisitStatusCommand(
            String id,
            String reason
    ) {
    }
}


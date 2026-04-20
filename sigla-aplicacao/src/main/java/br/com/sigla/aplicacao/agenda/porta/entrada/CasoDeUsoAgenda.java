package br.com.sigla.aplicacao.agenda.porta.entrada;

import br.com.sigla.dominio.agenda.VisitaAgendada;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CasoDeUsoAgenda {

    void schedule(ScheduleVisitCommand command);

    List<VisitaAgendada> listAll();

    List<VisitaAgendada> upcomingVisits(LocalDate referenceDate, int days);

    List<VisitaAgendada> overdueVisits(LocalDate referenceDate);

    record ScheduleVisitCommand(
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
    }
}


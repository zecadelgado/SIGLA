package br.com.sigla.aplicacao.agenda.porta.entrada;

import br.com.sigla.dominio.agenda.VisitaAgendada;

import java.time.LocalDate;
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
            VisitaAgendada.VisitStatus status,
            String notes
    ) {
    }
}


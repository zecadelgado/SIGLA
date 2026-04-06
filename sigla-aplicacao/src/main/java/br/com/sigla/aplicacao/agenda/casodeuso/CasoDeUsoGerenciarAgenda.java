package br.com.sigla.aplicacao.agenda.casodeuso;

import br.com.sigla.aplicacao.agenda.porta.entrada.CasoDeUsoAgenda;
import br.com.sigla.aplicacao.agenda.porta.saida.RepositorioAgenda;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CasoDeUsoGerenciarAgenda implements CasoDeUsoAgenda {

    private final RepositorioAgenda repository;

    public CasoDeUsoGerenciarAgenda(RepositorioAgenda repository) {
        this.repository = repository;
    }

    @Override
    public void schedule(ScheduleVisitCommand command) {
        repository.save(new VisitaAgendada(
                command.id(),
                command.customerId(),
                command.contractId(),
                command.type(),
                command.scheduledDate(),
                command.status(),
                command.notes()
        ));
    }

    @Override
    public List<VisitaAgendada> listAll() {
        return repository.findAll();
    }

    @Override
    public List<VisitaAgendada> upcomingVisits(LocalDate referenceDate, int days) {
        return repository.findAll().stream()
                .filter(schedule -> schedule.isUpcomingWithin(referenceDate, days))
                .toList();
    }

    @Override
    public List<VisitaAgendada> overdueVisits(LocalDate referenceDate) {
        return repository.findAll().stream()
                .filter(schedule -> schedule.isOverdue(referenceDate))
                .toList();
    }
}


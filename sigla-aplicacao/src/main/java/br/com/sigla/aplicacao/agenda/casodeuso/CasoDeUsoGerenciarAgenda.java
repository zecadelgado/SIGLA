package br.com.sigla.aplicacao.agenda.casodeuso;

import br.com.sigla.aplicacao.agenda.porta.entrada.CasoDeUsoAgenda;
import br.com.sigla.aplicacao.agenda.porta.saida.RepositorioAgenda;
import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CasoDeUsoGerenciarAgenda implements CasoDeUsoAgenda {

    private final RepositorioAgenda repository;
    private final CasoDeUsoOrdemServico ordemServicoUseCase;

    @Autowired
    public CasoDeUsoGerenciarAgenda(RepositorioAgenda repository, CasoDeUsoOrdemServico ordemServicoUseCase) {
        this.repository = repository;
        this.ordemServicoUseCase = ordemServicoUseCase;
    }

    public CasoDeUsoGerenciarAgenda(RepositorioAgenda repository) {
        this.repository = repository;
        this.ordemServicoUseCase = null;
    }

    @Override
    public void schedule(ScheduleVisitCommand command) {
        VisitaAgendada schedule = toSchedule(command);
        validate(schedule);
        repository.save(schedule);
    }

    @Override
    public void update(ScheduleVisitCommand command) {
        VisitaAgendada current = repository.findById(command.id())
                .orElseThrow(() -> new IllegalArgumentException("Evento de agenda nao encontrado."));
        VisitaAgendada schedule = toSchedule(command);
        validate(schedule);
        if (isOrdemServico(current)) {
            ordemServicoUseCase.reschedule(new CasoDeUsoOrdemServico.ReagendarOrdemServicoCommand(
                    current.orderId(), schedule.startAt(), schedule.endAt()));
            return;
        }
        repository.save(schedule);
    }

    @Override
    public void reschedule(RescheduleVisitCommand command) {
        VisitaAgendada current = repository.findById(command.id())
                .orElseThrow(() -> new IllegalArgumentException("Evento de agenda nao encontrado."));
        if (command.startAt() == null) {
            throw new IllegalArgumentException("Data inicial obrigatoria.");
        }
        LocalDateTime endAt = command.endAt() == null ? command.startAt().plusHours(1) : command.endAt();
        VisitaAgendada updated = copy(current, command.startAt().toLocalDate(), command.startAt(), endAt, current.status(), current.notes());
        validate(updated);
        if (isOrdemServico(current)) {
            ordemServicoUseCase.reschedule(new CasoDeUsoOrdemServico.ReagendarOrdemServicoCommand(
                    current.orderId(), command.startAt(), endAt));
            return;
        }
        repository.save(updated);
    }

    @Override
    public void start(ChangeVisitStatusCommand command) {
        VisitaAgendada current = find(command.id());
        if (isOrdemServico(current)) {
            ordemServicoUseCase.start(current.orderId());
            return;
        }
        repository.save(copy(current, current.scheduledDate(), current.startAt(), current.endAt(),
                VisitaAgendada.VisitStatus.IN_PROGRESS, current.notes()));
    }

    @Override
    public void cancel(ChangeVisitStatusCommand command) {
        VisitaAgendada current = find(command.id());
        if (current.status() == VisitaAgendada.VisitStatus.CANCELLED) {
            throw new IllegalArgumentException("Evento ja cancelado.");
        }
        if (isOrdemServico(current)) {
            ordemServicoUseCase.cancel(new CasoDeUsoOrdemServico.CancelarOrdemServicoCommand(current.orderId(), command.reason()));
            return;
        }
        repository.save(copy(current, current.scheduledDate(), current.startAt(), current.endAt(),
                VisitaAgendada.VisitStatus.CANCELLED, append(current.notes(), "[CANCELAMENTO] " + command.reason())));
    }

    @Override
    public void complete(ChangeVisitStatusCommand command) {
        VisitaAgendada current = find(command.id());
        if (current.status() == VisitaAgendada.VisitStatus.CANCELLED) {
            throw new IllegalArgumentException("Evento cancelado nao pode ser concluido.");
        }
        if (current.status() == VisitaAgendada.VisitStatus.COMPLETED) {
            throw new IllegalArgumentException("Evento ja concluido.");
        }
        if (isOrdemServico(current)) {
            ordemServicoUseCase.conclude(new CasoDeUsoOrdemServico.ConcluirOrdemServicoCommand(
                    current.orderId(), current.responsibleId(), null, false));
            return;
        }
        repository.save(copy(current, current.scheduledDate(), current.startAt(), current.endAt(),
                VisitaAgendada.VisitStatus.COMPLETED,
                command.reason() == null || command.reason().isBlank() ? current.notes() : append(current.notes(), "[CONCLUSAO] " + command.reason())));
    }

    @Override
    public List<VisitaAgendada> listAll() {
        return repository.findAll();
    }

    @Override
    public List<VisitaAgendada> listBetween(LocalDate start, LocalDate end) {
        return repository.findAll().stream()
                .flatMap(schedule -> schedule.occurrencesBetween(start, end).stream())
                .filter(schedule -> !schedule.scheduledDate().isBefore(start) && !schedule.scheduledDate().isAfter(end))
                .sorted(java.util.Comparator.comparing(VisitaAgendada::scheduledDate).thenComparing(VisitaAgendada::title))
                .toList();
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

    private VisitaAgendada toSchedule(ScheduleVisitCommand command) {
        return new VisitaAgendada(
                command.id(),
                command.customerId(),
                command.orderId(),
                command.contractId(),
                command.certificateId(),
                command.type(),
                command.recurrence(),
                command.scheduledDate(),
                command.title(),
                command.serviceType(),
                command.internalResponsible(),
                command.startAt(),
                command.endAt(),
                command.allDay(),
                command.status(),
                command.priority(),
                command.responsibleId(),
                command.reminderActive(),
                command.reminderDaysBefore(),
                command.notes()
        ).comDiasLembrete(command.diasLembrete());
    }

    private void validate(VisitaAgendada schedule) {
        if (schedule.title() == null || schedule.title().isBlank()) {
            throw new IllegalArgumentException("Titulo obrigatorio.");
        }
        if (schedule.reminderActive() && schedule.reminderDaysBefore() < 0) {
            throw new IllegalArgumentException("Dias de lembrete nao pode ser negativo.");
        }
        // Conflito so existe entre eventos do mesmo responsavel; sem responsavel nao ha o que validar.
        if (schedule.responsibleId() == null || schedule.responsibleId().isBlank()) {
            return;
        }
        for (VisitaAgendada existing : repository.findByResponsavel(schedule.responsibleId())) {
            if (schedule.conflictsWith(existing)) {
                throw new IllegalArgumentException("Conflito de agenda para o mesmo responsavel.");
            }
        }
    }

    private VisitaAgendada find(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Evento de agenda nao encontrado."));
    }

    private boolean isOrdemServico(VisitaAgendada schedule) {
        return ordemServicoUseCase != null && schedule.orderId() != null && !schedule.orderId().isBlank();
    }

    private VisitaAgendada copy(
            VisitaAgendada current,
            LocalDate scheduledDate,
            LocalDateTime startAt,
            LocalDateTime endAt,
            VisitaAgendada.VisitStatus status,
            String notes
    ) {
        return new VisitaAgendada(
                current.id(),
                current.customerId(),
                current.orderId(),
                current.contractId(),
                current.certificateId(),
                current.type(),
                current.recurrence(),
                scheduledDate,
                current.title(),
                current.serviceType(),
                current.internalResponsible(),
                startAt,
                endAt,
                current.allDay(),
                status,
                current.priority(),
                current.responsibleId(),
                current.reminderActive(),
                current.reminderDaysBefore(),
                notes
        ).comDiasLembrete(current.diasLembrete());
    }

    private String append(String current, String addition) {
        if (addition == null || addition.isBlank()) {
            return current;
        }
        if (current == null || current.isBlank()) {
            return addition.trim();
        }
        return current + System.lineSeparator() + addition.trim();
    }
}


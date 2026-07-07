package br.com.sigla.aplicacao.contratos.casodeuso;

import br.com.sigla.aplicacao.agenda.porta.saida.RepositorioAgenda;
import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.aplicacao.contratos.porta.saida.RepositorioContrato;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.dominio.contratos.Contrato;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CasoDeUsoGerenciarContrato implements CasoDeUsoContrato {

    private final RepositorioContrato repository;
    private final RepositorioAgenda agendaRepository;

    @Autowired
    public CasoDeUsoGerenciarContrato(RepositorioContrato repository, RepositorioAgenda agendaRepository) {
        this.repository = repository;
        this.agendaRepository = agendaRepository;
    }

    public CasoDeUsoGerenciarContrato(RepositorioContrato repository) {
        this.repository = repository;
        this.agendaRepository = null;
    }

    @Override
    public void create(CreateContratoCommand command) {
        Contrato contrato = new Contrato(
                command.id(),
                command.customerId(),
                command.description(),
                command.startDate(),
                command.endDate(),
                command.type(),
                command.serviceFrequency(),
                command.status(),
                command.renewalRule(),
                command.monthlyValue(),
                command.alertActive(),
                command.alertDaysBeforeEnd(),
                command.notes()
        ).comDiasLembrete(command.diasLembrete());
        repository.save(contrato);
        sincronizarCalendario(contrato);
    }

    @Override
    public void update(UpdateContratoCommand command) {
        Contrato atual = find(command.id());
        Contrato contrato = new Contrato(
                command.id(),
                command.customerId(),
                command.description(),
                command.startDate(),
                command.endDate(),
                command.type(),
                command.serviceFrequency(),
                atual.status(),
                command.renewalRule(),
                command.monthlyValue(),
                command.alertActive(),
                command.alertDaysBeforeEnd(),
                command.notes()
        ).comDiasLembrete(command.diasLembrete());
        repository.save(contrato);
        sincronizarCalendario(contrato);
    }

    @Override
    public void encerrar(EncerrarContratoCommand command) {
        if (command.motivo() == null || command.motivo().isBlank()) {
            throw new IllegalArgumentException("Motivo do encerramento e obrigatorio.");
        }
        Contrato contrato = find(command.id());
        if (contrato.status() == Contrato.ContratoStatus.CANCELLED) {
            throw new IllegalArgumentException("Contrato ja encerrado.");
        }
        Contrato encerrado = contrato
                .comObservacoes(append(contrato.notes(), "[ENCERRAMENTO] " + command.motivo()))
                .comStatus(Contrato.ContratoStatus.CANCELLED);
        repository.save(encerrado);
        sincronizarCalendario(encerrado);
    }

    @Override
    public void renovar(RenovarContratoCommand command) {
        Contrato contrato = find(command.id());
        if (contrato.status() == Contrato.ContratoStatus.CANCELLED) {
            throw new IllegalArgumentException("Contrato encerrado nao pode ser renovado.");
        }
        LocalDate novaDataFim = command.novaDataFim() == null
                ? contrato.endDate().plusMonths(contrato.periodoMeses())
                : command.novaDataFim();
        if (!novaDataFim.isAfter(contrato.endDate())) {
            throw new IllegalArgumentException("Nova data fim deve ser posterior ao vencimento atual.");
        }
        Contrato renovado = contrato.renovado(novaDataFim)
                .comObservacoes(append(contrato.notes(), "[RENOVACAO] ate " + novaDataFim));
        repository.save(renovado);
        sincronizarCalendario(renovado);
    }

    @Override
    public List<Contrato> marcarVencidos(LocalDate referenceDate) {
        LocalDate hoje = referenceDate == null ? LocalDate.now() : referenceDate;
        List<Contrato> afetados = new java.util.ArrayList<>();
        for (Contrato contrato : repository.findAll()) {
            if (contrato.status() == Contrato.ContratoStatus.ACTIVE && contrato.endDate().isBefore(hoje)) {
                Contrato vencido = contrato.comStatus(Contrato.ContratoStatus.EXPIRED);
                repository.save(vencido);
                afetados.add(vencido);
            }
        }
        return afetados;
    }

    @Override
    public List<Contrato> listAll() {
        return repository.findAll();
    }

    @Override
    public List<Contrato> expiringContratos(LocalDate referenceDate) {
        return repository.findAll().stream()
                .filter(contract -> contract.isExpiringWithin(referenceDate))
                .toList();
    }

    private Contrato find(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contrato nao encontrado."));
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

    private void sincronizarCalendario(Contrato contrato) {
        if (agendaRepository == null || contrato.endDate() == null) {
            return;
        }
        agendaRepository.save(new VisitaAgendada(
                "contrato-vencimento-" + contrato.id(),
                contrato.customerId(),
                "",
                contrato.id(),
                "",
                VisitaAgendada.VisitType.ONE_OFF,
                VisitaAgendada.Recurrence.NONE,
                contrato.endDate(),
                "Vencimento de contrato",
                "contrato_vencimento",
                "",
                contrato.endDate().atStartOfDay(),
                contrato.endDate().atStartOfDay(),
                true,
                contrato.status() == Contrato.ContratoStatus.CANCELLED
                        ? VisitaAgendada.VisitStatus.CANCELLED
                        : VisitaAgendada.VisitStatus.SCHEDULED,
                VisitaAgendada.VisitPriority.HIGH,
                "",
                contrato.alertActive(),
                contrato.alertDaysBeforeEnd(),
                contrato.description()
        ));
    }
}


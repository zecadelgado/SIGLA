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
        );
        repository.save(contrato);
        sincronizarCalendario(contrato);
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


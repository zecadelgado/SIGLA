package br.com.sigla.aplicacao.certificados.casodeuso;

import br.com.sigla.aplicacao.agenda.porta.saida.RepositorioAgenda;
import br.com.sigla.aplicacao.certificados.porta.entrada.CasoDeUsoCertificado;
import br.com.sigla.aplicacao.certificados.porta.saida.RepositorioCertificado;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.dominio.certificados.Certificado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CasoDeUsoGerenciarCertificado implements CasoDeUsoCertificado {

    private final RepositorioCertificado repository;
    private final RepositorioAgenda agendaRepository;

    @Autowired
    public CasoDeUsoGerenciarCertificado(RepositorioCertificado repository, RepositorioAgenda agendaRepository) {
        this.repository = repository;
        this.agendaRepository = agendaRepository;
    }

    public CasoDeUsoGerenciarCertificado(RepositorioCertificado repository) {
        this.repository = repository;
        this.agendaRepository = null;
    }

    @Override
    public void issue(IssueCertificadoCommand command) {
        int intervalMonths = command.intervalMonths() <= 0 ? 6 : command.intervalMonths();
        LocalDate validUntil = command.validUntil() == null ? command.issuedOn().plusMonths(intervalMonths) : command.validUntil();
        int alertDays = command.renewalAlertDays() <= 0 ? 15 : command.renewalAlertDays();
        Certificado certificado = new Certificado(
                command.id(),
                command.customerId(),
                command.serviceProvidedId(),
                command.orderId(),
                command.description(),
                command.issuedOn(),
                validUntil,
                intervalMonths,
                command.alertActive(),
                command.status(),
                alertDays,
                command.notes()
        );
        repository.save(certificado);
        sincronizarCalendario(certificado);
    }

    @Override
    public List<Certificado> listAll() {
        return repository.findAll();
    }

    @Override
    public List<Certificado> expiringCertificados(LocalDate referenceDate) {
        return repository.findAll().stream()
                .filter(certificate -> certificate.isExpiringWithin(referenceDate))
                .toList();
    }

    private void sincronizarCalendario(Certificado certificado) {
        if (agendaRepository == null || certificado.validUntil() == null) {
            return;
        }
        agendaRepository.save(new VisitaAgendada(
                "certificado-vencimento-" + certificado.id(),
                certificado.customerId(),
                certificado.orderId(),
                "",
                certificado.id(),
                VisitaAgendada.VisitType.ONE_OFF,
                VisitaAgendada.Recurrence.NONE,
                certificado.validUntil(),
                "Vencimento de certificado",
                "certificado_vencimento",
                "",
                certificado.validUntil().atStartOfDay(),
                certificado.validUntil().atStartOfDay(),
                true,
                certificado.status() == Certificado.CertificadoStatus.REPLACED
                        ? VisitaAgendada.VisitStatus.CANCELLED
                        : VisitaAgendada.VisitStatus.SCHEDULED,
                VisitaAgendada.VisitPriority.HIGH,
                "",
                certificado.alertActive(),
                certificado.renewalAlertDays(),
                certificado.description()
        ));
    }
}


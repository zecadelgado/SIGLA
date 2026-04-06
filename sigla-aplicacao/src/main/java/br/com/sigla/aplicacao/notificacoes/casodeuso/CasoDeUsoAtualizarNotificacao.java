package br.com.sigla.aplicacao.notificacoes.casodeuso;

import br.com.sigla.aplicacao.certificados.porta.saida.RepositorioCertificado;
import br.com.sigla.aplicacao.contratos.porta.saida.RepositorioContrato;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioPlanoParcelamento;
import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoNotificacao;
import br.com.sigla.aplicacao.notificacoes.porta.saida.RepositorioNotificacao;
import br.com.sigla.aplicacao.agenda.porta.saida.RepositorioAgenda;
import br.com.sigla.dominio.certificados.Certificado;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.dominio.financeiro.PlanoParcelamento;
import br.com.sigla.dominio.notificacoes.Notificacao;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CasoDeUsoAtualizarNotificacao implements CasoDeUsoNotificacao {

    private final RepositorioNotificacao notificationRepository;
    private final RepositorioContrato contractRepository;
    private final RepositorioCertificado certificateRepository;
    private final RepositorioPlanoParcelamento installmentPlanRepository;
    private final RepositorioAgenda agendaRepository;

    public CasoDeUsoAtualizarNotificacao(
            RepositorioNotificacao notificationRepository,
            RepositorioContrato contractRepository,
            RepositorioCertificado certificateRepository,
            RepositorioPlanoParcelamento installmentPlanRepository,
            RepositorioAgenda agendaRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.contractRepository = contractRepository;
        this.certificateRepository = certificateRepository;
        this.installmentPlanRepository = installmentPlanRepository;
        this.agendaRepository = agendaRepository;
    }

    @Override
    public void refresh(LocalDate referenceDate) {
        List<Notificacao> notificacoes = new ArrayList<>();
        notificacoes.addAll(buildContratoAlerts(referenceDate));
        notificacoes.addAll(buildCertificadoAlerts(referenceDate));
        notificacoes.addAll(buildInstallmentAlerts(referenceDate));
        notificacoes.addAll(buildVisitAlerts(referenceDate));
        notificacoes.sort(Comparator.comparing(Notificacao::triggerDate).thenComparing(Notificacao::title));
        notificationRepository.replaceAll(notificacoes);
    }

    @Override
    public List<Notificacao> listAll() {
        return notificationRepository.findAll();
    }

    private List<Notificacao> buildContratoAlerts(LocalDate referenceDate) {
        return contractRepository.findAll().stream()
                .filter(contract -> contract.isExpiringWithin(referenceDate))
                .map(contract -> new Notificacao(
                        "contract-" + contract.id(),
                        Notificacao.NotificacaoType.CONTRACT_EXPIRING,
                        "Contrato proximo do vencimento",
                        "Contrato " + contract.id() + " do cliente " + contract.customerId() + " vence em " + contract.endDate() + ".",
                        contract.id(),
                        contract.endDate(),
                        Notificacao.NotificacaoStatus.OPEN
                ))
                .toList();
    }

    private List<Notificacao> buildCertificadoAlerts(LocalDate referenceDate) {
        return certificateRepository.findAll().stream()
                .filter(certificate -> certificate.isExpiringWithin(referenceDate))
                .map(certificate -> new Notificacao(
                        "certificate-" + certificate.id(),
                        Notificacao.NotificacaoType.CERTIFICATE_EXPIRING,
                        "Certificado proximo do vencimento",
                        "Certificado " + certificate.id() + " do servico " + certificate.serviceProvidedId() + " vence em " + certificate.validUntil() + ".",
                        certificate.id(),
                        certificate.validUntil(),
                        Notificacao.NotificacaoStatus.OPEN
                ))
                .toList();
    }

    private List<Notificacao> buildInstallmentAlerts(LocalDate referenceDate) {
        return installmentPlanRepository.findAll().stream()
                .filter(plan -> plan.isOverdue(referenceDate))
                .map(plan -> new Notificacao(
                        "installment-" + plan.id(),
                        Notificacao.NotificacaoType.INSTALLMENT_OVERDUE,
                        "Parcela em atraso",
                        "Plano " + plan.id() + " do cliente " + plan.customerId() + " esta com vencimento em " + plan.nextDueDate() + ".",
                        plan.id(),
                        plan.nextDueDate(),
                        Notificacao.NotificacaoStatus.OPEN
                ))
                .toList();
    }

    private List<Notificacao> buildVisitAlerts(LocalDate referenceDate) {
        List<Notificacao> notificacoes = new ArrayList<>();
        for (VisitaAgendada schedule : agendaRepository.findAll()) {
            if (schedule.isUpcomingWithin(referenceDate, 7)) {
                notificacoes.add(new Notificacao(
                        "visit-upcoming-" + schedule.id(),
                        Notificacao.NotificacaoType.VISIT_UPCOMING,
                        "Visita proxima",
                        "Visita " + schedule.id() + " para o cliente " + schedule.customerId() + " esta agendada para " + schedule.scheduledDate() + ".",
                        schedule.id(),
                        schedule.scheduledDate(),
                        Notificacao.NotificacaoStatus.OPEN
                ));
            }
            if (schedule.isOverdue(referenceDate)) {
                notificacoes.add(new Notificacao(
                        "visit-missed-" + schedule.id(),
                        Notificacao.NotificacaoType.VISIT_MISSED,
                        "Visita nao realizada",
                        "Visita " + schedule.id() + " para o cliente " + schedule.customerId() + " estava prevista para " + schedule.scheduledDate() + ".",
                        schedule.id(),
                        schedule.scheduledDate(),
                        Notificacao.NotificacaoStatus.OPEN
                ));
            }
        }
        return notificacoes;
    }
}


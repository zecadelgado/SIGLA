package br.com.sigla.aplicacao.notificacoes.casodeuso;

import br.com.sigla.aplicacao.clientes.porta.saida.RepositorioCliente;
import br.com.sigla.aplicacao.certificados.porta.saida.RepositorioCertificado;
import br.com.sigla.aplicacao.contratos.porta.saida.RepositorioContrato;
import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoNotificacao;
import br.com.sigla.aplicacao.notificacoes.porta.saida.PortaWebhookNotificacao;
import br.com.sigla.aplicacao.notificacoes.porta.saida.RepositorioNotificacao;
import br.com.sigla.dominio.certificados.Certificado;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.dominio.notificacoes.Notificacao;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CasoDeUsoAtualizarNotificacao implements CasoDeUsoNotificacao {

    private final RepositorioNotificacao notificationRepository;
    private final RepositorioContrato contractRepository;
    private final RepositorioCertificado certificateRepository;
    private final RepositorioCliente customerRepository;
    private final PortaWebhookNotificacao notificationWebhook;

    public CasoDeUsoAtualizarNotificacao(
            RepositorioNotificacao notificationRepository,
            RepositorioContrato contractRepository,
            RepositorioCertificado certificateRepository,
            RepositorioCliente customerRepository,
            PortaWebhookNotificacao notificationWebhook
    ) {
        this.notificationRepository = notificationRepository;
        this.contractRepository = contractRepository;
        this.certificateRepository = certificateRepository;
        this.customerRepository = customerRepository;
        this.notificationWebhook = notificationWebhook;
    }

    @Override
    public void refresh(LocalDate referenceDate) {
        Map<String, Cliente> customers = customerRepository.findAll().stream()
                .collect(Collectors.toMap(Cliente::id, Function.identity(), (left, right) -> left));
        processarContratos(referenceDate, customers);
        processarCertificados(referenceDate, customers);
    }

    @Override
    public List<Notificacao> listAll() {
        return notificationRepository.findAll().stream()
                .sorted(Comparator.comparing(Notificacao::triggerDate).thenComparing(Notificacao::title))
                .toList();
    }

    private void processarContratos(LocalDate referenceDate, Map<String, Cliente> customers) {
        contractRepository.findAll().stream()
                .filter(contract -> contract.shouldNotify(referenceDate))
                .forEach(contract -> {
                    Cliente customer = customers.get(contract.customerId());
                    String customerName = customer == null ? contract.customerId() : customer.name();
                    String message = "O contrato do cliente " + customerName + " vence em " + contract.endDate() + ".";
                    Notificacao notification = new Notificacao(
                            "contrato-vencimento-" + contract.id(),
                            Notificacao.NotificacaoType.CONTRACT_EXPIRING,
                            "Contrato proximo do vencimento",
                            message,
                            contract.id(),
                            contract.endDate(),
                            Notificacao.NotificacaoStatus.PENDING
                    );
                    PortaWebhookNotificacao.PayloadWebhook payload = new PortaWebhookNotificacao.PayloadWebhook(
                            "contrato_vencimento",
                            contract.customerId(),
                            customerName,
                            contract.id(),
                            contract.description().isBlank() ? contract.type().name() : contract.description(),
                            contract.startDate(),
                            contract.endDate(),
                            contract.alertDaysBeforeEnd(),
                            message
                    );
                    enviarSemDuplicar(notification, payload);
                });
    }

    private void processarCertificados(LocalDate referenceDate, Map<String, Cliente> customers) {
        certificateRepository.findAll().stream()
                .filter(certificate -> certificate.shouldNotify(referenceDate))
                .forEach(certificate -> {
                    Cliente customer = customers.get(certificate.customerId());
                    String customerName = customer == null ? certificate.customerId() : customer.name();
                    String message = "O certificado do cliente " + customerName + " vence em " + certificate.validUntil() + ".";
                    Notificacao notification = new Notificacao(
                            "certificado-vencimento-" + certificate.id(),
                            Notificacao.NotificacaoType.CERTIFICATE_EXPIRING,
                            "Certificado proximo do vencimento",
                            message,
                            certificate.id(),
                            certificate.validUntil(),
                            Notificacao.NotificacaoStatus.PENDING
                    );
                    PortaWebhookNotificacao.PayloadWebhook payload = new PortaWebhookNotificacao.PayloadWebhook(
                            "certificado_vencimento",
                            certificate.customerId(),
                            customerName,
                            certificate.id(),
                            certificate.description(),
                            certificate.issuedOn(),
                            certificate.validUntil(),
                            certificate.renewalAlertDays(),
                            message
                    );
                    enviarSemDuplicar(notification, payload);
                });
    }

    private void enviarSemDuplicar(Notificacao pendingNotification, PortaWebhookNotificacao.PayloadWebhook payload) {
        Set<Notificacao.NotificacaoStatus> alreadyHandled = Set.of(
                Notificacao.NotificacaoStatus.PENDING,
                Notificacao.NotificacaoStatus.SENT,
                Notificacao.NotificacaoStatus.OPEN
        );
        boolean alreadyExists = notificationRepository.existsByTypeAndRelatedEntityIdAndStatusIn(
                pendingNotification.type(),
                pendingNotification.relatedEntityId(),
                alreadyHandled
        );
        if (alreadyExists) {
            return;
        }
        notificationRepository.save(pendingNotification);
        PortaWebhookNotificacao.ResultadoEnvio resultado = notificationWebhook.enviar(payload);
        Notificacao.NotificacaoStatus status = resultado.sucesso()
                ? Notificacao.NotificacaoStatus.SENT
                : Notificacao.NotificacaoStatus.FAILED;
        String detail = resultado.detalhe().isBlank() ? "" : " Detalhe: " + resultado.detalhe();
        notificationRepository.save(new Notificacao(
                pendingNotification.id(),
                pendingNotification.type(),
                pendingNotification.title(),
                pendingNotification.message() + detail,
                pendingNotification.relatedEntityId(),
                pendingNotification.triggerDate(),
                status
        ));
    }
}


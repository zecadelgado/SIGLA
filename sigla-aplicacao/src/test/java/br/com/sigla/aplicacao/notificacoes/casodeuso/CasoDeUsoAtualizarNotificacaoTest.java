package br.com.sigla.aplicacao.notificacoes.casodeuso;

import br.com.sigla.aplicacao.certificados.porta.saida.RepositorioCertificado;
import br.com.sigla.aplicacao.clientes.porta.saida.RepositorioCliente;
import br.com.sigla.aplicacao.contratos.porta.saida.RepositorioContrato;
import br.com.sigla.aplicacao.notificacoes.porta.saida.PortaWebhookNotificacao;
import br.com.sigla.aplicacao.notificacoes.porta.saida.RepositorioNotificacao;
import br.com.sigla.dominio.certificados.Certificado;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.dominio.notificacoes.Notificacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CasoDeUsoAtualizarNotificacaoTest {

    @Test
    void enviaVencimentosParaWebhookSemDuplicarNotificacaoEnviada() {
        FakeRepositorioNotificacao notificacoes = new FakeRepositorioNotificacao();
        FakeWebhook webhook = new FakeWebhook(true);
        CasoDeUsoAtualizarNotificacao casoDeUso = new CasoDeUsoAtualizarNotificacao(
                notificacoes,
                new FakeRepositorioContrato(List.of(contrato())),
                new FakeRepositorioCertificado(List.of(certificado())),
                new FakeRepositorioCliente(List.of(cliente())),
                webhook
        );

        casoDeUso.refresh(LocalDate.of(2026, 6, 16));
        casoDeUso.refresh(LocalDate.of(2026, 6, 16));

        assertEquals(2, webhook.payloads.size());
        assertEquals(2, notificacoes.findAll().stream()
                .filter(notificacao -> notificacao.status() == Notificacao.NotificacaoStatus.SENT)
                .count());
    }

    @Test
    void registraFalhaDoWebhookSemQuebrarFluxo() {
        FakeRepositorioNotificacao notificacoes = new FakeRepositorioNotificacao();
        CasoDeUsoAtualizarNotificacao casoDeUso = new CasoDeUsoAtualizarNotificacao(
                notificacoes,
                new FakeRepositorioContrato(List.of(contrato())),
                new FakeRepositorioCertificado(List.of()),
                new FakeRepositorioCliente(List.of(cliente())),
                new FakeWebhook(false)
        );

        casoDeUso.refresh(LocalDate.of(2026, 6, 16));

        assertEquals(Notificacao.NotificacaoStatus.FAILED, notificacoes.findAll().getFirst().status());
    }

    private Contrato contrato() {
        return new Contrato(
                "contrato-1",
                "cliente-1",
                "Mensal",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30),
                Contrato.ContratoType.MONTHLY,
                Contrato.ServiceFrequency.MONTHLY,
                Contrato.ContratoStatus.ACTIVE,
                Contrato.RenewalRule.MANUAL,
                BigDecimal.TEN,
                true,
                15,
                ""
        );
    }

    private Certificado certificado() {
        return new Certificado(
                "cert-1",
                "cliente-1",
                "",
                "",
                "Higiene",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 7, 1),
                6,
                true,
                Certificado.CertificadoStatus.ACTIVE,
                15,
                ""
        );
    }

    private Cliente cliente() {
        return new Cliente(
                "cliente-1",
                Cliente.TipoCliente.PESSOA_FISICA,
                "Cliente Teste",
                "",
                "",
                "52998224725",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                List.of(),
                "",
                true
        );
    }

    private static final class FakeWebhook implements PortaWebhookNotificacao {
        private final boolean success;
        private final List<PayloadWebhook> payloads = new java.util.ArrayList<>();

        private FakeWebhook(boolean success) {
            this.success = success;
        }

        @Override
        public ResultadoEnvio enviar(PayloadWebhook payload) {
            payloads.add(payload);
            return success ? ResultadoEnvio.enviado() : ResultadoEnvio.comFalha("n8n indisponivel");
        }
    }

    private static final class FakeRepositorioNotificacao implements RepositorioNotificacao {
        private final Map<String, Notificacao> storage = new ConcurrentHashMap<>();

        @Override
        public void replaceAll(List<Notificacao> notificacoes) {
            notificacoes.forEach(this::save);
        }

        @Override
        public void save(Notificacao notificacao) {
            storage.put(notificacao.id(), notificacao);
        }

        @Override
        public List<Notificacao> findAll() {
            return storage.values().stream().toList();
        }

        @Override
        public boolean existsByTypeAndRelatedEntityIdAndStatusIn(
                Notificacao.NotificacaoType type,
                String relatedEntityId,
                Set<Notificacao.NotificacaoStatus> statuses
        ) {
            return storage.values().stream()
                    .anyMatch(notification -> notification.type() == type
                            && notification.relatedEntityId().equals(relatedEntityId)
                            && statuses.contains(notification.status()));
        }
    }

    private record FakeRepositorioContrato(List<Contrato> contracts) implements RepositorioContrato {
        @Override
        public void save(Contrato contract) {
        }

        @Override
        public List<Contrato> findAll() {
            return contracts;
        }

        @Override
        public Optional<Contrato> findById(String id) {
            return contracts.stream().filter(contract -> contract.id().equals(id)).findFirst();
        }
    }

    private record FakeRepositorioCertificado(List<Certificado> certificates) implements RepositorioCertificado {
        @Override
        public void save(Certificado certificate) {
        }

        @Override
        public List<Certificado> findAll() {
            return certificates;
        }

        @Override
        public Optional<Certificado> findById(String id) {
            return certificates.stream().filter(certificate -> certificate.id().equals(id)).findFirst();
        }
    }

    private record FakeRepositorioCliente(List<Cliente> customers) implements RepositorioCliente {
        @Override
        public void save(Cliente customer) {
        }

        @Override
        public void deleteById(String id) {
        }

        @Override
        public List<Cliente> findAll() {
            return customers;
        }

        @Override
        public Optional<Cliente> findById(String id) {
            return customers.stream().filter(customer -> customer.id().equals(id)).findFirst();
        }

        @Override
        public boolean existsActiveCpf(String cpf, String exceptId) {
            return false;
        }

        @Override
        public boolean existsActiveCnpj(String cnpj, String exceptId) {
            return false;
        }

        @Override
        public boolean existsActiveEmail(String email, String exceptId) {
            return false;
        }

        @Override
        public boolean hasLinkedRecords(String id) {
            return false;
        }
    }
}

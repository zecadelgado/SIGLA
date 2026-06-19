package br.com.sigla.aplicacao.notificacoes.casodeuso;

import br.com.sigla.aplicacao.agenda.porta.saida.RepositorioAgenda;
import br.com.sigla.aplicacao.certificados.porta.saida.RepositorioCertificado;
import br.com.sigla.aplicacao.clientes.porta.saida.RepositorioCliente;
import br.com.sigla.aplicacao.contratos.porta.saida.RepositorioContrato;
import br.com.sigla.aplicacao.funcionarios.porta.saida.RepositorioFuncionario;
import br.com.sigla.aplicacao.notificacoes.porta.saida.RepositorioNotificacao;
import br.com.sigla.aplicacao.notificacoes.porta.saida.RepositorioNotificacaoConfiguracao;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.dominio.certificados.Certificado;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.dominio.funcionarios.Funcionario;
import br.com.sigla.dominio.notificacoes.CanalNotificacao;
import br.com.sigla.dominio.notificacoes.Destinatario;
import br.com.sigla.dominio.notificacoes.FonteTelefone;
import br.com.sigla.dominio.notificacoes.Notificacao;
import br.com.sigla.dominio.notificacoes.NotificacaoConfiguracao;
import br.com.sigla.dominio.notificacoes.OrigemNotificacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasoDeUsoGerarNotificacoesTest {

    private final FakeNotif notif = new FakeNotif();
    private final FakeConfig config = new FakeConfig();
    private final FakeCliente clientes = new FakeCliente();
    private final FakeFuncionario funcionarios = new FakeFuncionario();
    private final FakeContrato contratos = new FakeContrato();
    private final FakeCertificado certificados = new FakeCertificado();
    private final FakeAgenda agenda = new FakeAgenda();

    private CasoDeUsoGerarNotificacoes casoDeUso() {
        return new CasoDeUsoGerarNotificacoes(notif, config, clientes, funcionarios, contratos, certificados, agenda);
    }

    @Test
    void geraLembreteDeVisitaRespeitandoDiasAntes() {
        clientes.add(cliente("cli-1", "11999990000"));
        config.salvar(configVisita(Destinatario.CLIENTE, FonteTelefone.CLIENTE, 2));
        agenda.salvar(visita("v1", LocalDate.of(2026, 6, 20), VisitaAgendada.VisitStatus.SCHEDULED, true, 2, "fun-1"));

        casoDeUso().gerar(LocalDate.of(2026, 6, 17));
        assertEquals(0, pendentes().size());

        casoDeUso().gerar(LocalDate.of(2026, 6, 18));
        List<Notificacao> apos = pendentes();
        assertEquals(1, apos.size());
        Notificacao n = apos.getFirst();
        assertEquals(Notificacao.NotificacaoType.VISIT_UPCOMING, n.type());
        assertEquals(Destinatario.CLIENTE, n.destinatario().tipo());
        assertEquals(LocalDate.of(2026, 6, 18), n.triggerDate());
        assertEquals("5511999990000", n.destinatario().telefone());

        casoDeUso().gerar(LocalDate.of(2026, 6, 18));
        assertEquals(1, pendentes().size());
    }

    @Test
    void cancelaPendenteQuandoVisitaCancelada() {
        clientes.add(cliente("cli-1", "11999990000"));
        config.salvar(configVisita(Destinatario.CLIENTE, FonteTelefone.CLIENTE, 2));
        agenda.salvar(visita("v1", LocalDate.of(2026, 6, 20), VisitaAgendada.VisitStatus.SCHEDULED, true, 2, "fun-1"));
        casoDeUso().gerar(LocalDate.of(2026, 6, 18));
        assertEquals(1, pendentes().size());

        agenda.salvar(visita("v1", LocalDate.of(2026, 6, 20), VisitaAgendada.VisitStatus.CANCELLED, true, 2, "fun-1"));
        casoDeUso().gerar(LocalDate.of(2026, 6, 18));

        assertEquals(0, pendentes().size());
        assertEquals(1, comStatus(Notificacao.NotificacaoStatus.CANCELLED).size());
    }

    @Test
    void recalculaQuandoVisitaReagendada() {
        clientes.add(cliente("cli-1", "11999990000"));
        config.salvar(configVisita(Destinatario.CLIENTE, FonteTelefone.CLIENTE, 2));
        agenda.salvar(visita("v1", LocalDate.of(2026, 6, 20), VisitaAgendada.VisitStatus.SCHEDULED, true, 2, "fun-1"));
        casoDeUso().gerar(LocalDate.of(2026, 6, 18));
        assertEquals(1, pendentes().size());

        // reagenda para 25/06 -> novo gatilho 23/06
        agenda.salvar(visita("v1", LocalDate.of(2026, 6, 25), VisitaAgendada.VisitStatus.SCHEDULED, true, 2, "fun-1"));
        casoDeUso().gerar(LocalDate.of(2026, 6, 18));
        assertEquals(0, pendentes().size(), "pendente antigo deve ser cancelado e o novo ainda nao vencido");

        casoDeUso().gerar(LocalDate.of(2026, 6, 23));
        List<Notificacao> apos = pendentes();
        assertEquals(1, apos.size());
        assertEquals(LocalDate.of(2026, 6, 23), apos.getFirst().triggerDate());
    }

    @Test
    void ambosCriaDoisDestinatarios() {
        clientes.add(cliente("cli-1", "11999990000"));
        funcionarios.add(funcionario("fun-1", "11988887777"));
        config.salvar(configVisita(Destinatario.AMBOS, FonteTelefone.CLIENTE, 2));
        agenda.salvar(visita("v1", LocalDate.of(2026, 6, 20), VisitaAgendada.VisitStatus.SCHEDULED, true, 2, "fun-1"));

        casoDeUso().gerar(LocalDate.of(2026, 6, 18));

        List<Notificacao> apos = pendentes();
        assertEquals(2, apos.size());
        assertTrue(apos.stream().anyMatch(n -> n.destinatario().tipo() == Destinatario.CLIENTE));
        assertTrue(apos.stream().anyMatch(n -> n.destinatario().tipo() == Destinatario.FUNCIONARIO
                && n.destinatario().telefone().equals("5511988887777")));
    }

    @Test
    void contratoGeraComConfiguracaoAtiva() {
        clientes.add(cliente("cli-1", "11999990000"));
        contratos.add(contrato(LocalDate.of(2026, 7, 15), 30));
        config.salvar(new NotificacaoConfiguracao(
                "cfg-ctr", Notificacao.NotificacaoType.CONTRACT_EXPIRING, "Vencimento contrato",
                "Contrato vencendo", "Ola {{cliente_nome}}, contrato vence em {{contrato_vencimento}}",
                Destinatario.CLIENTE, OrigemNotificacao.SISTEMA, CanalNotificacao.WHATSAPP_N8N,
                FonteTelefone.CLIENTE, "", true, null, true, "admin",
                LocalDateTime.now(), LocalDateTime.now()));

        casoDeUso().gerar(LocalDate.of(2026, 6, 16));

        List<Notificacao> apos = pendentes();
        assertEquals(1, apos.size());
        assertEquals(Notificacao.NotificacaoType.CONTRACT_EXPIRING, apos.getFirst().type());
        assertTrue(apos.getFirst().message().contains("15/07/2026"));
    }

    @Test
    void semConfiguracaoNaoGeraContrato() {
        clientes.add(cliente("cli-1", "11999990000"));
        contratos.add(contrato(LocalDate.of(2026, 7, 15), 30));

        casoDeUso().gerar(LocalDate.of(2026, 6, 16));

        assertEquals(0, notif.findAll().size());
    }

    @Test
    void naoCriaSemTelefone() {
        clientes.add(cliente("cli-1", ""));
        config.salvar(configVisita(Destinatario.CLIENTE, FonteTelefone.CLIENTE, 2));
        agenda.salvar(visita("v1", LocalDate.of(2026, 6, 20), VisitaAgendada.VisitStatus.SCHEDULED, true, 2, "fun-1"));

        casoDeUso().gerar(LocalDate.of(2026, 6, 18));

        assertEquals(0, notif.findAll().size());
    }

    // ---- helpers ----

    private List<Notificacao> pendentes() {
        return comStatus(Notificacao.NotificacaoStatus.PENDING);
    }

    private List<Notificacao> comStatus(Notificacao.NotificacaoStatus status) {
        return notif.findAll().stream().filter(n -> n.status() == status).toList();
    }

    private VisitaAgendada visita(String id, LocalDate data, VisitaAgendada.VisitStatus status,
                                  boolean lembrete, int dias, String responsibleId) {
        return new VisitaAgendada(
                id, "cli-1", "", "", "",
                VisitaAgendada.VisitType.ONE_OFF, VisitaAgendada.Recurrence.NONE, data,
                "Dedetizacao", "Dedetizacao", "Joao",
                data.atTime(9, 0), data.atTime(10, 0), false,
                status, VisitaAgendada.VisitPriority.NORMAL, responsibleId,
                lembrete, dias, "Levar equipamento");
    }

    private Cliente cliente(String id, String phone) {
        return new Cliente(id, Cliente.TipoCliente.PESSOA_FISICA, "Maria", "", "", "", "", phone, "maria@x.com",
                "01000-000", "Rua A", "10", "", "Centro", "Sao Paulo", "SP", List.of(), "", true);
    }

    private Funcionario funcionario(String id, String telefone) {
        return new Funcionario(id, "Joao", "", "Tecnico", telefone, "joao@x.com", "", "", "", "", "", "", "",
                Funcionario.FuncionarioStatus.ACTIVE);
    }

    private Contrato contrato(LocalDate fim, int alertDias) {
        return new Contrato("ctr-1", "cli-1", "Mensal", fim.minusMonths(6), fim, Contrato.ContratoType.MONTHLY,
                Contrato.ServiceFrequency.MONTHLY, Contrato.ContratoStatus.ACTIVE, Contrato.RenewalRule.MANUAL,
                BigDecimal.TEN, true, alertDias, "");
    }

    private NotificacaoConfiguracao configVisita(Destinatario destinatario, FonteTelefone fonte, Integer dias) {
        return new NotificacaoConfiguracao(
                "cfg-visita", Notificacao.NotificacaoType.VISIT_UPCOMING, "Lembrete de visita",
                "Lembrete de visita", "Ola {{cliente_nome}}, visita em {{data_visita}}.",
                destinatario, OrigemNotificacao.SISTEMA, CanalNotificacao.WHATSAPP_N8N,
                fonte, "", true, dias, true, "admin", LocalDateTime.now(), LocalDateTime.now());
    }

    // ---- fakes ----

    private static final class FakeNotif implements RepositorioNotificacao {
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
            return new ArrayList<>(storage.values());
        }

        @Override
        public Optional<Notificacao> findById(String id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public List<Notificacao> findDuePending(LocalDateTime momento) {
            return storage.values().stream()
                    .filter(n -> n.status() == Notificacao.NotificacaoStatus.PENDING)
                    .filter(n -> !n.momentoDisparo().isAfter(momento))
                    .toList();
        }

        @Override
        public List<Notificacao> findByRelatedEntityId(String relatedEntityId) {
            return storage.values().stream()
                    .filter(n -> n.relatedEntityId().equals(relatedEntityId))
                    .toList();
        }
    }

    private static final class FakeConfig implements RepositorioNotificacaoConfiguracao {
        private final Map<String, NotificacaoConfiguracao> storage = new ConcurrentHashMap<>();

        void salvar(NotificacaoConfiguracao configuracao) {
            save(configuracao);
        }

        @Override
        public void save(NotificacaoConfiguracao configuracao) {
            storage.put(configuracao.id(), configuracao);
        }

        @Override
        public void deleteById(String id) {
            storage.remove(id);
        }

        @Override
        public Optional<NotificacaoConfiguracao> findById(String id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public List<NotificacaoConfiguracao> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public List<NotificacaoConfiguracao> findAtivasPorEvento(Notificacao.NotificacaoType eventType) {
            return storage.values().stream()
                    .filter(NotificacaoConfiguracao::ativo)
                    .filter(c -> c.eventType() == eventType)
                    .toList();
        }
    }

    private static final class FakeCliente implements RepositorioCliente {
        private final Map<String, Cliente> storage = new ConcurrentHashMap<>();

        void add(Cliente cliente) {
            storage.put(cliente.id(), cliente);
        }

        @Override
        public void save(Cliente customer) {
            add(customer);
        }

        @Override
        public void deleteById(String id) {
            storage.remove(id);
        }

        @Override
        public List<Cliente> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public Optional<Cliente> findById(String id) {
            return Optional.ofNullable(storage.get(id));
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

    private static final class FakeFuncionario implements RepositorioFuncionario {
        private final Map<String, Funcionario> storage = new ConcurrentHashMap<>();

        void add(Funcionario funcionario) {
            storage.put(funcionario.id(), funcionario);
        }

        @Override
        public void save(Funcionario employee) {
            add(employee);
        }

        @Override
        public void deleteById(String id) {
            storage.remove(id);
        }

        @Override
        public List<Funcionario> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public Optional<Funcionario> findById(String id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public boolean hasLinkedRecords(String id) {
            return false;
        }
    }

    private static final class FakeContrato implements RepositorioContrato {
        private final List<Contrato> storage = new ArrayList<>();

        void add(Contrato contrato) {
            storage.add(contrato);
        }

        @Override
        public void save(Contrato contract) {
            storage.add(contract);
        }

        @Override
        public List<Contrato> findAll() {
            return new ArrayList<>(storage);
        }

        @Override
        public Optional<Contrato> findById(String id) {
            return storage.stream().filter(c -> c.id().equals(id)).findFirst();
        }
    }

    private static final class FakeCertificado implements RepositorioCertificado {
        private final List<Certificado> storage = new ArrayList<>();

        @Override
        public void save(Certificado certificate) {
            storage.add(certificate);
        }

        @Override
        public List<Certificado> findAll() {
            return new ArrayList<>(storage);
        }

        @Override
        public Optional<Certificado> findById(String id) {
            return storage.stream().filter(c -> c.id().equals(id)).findFirst();
        }
    }

    private static final class FakeAgenda implements RepositorioAgenda {
        private final Map<String, VisitaAgendada> storage = new ConcurrentHashMap<>();

        void salvar(VisitaAgendada visita) {
            save(visita);
        }

        @Override
        public void save(VisitaAgendada schedule) {
            storage.put(schedule.id(), schedule);
        }

        @Override
        public List<VisitaAgendada> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public Optional<VisitaAgendada> findById(String id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public List<VisitaAgendada> findByResponsavel(String responsibleId) {
            return storage.values().stream()
                    .filter(v -> responsibleId.equals(v.responsibleId()))
                    .toList();
        }
    }
}

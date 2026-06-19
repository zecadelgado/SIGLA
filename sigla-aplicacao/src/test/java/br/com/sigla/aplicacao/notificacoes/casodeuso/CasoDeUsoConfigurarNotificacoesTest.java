package br.com.sigla.aplicacao.notificacoes.casodeuso;

import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoConfiguracaoNotificacao.ComandoSalvarConfiguracao;
import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoConfiguracaoNotificacao.ComandoTesteEnvio;
import br.com.sigla.aplicacao.notificacoes.porta.saida.PortaEnvioWhatsapp;
import br.com.sigla.aplicacao.notificacoes.porta.saida.RepositorioNotificacaoConfiguracao;
import br.com.sigla.dominio.notificacoes.CanalNotificacao;
import br.com.sigla.dominio.notificacoes.Destinatario;
import br.com.sigla.dominio.notificacoes.FonteTelefone;
import br.com.sigla.dominio.notificacoes.Notificacao;
import br.com.sigla.dominio.notificacoes.NotificacaoConfiguracao;
import br.com.sigla.dominio.notificacoes.OrigemNotificacao;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasoDeUsoConfigurarNotificacoesTest {

    @Test
    void salvarCriaConfiguracaoComIdGerado() {
        FakeRepositorio repositorio = new FakeRepositorio();
        CasoDeUsoConfigurarNotificacoes casoDeUso = new CasoDeUsoConfigurarNotificacoes(repositorio, new FakePorta(true));

        NotificacaoConfiguracao salva = casoDeUso.salvar(comando(null, true));

        assertNotNull(salva.id());
        assertFalse(salva.id().isBlank());
        assertEquals(1, repositorio.findAll().size());
        assertNotNull(salva.criadoEm());
    }

    @Test
    void salvarAtualizandoPreservaCriadoEm() {
        FakeRepositorio repositorio = new FakeRepositorio();
        CasoDeUsoConfigurarNotificacoes casoDeUso = new CasoDeUsoConfigurarNotificacoes(repositorio, new FakePorta(true));

        NotificacaoConfiguracao criada = casoDeUso.salvar(comando(null, true));
        NotificacaoConfiguracao atualizada = casoDeUso.salvar(comando(criada.id(), true));

        assertEquals(criada.id(), atualizada.id());
        assertEquals(criada.criadoEm(), atualizada.criadoEm());
        assertEquals(1, repositorio.findAll().size());
    }

    @Test
    void ativarEDesativarAlternamFlag() {
        FakeRepositorio repositorio = new FakeRepositorio();
        CasoDeUsoConfigurarNotificacoes casoDeUso = new CasoDeUsoConfigurarNotificacoes(repositorio, new FakePorta(true));
        NotificacaoConfiguracao criada = casoDeUso.salvar(comando(null, true));

        casoDeUso.desativar(criada.id());
        assertFalse(repositorio.findById(criada.id()).orElseThrow().ativo());

        casoDeUso.ativar(criada.id());
        assertTrue(repositorio.findById(criada.id()).orElseThrow().ativo());
    }

    @Test
    void listarAtivasPorEventoFiltra() {
        FakeRepositorio repositorio = new FakeRepositorio();
        CasoDeUsoConfigurarNotificacoes casoDeUso = new CasoDeUsoConfigurarNotificacoes(repositorio, new FakePorta(true));
        casoDeUso.salvar(comando(null, true));
        casoDeUso.salvar(comando(null, false));

        List<NotificacaoConfiguracao> ativas = casoDeUso.listarAtivasPorEvento(Notificacao.NotificacaoType.VISIT_UPCOMING);

        assertEquals(1, ativas.size());
        assertTrue(ativas.getFirst().ativo());
    }

    @Test
    void enviarTesteRenderizaMensagemENormalizaTelefone() {
        FakePorta porta = new FakePorta(true);
        CasoDeUsoConfigurarNotificacoes casoDeUso = new CasoDeUsoConfigurarNotificacoes(new FakeRepositorio(), porta);

        PortaEnvioWhatsapp.ResultadoEnvio resultado = casoDeUso.enviarTeste(new ComandoTesteEnvio(
                "(11) 98765-4321",
                "Teste",
                "Ola {{cliente_nome}}",
                Map.of("cliente_nome", "Maria"),
                Notificacao.NotificacaoType.VISIT_UPCOMING));

        assertTrue(resultado.sucesso());
        assertEquals("Ola Maria", porta.ultimoPayload.message());
        assertEquals("5511987654321", porta.ultimoPayload.recipientPhone());
    }

    @Test
    void enviarTesteComTelefoneInvalidoNaoChamaPorta() {
        FakePorta porta = new FakePorta(true);
        CasoDeUsoConfigurarNotificacoes casoDeUso = new CasoDeUsoConfigurarNotificacoes(new FakeRepositorio(), porta);

        PortaEnvioWhatsapp.ResultadoEnvio resultado = casoDeUso.enviarTeste(new ComandoTesteEnvio(
                "  ", "Teste", "Ola", Map.of(), Notificacao.NotificacaoType.MANUAL));

        assertEquals(PortaEnvioWhatsapp.ResultadoEnvio.Situacao.FALHA, resultado.situacao());
        assertEquals(0, porta.chamadas);
    }

    private ComandoSalvarConfiguracao comando(String id, boolean ativo) {
        return new ComandoSalvarConfiguracao(
                id,
                Notificacao.NotificacaoType.VISIT_UPCOMING,
                "Lembrete de visita",
                "Lembrete de visita",
                "Ola {{cliente_nome}}, sua visita e em {{data_visita}}.",
                Destinatario.CLIENTE,
                OrigemNotificacao.SISTEMA,
                CanalNotificacao.WHATSAPP_N8N,
                FonteTelefone.CLIENTE,
                "",
                true,
                2,
                ativo,
                "admin");
    }

    private static final class FakePorta implements PortaEnvioWhatsapp {
        private final boolean sucesso;
        private PayloadNotificacaoWhatsapp ultimoPayload;
        private int chamadas;

        private FakePorta(boolean sucesso) {
            this.sucesso = sucesso;
        }

        @Override
        public ResultadoEnvio enviar(PayloadNotificacaoWhatsapp payload) {
            this.ultimoPayload = payload;
            this.chamadas++;
            return sucesso ? ResultadoEnvio.enviado() : ResultadoEnvio.falha("erro");
        }
    }

    private static final class FakeRepositorio implements RepositorioNotificacaoConfiguracao {
        private final Map<String, NotificacaoConfiguracao> storage = new ConcurrentHashMap<>();

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
            return storage.values().stream().toList();
        }

        @Override
        public List<NotificacaoConfiguracao> findAtivasPorEvento(Notificacao.NotificacaoType eventType) {
            return storage.values().stream()
                    .filter(NotificacaoConfiguracao::ativo)
                    .filter(configuracao -> configuracao.eventType() == eventType)
                    .toList();
        }
    }
}

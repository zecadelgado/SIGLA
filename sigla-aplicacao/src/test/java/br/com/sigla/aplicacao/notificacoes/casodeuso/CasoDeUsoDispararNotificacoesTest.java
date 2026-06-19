package br.com.sigla.aplicacao.notificacoes.casodeuso;

import br.com.sigla.aplicacao.notificacoes.porta.saida.PortaEnvioWhatsapp;
import br.com.sigla.aplicacao.notificacoes.porta.saida.RepositorioNotificacao;
import br.com.sigla.dominio.notificacoes.CanalNotificacao;
import br.com.sigla.dominio.notificacoes.DestinatarioNotificacao;
import br.com.sigla.dominio.notificacoes.Notificacao;
import br.com.sigla.dominio.notificacoes.RemetenteNotificacao;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CasoDeUsoDispararNotificacoesTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 6, 18, 8, 0);

    @Test
    void enviaPendenteVencidaEMarcaSent() {
        FakeNotif notif = new FakeNotif();
        notif.save(pendente("n1", AGORA.minusHours(1)));
        CasoDeUsoDispararNotificacoes casoDeUso = new CasoDeUsoDispararNotificacoes(
                notif, new FakePorta(PortaEnvioWhatsapp.ResultadoEnvio.enviado()));

        int enviados = casoDeUso.dispatchDue(AGORA);

        assertEquals(1, enviados);
        Notificacao atualizada = notif.findById("n1").orElseThrow();
        assertEquals(Notificacao.NotificacaoStatus.SENT, atualizada.status());
        assertNotNull(atualizada.sentAt());
    }

    @Test
    void naoEnviaPendenteFutura() {
        FakeNotif notif = new FakeNotif();
        notif.save(pendente("n1", AGORA.plusDays(1)));
        CasoDeUsoDispararNotificacoes casoDeUso = new CasoDeUsoDispararNotificacoes(
                notif, new FakePorta(PortaEnvioWhatsapp.ResultadoEnvio.enviado()));

        int enviados = casoDeUso.dispatchDue(AGORA);

        assertEquals(0, enviados);
        assertEquals(Notificacao.NotificacaoStatus.PENDING, notif.findById("n1").orElseThrow().status());
    }

    @Test
    void desabilitadoMantemPending() {
        FakeNotif notif = new FakeNotif();
        notif.save(pendente("n1", AGORA.minusHours(1)));
        CasoDeUsoDispararNotificacoes casoDeUso = new CasoDeUsoDispararNotificacoes(
                notif, new FakePorta(PortaEnvioWhatsapp.ResultadoEnvio.desabilitado("off")));

        int enviados = casoDeUso.dispatchDue(AGORA);

        assertEquals(0, enviados);
        assertEquals(Notificacao.NotificacaoStatus.PENDING, notif.findById("n1").orElseThrow().status());
    }

    @Test
    void modoTesteSimuladoMarcaSent() {
        FakeNotif notif = new FakeNotif();
        notif.save(pendente("n1", AGORA.minusHours(1)));
        CasoDeUsoDispararNotificacoes casoDeUso = new CasoDeUsoDispararNotificacoes(
                notif, new FakePorta(PortaEnvioWhatsapp.ResultadoEnvio.simulado("modo-teste")));

        int enviados = casoDeUso.dispatchDue(AGORA);

        assertEquals(1, enviados);
        assertEquals(Notificacao.NotificacaoStatus.SENT, notif.findById("n1").orElseThrow().status());
    }

    @Test
    void falhaMarcaFailedComDetalhe() {
        FakeNotif notif = new FakeNotif();
        notif.save(pendente("n1", AGORA.minusHours(1)));
        CasoDeUsoDispararNotificacoes casoDeUso = new CasoDeUsoDispararNotificacoes(
                notif, new FakePorta(PortaEnvioWhatsapp.ResultadoEnvio.falha("n8n indisponivel")));

        int enviados = casoDeUso.dispatchDue(AGORA);

        assertEquals(0, enviados);
        Notificacao atualizada = notif.findById("n1").orElseThrow();
        assertEquals(Notificacao.NotificacaoStatus.FAILED, atualizada.status());
        assertEquals("n8n indisponivel", atualizada.lastError());
        assertEquals(1, atualizada.attempts());
    }

    @Test
    void reprocessarFalhasReenvia() {
        FakeNotif notif = new FakeNotif();
        notif.save(pendente("n1", AGORA.minusHours(1)).registrarFalha("erro anterior"));
        CasoDeUsoDispararNotificacoes casoDeUso = new CasoDeUsoDispararNotificacoes(
                notif, new FakePorta(PortaEnvioWhatsapp.ResultadoEnvio.enviado()));

        int reprocessados = casoDeUso.reprocessarFalhas();

        assertEquals(1, reprocessados);
        assertEquals(Notificacao.NotificacaoStatus.SENT, notif.findById("n1").orElseThrow().status());
    }

    private Notificacao pendente(String id, LocalDateTime scheduledFor) {
        return Notificacao.builder()
                .id(id)
                .type(Notificacao.NotificacaoType.VISIT_UPCOMING)
                .title("Lembrete")
                .message("Mensagem")
                .relatedEntityId("evento-1")
                .triggerDate(scheduledFor.toLocalDate())
                .status(Notificacao.NotificacaoStatus.PENDING)
                .destinatario(DestinatarioNotificacao.cliente("c1", "Maria", "5511999990000"))
                .remetente(RemetenteNotificacao.sistema())
                .scheduledFor(scheduledFor)
                .canal(CanalNotificacao.WHATSAPP_N8N)
                .build();
    }

    private static final class FakePorta implements PortaEnvioWhatsapp {
        private final ResultadoEnvio resposta;

        private FakePorta(ResultadoEnvio resposta) {
            this.resposta = resposta;
        }

        @Override
        public ResultadoEnvio enviar(PayloadNotificacaoWhatsapp payload) {
            return resposta;
        }
    }

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
}

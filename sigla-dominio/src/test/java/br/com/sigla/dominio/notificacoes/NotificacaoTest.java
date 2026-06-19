package br.com.sigla.dominio.notificacoes;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificacaoTest {

    @Test
    void construtorBasicoUsaDefaults() {
        Notificacao notificacao = new Notificacao(
                "n1",
                Notificacao.NotificacaoType.MANUAL,
                "Titulo",
                "Mensagem",
                "entidade-1",
                LocalDate.of(2026, 6, 18),
                Notificacao.NotificacaoStatus.PENDING
        );

        assertEquals("SIGLA", notificacao.source());
        assertEquals(0, notificacao.attempts());
        assertTrue(notificacao.metadata().isEmpty());
    }

    @Test
    void builderConstroiNotificacaoRica() {
        Notificacao notificacao = Notificacao.builder()
                .id("n2")
                .type(Notificacao.NotificacaoType.VISIT_UPCOMING)
                .title("Lembrete de visita")
                .message("Sua visita e amanha")
                .relatedEntityId("visita-9")
                .triggerDate(LocalDate.of(2026, 6, 18))
                .status(Notificacao.NotificacaoStatus.PENDING)
                .destinatario(DestinatarioNotificacao.cliente("c1", "Maria", "5511999999999"))
                .remetente(RemetenteNotificacao.sistema())
                .canal(CanalNotificacao.WHATSAPP_N8N)
                .scheduledFor(LocalDateTime.of(2026, 6, 18, 8, 0))
                .metadata(Map.of("cliente_nome", "Maria"))
                .build();

        assertEquals(Destinatario.CLIENTE, notificacao.destinatario().tipo());
        assertEquals("Maria", notificacao.metadata().get("cliente_nome"));
        assertEquals(LocalDateTime.of(2026, 6, 18, 8, 0), notificacao.momentoDisparo());
    }

    @Test
    void registrarFalhaIncrementaTentativas() {
        Notificacao notificacao = new Notificacao(
                "n3",
                Notificacao.NotificacaoType.MANUAL,
                "Titulo",
                "Mensagem",
                "entidade-1",
                LocalDate.of(2026, 6, 18),
                Notificacao.NotificacaoStatus.PENDING
        ).registrarFalha("erro de rede");

        assertEquals(Notificacao.NotificacaoStatus.FAILED, notificacao.status());
        assertEquals(1, notificacao.attempts());
        assertEquals("erro de rede", notificacao.lastError());
    }

    @Test
    void marcarEnviadaDefineSentAt() {
        LocalDateTime quando = LocalDateTime.of(2026, 6, 18, 8, 5);
        Notificacao notificacao = new Notificacao(
                "n4",
                Notificacao.NotificacaoType.MANUAL,
                "Titulo",
                "Mensagem",
                "entidade-1",
                LocalDate.of(2026, 6, 18),
                Notificacao.NotificacaoStatus.PENDING
        ).marcarEnviada(quando);

        assertEquals(Notificacao.NotificacaoStatus.SENT, notificacao.status());
        assertEquals(quando, notificacao.sentAt());
    }
}

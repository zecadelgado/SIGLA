package br.com.sigla.infraestrutura.integracao;

import br.com.sigla.aplicacao.notificacoes.porta.saida.PortaEnvioWhatsapp.PayloadNotificacaoWhatsapp;
import br.com.sigla.aplicacao.notificacoes.porta.saida.PortaEnvioWhatsapp.ResultadoEnvio;
import br.com.sigla.infraestrutura.configuracao.PropriedadesNotificacoesSigla;
import br.com.sigla.infraestrutura.integracao.AdaptadorEnvioWhatsappN8n.Transporte;
import org.junit.jupiter.api.Test;

import java.net.http.HttpRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptadorEnvioWhatsappN8nTest {

    private PayloadNotificacaoWhatsapp payload() {
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("cliente_nome", "Maria");
        return new PayloadNotificacaoWhatsapp(
                "evt-1", "VISIT_UPCOMING", "SIGLA", "CLIENTE", "Maria", "5511999998888",
                "SISTEMA", "SIGLA", "cli-1", "fun-1", "visita-1", "tpl-1", "Ola Maria",
                LocalDateTime.of(2026, 6, 18, 8, 0), meta);
    }

    @Test
    void desabilitadoNaoEnvia() {
        PropriedadesNotificacoesSigla props = new PropriedadesNotificacoesSigla();
        props.getWhatsapp().setEnabled(false);
        AtomicBoolean chamou = new AtomicBoolean(false);
        AdaptadorEnvioWhatsappN8n adapter = new AdaptadorEnvioWhatsappN8n(props, req -> {
            chamou.set(true);
            return new Transporte.Resposta(200, "");
        });

        ResultadoEnvio resultado = adapter.enviar(payload());

        assertEquals(ResultadoEnvio.Situacao.DESABILITADO, resultado.situacao());
        assertFalse(chamou.get());
    }

    @Test
    void modoTesteSimulaSemEnviar() {
        PropriedadesNotificacoesSigla props = new PropriedadesNotificacoesSigla();
        props.getWhatsapp().setEnabled(true);
        props.setModoTeste(true);
        AtomicBoolean chamou = new AtomicBoolean(false);
        AdaptadorEnvioWhatsappN8n adapter = new AdaptadorEnvioWhatsappN8n(props, req -> {
            chamou.set(true);
            return new Transporte.Resposta(200, "");
        });

        ResultadoEnvio resultado = adapter.enviar(payload());

        assertEquals(ResultadoEnvio.Situacao.SIMULADO, resultado.situacao());
        assertTrue(resultado.sucesso());
        assertFalse(chamou.get());
    }

    @Test
    void enviaComTokenERetornaEnviado() {
        PropriedadesNotificacoesSigla props = new PropriedadesNotificacoesSigla();
        props.getWhatsapp().setEnabled(true);
        props.setModoTeste(false);
        props.getWebhook().setUrl("https://n8n.example/webhook/abc");
        props.getWebhook().setToken("segredo");
        AtomicReference<HttpRequest> capturado = new AtomicReference<>();
        AdaptadorEnvioWhatsappN8n adapter = new AdaptadorEnvioWhatsappN8n(props, req -> {
            capturado.set(req);
            return new Transporte.Resposta(200, "ok");
        });

        ResultadoEnvio resultado = adapter.enviar(payload());

        assertEquals(ResultadoEnvio.Situacao.ENVIADO, resultado.situacao());
        assertEquals("https://n8n.example/webhook/abc", capturado.get().uri().toString());
        assertEquals("Bearer segredo", capturado.get().headers().firstValue("Authorization").orElse(""));
    }

    @Test
    void httpNao2xxRetornaFalha() {
        PropriedadesNotificacoesSigla props = new PropriedadesNotificacoesSigla();
        props.getWhatsapp().setEnabled(true);
        props.getWebhook().setUrl("https://n8n.example/webhook/abc");
        AdaptadorEnvioWhatsappN8n adapter = new AdaptadorEnvioWhatsappN8n(props,
                req -> new Transporte.Resposta(500, "erro"));

        ResultadoEnvio resultado = adapter.enviar(payload());

        assertEquals(ResultadoEnvio.Situacao.FALHA, resultado.situacao());
    }

    @Test
    void urlNaoConfiguradaRetornaFalha() {
        PropriedadesNotificacoesSigla props = new PropriedadesNotificacoesSigla();
        props.getWhatsapp().setEnabled(true);
        AdaptadorEnvioWhatsappN8n adapter = new AdaptadorEnvioWhatsappN8n(props,
                req -> new Transporte.Resposta(200, ""));

        ResultadoEnvio resultado = adapter.enviar(payload());

        assertEquals(ResultadoEnvio.Situacao.FALHA, resultado.situacao());
    }

    @Test
    void jsonEscapaCaracteresDeControle() {
        PropriedadesNotificacoesSigla props = new PropriedadesNotificacoesSigla();
        AdaptadorEnvioWhatsappN8n adapter = new AdaptadorEnvioWhatsappN8n(props,
                req -> new Transporte.Resposta(200, ""));
        String tab = String.valueOf((char) 9);
        String bel = String.valueOf((char) 7);
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("obs", "linha" + tab + "col" + bel + "fim");
        PayloadNotificacaoWhatsapp p = new PayloadNotificacaoWhatsapp(
                "e", "VISIT_UPCOMING", "SIGLA", "CLIENTE", "Maria", "5511999998888",
                "SISTEMA", "SIGLA", "c", "f", "v", "t", "ola" + tab + "mundo",
                LocalDateTime.of(2026, 6, 18, 8, 0), meta);

        String json = adapter.toJson(p);

        assertTrue(json.contains("\\t"), "tab deve ser escapado");
        assertTrue(json.contains("\\u0007"), "caractere de controle deve virar \\u00xx");
        assertFalse(json.contains(bel), "nenhum caractere de controle cru no JSON");
    }

    @Test
    void jsonContemCamposPrincipais() {
        PropriedadesNotificacoesSigla props = new PropriedadesNotificacoesSigla();
        AdaptadorEnvioWhatsappN8n adapter = new AdaptadorEnvioWhatsappN8n(props,
                req -> new Transporte.Resposta(200, ""));

        String json = adapter.toJson(payload());

        assertTrue(json.contains("\"eventId\":\"evt-1\""));
        assertTrue(json.contains("\"eventType\":\"VISIT_UPCOMING\""));
        assertTrue(json.contains("\"recipientPhone\":\"5511999998888\""));
        assertTrue(json.contains("\"scheduledFor\":\"2026-06-18T08:00:00\""));
        assertTrue(json.contains("\"metadata\":{\"cliente_nome\":\"Maria\"}"));
    }
}

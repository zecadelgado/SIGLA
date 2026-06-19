package br.com.sigla.infraestrutura.integracao;

import br.com.sigla.aplicacao.notificacoes.porta.saida.PortaEnvioWhatsapp;
import br.com.sigla.infraestrutura.configuracao.PropriedadesNotificacoesSigla;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

/**
 * Envia o payload ao webhook do n8n (que aciona a Uazap). Respeita os flags
 * {@code whatsapp.enabled} (liga/desliga global) e {@code modo-teste} (simula sem POST real),
 * e adiciona o token como header Authorization quando configurado.
 */
@Component
public class AdaptadorEnvioWhatsappN8n implements PortaEnvioWhatsapp {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PropriedadesNotificacoesSigla propriedades;
    private final Transporte transporte;

    @Autowired
    public AdaptadorEnvioWhatsappN8n(PropriedadesNotificacoesSigla propriedades) {
        this(propriedades, transporteHttpPadrao());
    }

    AdaptadorEnvioWhatsappN8n(PropriedadesNotificacoesSigla propriedades, Transporte transporte) {
        this.propriedades = Objects.requireNonNull(propriedades);
        this.transporte = Objects.requireNonNull(transporte);
    }

    @Override
    public ResultadoEnvio enviar(PayloadNotificacaoWhatsapp payload) {
        if (!propriedades.getWhatsapp().isEnabled()) {
            return ResultadoEnvio.desabilitado("Envio de WhatsApp desabilitado (sigla.notificacoes.whatsapp.enabled=false).");
        }
        if (propriedades.isModoTeste()) {
            return ResultadoEnvio.simulado("Modo-teste ativo: envio simulado, nada enviado ao n8n.");
        }
        String url = propriedades.getWebhook().getUrl();
        if (url == null || url.isBlank()) {
            return ResultadoEnvio.falha("URL do webhook nao configurada (sigla.notificacoes.webhook.url).");
        }

        HttpRequest request;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url.trim()))
                    .header("Content-Type", "application/json");
            String token = propriedades.getWebhook().getToken();
            if (token != null && !token.isBlank()) {
                builder.header("Authorization", "Bearer " + token.trim());
            }
            request = builder.POST(HttpRequest.BodyPublishers.ofString(toJson(payload))).build();
        } catch (IllegalArgumentException exception) {
            return ResultadoEnvio.falha("URL do webhook invalida: " + exception.getMessage());
        }

        try {
            Transporte.Resposta resposta = transporte.enviar(request);
            if (resposta.status() >= 200 && resposta.status() < 300) {
                return ResultadoEnvio.enviado();
            }
            return ResultadoEnvio.falha("Webhook retornou HTTP " + resposta.status() + ".");
        } catch (IOException exception) {
            return ResultadoEnvio.falha("Falha de IO ao enviar webhook: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ResultadoEnvio.falha("Envio interrompido.");
        }
    }

    String toJson(PayloadNotificacaoWhatsapp payload) {
        StringBuilder sb = new StringBuilder("{");
        sb.append(campo("eventId", payload.eventId())).append(",");
        sb.append(campo("eventType", payload.eventType())).append(",");
        sb.append(campo("source", payload.source())).append(",");
        sb.append(campo("recipientType", payload.recipientType())).append(",");
        sb.append(campo("recipientName", payload.recipientName())).append(",");
        sb.append(campo("recipientPhone", payload.recipientPhone())).append(",");
        sb.append(campo("senderType", payload.senderType())).append(",");
        sb.append(campo("senderName", payload.senderName())).append(",");
        sb.append(campo("customerId", payload.customerId())).append(",");
        sb.append(campo("employeeId", payload.employeeId())).append(",");
        sb.append(campo("relatedEntityId", payload.relatedEntityId())).append(",");
        sb.append(campo("templateId", payload.templateId())).append(",");
        sb.append(campo("message", payload.message())).append(",");
        sb.append(campo("scheduledFor", payload.scheduledFor() == null ? "" : DATA_HORA.format(payload.scheduledFor()))).append(",");
        sb.append("\"modoTeste\":").append(propriedades.isModoTeste()).append(",");
        sb.append("\"metadata\":").append(metadataJson(payload.metadata()));
        sb.append("}");
        return sb.toString();
    }

    private String metadataJson(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean primeiro = true;
        for (Map.Entry<String, String> entrada : metadata.entrySet()) {
            if (!primeiro) {
                sb.append(",");
            }
            primeiro = false;
            sb.append(campo(entrada.getKey(), entrada.getValue()));
        }
        return sb.append("}").toString();
    }

    private String campo(String nome, String valor) {
        return "\"" + escape(nome) + "\":\"" + escape(valor == null ? "" : valor) + "\"";
    }

    private String escape(String valor) {
        return valor.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static Transporte transporteHttpPadrao() {
        HttpClient client = HttpClient.newHttpClient();
        return request -> {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new Transporte.Resposta(response.statusCode(), response.body());
        };
    }

    /** Seam de transporte HTTP, para permitir testes sem rede. */
    @FunctionalInterface
    interface Transporte {
        Resposta enviar(HttpRequest request) throws IOException, InterruptedException;

        record Resposta(int status, String corpo) {
        }
    }
}

package br.com.sigla.infraestrutura.integracao;

import br.com.sigla.aplicacao.notificacoes.porta.saida.PortaWebhookNotificacao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Component
public class AdaptadorWebhookN8n implements PortaWebhookNotificacao {

    private static final String WEBHOOK_PROPERTY = "N8N_WEBHOOK_VENCIMENTOS_URL";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Environment environment;
    private final HttpClient httpClient;

    @Autowired
    public AdaptadorWebhookN8n(Environment environment) {
        this(environment, HttpClient.newHttpClient());
    }

    AdaptadorWebhookN8n(Environment environment, HttpClient httpClient) {
        this.environment = Objects.requireNonNull(environment);
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    @Override
    public ResultadoEnvio enviar(PayloadWebhook payload) {
        String webhookUrl = webhookUrl();
        if (webhookUrl.isBlank()) {
            return ResultadoEnvio.comFalha("Variavel N8N_WEBHOOK_VENCIMENTOS_URL nao configurada.");
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(payload)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return ResultadoEnvio.enviado();
            }
            return ResultadoEnvio.comFalha("Webhook retornou HTTP " + response.statusCode() + ".");
        } catch (IOException exception) {
            return ResultadoEnvio.comFalha("Falha de IO ao enviar webhook: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ResultadoEnvio.comFalha("Envio interrompido.");
        } catch (IllegalArgumentException exception) {
            return ResultadoEnvio.comFalha("URL do webhook invalida: " + exception.getMessage());
        }
    }

    private String webhookUrl() {
        String configured = environment.getProperty(WEBHOOK_PROPERTY);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(WEBHOOK_PROPERTY);
        }
        return configured == null ? "" : configured.trim();
    }

    private String toJson(PayloadWebhook payload) {
        String entityFields = "contrato_vencimento".equals(payload.tipo())
                ? field("contrato_id", payload.entidadeId()) + ","
                + field("data_inicio", formatDate(payload.dataReferenciaInicial())) + ","
                + field("data_fim", formatDate(payload.dataVencimento())) + ","
                + field("tipo_contrato", payload.descricao()) + ","
                : field("certificado_id", payload.entidadeId()) + ","
                + field("data_emissao", formatDate(payload.dataReferenciaInicial())) + ","
                + field("data_validade", formatDate(payload.dataVencimento())) + ",";
        return "{"
                + field("tipo", payload.tipo()) + ","
                + field("cliente_id", payload.clienteId()) + ","
                + field("cliente_nome", payload.clienteNome()) + ","
                + field("entidade_id", payload.entidadeId()) + ","
                + entityFields
                + field("descricao", payload.descricao()) + ","
                + field("data_referencia_inicial", formatDate(payload.dataReferenciaInicial())) + ","
                + field("data_vencimento", formatDate(payload.dataVencimento())) + ","
                + "\"dias_alerta\":" + payload.diasAlerta() + ","
                + field("mensagem", payload.mensagem())
                + "}";
    }

    private String field(String name, String value) {
        return "\"" + escape(name) + "\":\"" + escape(value == null ? "" : value) + "\"";
    }

    private String formatDate(java.time.LocalDate date) {
        return date == null ? "" : DATE_FORMATTER.format(date);
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

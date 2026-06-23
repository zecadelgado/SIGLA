package br.com.sigla.dominio.notificacoes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Notificacao operacional. Alem dos campos basicos (tipo, titulo, mensagem, entidade relacionada),
 * carrega os dados necessarios para o envio via WhatsApp/n8n: destinatario, remetente, template,
 * horario agendado, metadados (variaveis) e o estado de entrega.
 *
 * <p>Por compatibilidade, o construtor de 7 argumentos continua valido e usa defaults para os
 * campos ricos. A construcao recomendada e via {@link #builder()}.
 */
public record Notificacao(
        String id,
        NotificacaoType type,
        String title,
        String message,
        String relatedEntityId,
        LocalDate triggerDate,
        NotificacaoStatus status,
        DestinatarioNotificacao destinatario,
        RemetenteNotificacao remetente,
        String templateId,
        LocalDateTime scheduledFor,
        String source,
        CanalNotificacao canal,
        int attempts,
        String lastError,
        Map<String, String> metadata,
        LocalDateTime sentAt,
        String createdBy
) {
    public Notificacao {
        id = requireText(id, "id");
        type = Objects.requireNonNull(type, "type is required");
        title = requireText(title, "title");
        message = requireText(message, "message");
        relatedEntityId = requireText(relatedEntityId, "relatedEntityId");
        triggerDate = Objects.requireNonNull(triggerDate, "triggerDate is required");
        status = Objects.requireNonNull(status, "status is required");
        source = (source == null || source.isBlank()) ? "SIGLA" : source.trim();
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts must not be negative");
        }
        templateId = normalizeOptional(templateId);
        lastError = normalizeOptional(lastError);
        createdBy = normalizeOptional(createdBy);
        metadata = copiaImutavel(metadata);
    }

    /** Construtor compativel com a versao anterior (somente campos basicos). */
    public Notificacao(
            String id,
            NotificacaoType type,
            String title,
            String message,
            String relatedEntityId,
            LocalDate triggerDate,
            NotificacaoStatus status
    ) {
        this(id, type, title, message, relatedEntityId, triggerDate, status,
                null, null, null, null, "SIGLA", null, 0, null, Map.of(), null, null);
    }

    public Notificacao comStatus(NotificacaoStatus novoStatus) {
        return toBuilder().status(novoStatus).build();
    }

    /** Marca como enviada com sucesso. */
    public Notificacao marcarEnviada(LocalDateTime quando) {
        return toBuilder()
                .status(NotificacaoStatus.SENT)
                .sentAt(quando)
                .lastError("")
                .build();
    }

    /** Registra uma falha de envio, incrementando a contagem de tentativas. */
    public Notificacao registrarFalha(String detalhe) {
        return toBuilder()
                .status(NotificacaoStatus.FAILED)
                .attempts(attempts + 1)
                .lastError(detalhe)
                .build();
    }

    /** Marca como cancelada (ex.: o evento relacionado foi cancelado). */
    public Notificacao cancelar() {
        return comStatus(NotificacaoStatus.CANCELLED);
    }

    public boolean estaAtiva() {
        return status == NotificacaoStatus.PENDING
                || status == NotificacaoStatus.SENT
                || status == NotificacaoStatus.OPEN;
    }

    public LocalDateTime momentoDisparo() {
        return scheduledFor != null ? scheduledFor : triggerDate.atStartOfDay();
    }

    public enum NotificacaoType {
        CONTRACT_EXPIRING("Vencimento de contrato"),
        CERTIFICATE_EXPIRING("Vencimento de certificado"),
        INSTALLMENT_OVERDUE("Parcela em atraso"),
        VISIT_UPCOMING("Visita agendada"),
        VISIT_MISSED("Visita não realizada"),
        SERVICE_ORDER_UPCOMING("Ordem de serviço próxima"),
        CONTACT_FOLLOWUP("Acompanhamento de contato"),
        EMPLOYEE_TASK("Tarefa do funcionário"),
        MANUAL("Manual");

        private final String rotulo;

        NotificacaoType(String rotulo) {
            this.rotulo = rotulo;
        }

        public String rotulo() {
            return rotulo;
        }
    }

    public enum NotificacaoStatus {
        OPEN("Aberta"),
        RESOLVED("Resolvida"),
        PENDING("Pendente"),
        SENT("Enviada"),
        FAILED("Falhou"),
        CANCELLED("Cancelada");

        private final String rotulo;

        NotificacaoStatus(String rotulo) {
            this.rotulo = rotulo;
        }

        public String rotulo() {
            return rotulo;
        }
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .type(type)
                .title(title)
                .message(message)
                .relatedEntityId(relatedEntityId)
                .triggerDate(triggerDate)
                .status(status)
                .destinatario(destinatario)
                .remetente(remetente)
                .templateId(templateId)
                .scheduledFor(scheduledFor)
                .source(source)
                .canal(canal)
                .attempts(attempts)
                .lastError(lastError)
                .metadata(metadata)
                .sentAt(sentAt)
                .createdBy(createdBy);
    }

    public static Builder builder() {
        return new Builder();
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }

    private static Map<String, String> copiaImutavel(Map<String, String> origem) {
        if (origem == null || origem.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copia = new LinkedHashMap<>();
        origem.forEach((chave, valor) -> {
            if (chave != null) {
                copia.put(chave, valor == null ? "" : valor);
            }
        });
        return Map.copyOf(copia);
    }

    public static final class Builder {
        private String id;
        private NotificacaoType type;
        private String title;
        private String message;
        private String relatedEntityId;
        private LocalDate triggerDate;
        private NotificacaoStatus status = NotificacaoStatus.PENDING;
        private DestinatarioNotificacao destinatario;
        private RemetenteNotificacao remetente;
        private String templateId;
        private LocalDateTime scheduledFor;
        private String source = "SIGLA";
        private CanalNotificacao canal;
        private int attempts;
        private String lastError;
        private Map<String, String> metadata = Map.of();
        private LocalDateTime sentAt;
        private String createdBy;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder type(NotificacaoType type) {
            this.type = type;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder relatedEntityId(String relatedEntityId) {
            this.relatedEntityId = relatedEntityId;
            return this;
        }

        public Builder triggerDate(LocalDate triggerDate) {
            this.triggerDate = triggerDate;
            return this;
        }

        public Builder status(NotificacaoStatus status) {
            this.status = status;
            return this;
        }

        public Builder destinatario(DestinatarioNotificacao destinatario) {
            this.destinatario = destinatario;
            return this;
        }

        public Builder remetente(RemetenteNotificacao remetente) {
            this.remetente = remetente;
            return this;
        }

        public Builder templateId(String templateId) {
            this.templateId = templateId;
            return this;
        }

        public Builder scheduledFor(LocalDateTime scheduledFor) {
            this.scheduledFor = scheduledFor;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder canal(CanalNotificacao canal) {
            this.canal = canal;
            return this;
        }

        public Builder attempts(int attempts) {
            this.attempts = attempts;
            return this;
        }

        public Builder lastError(String lastError) {
            this.lastError = lastError;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder sentAt(LocalDateTime sentAt) {
            this.sentAt = sentAt;
            return this;
        }

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Notificacao build() {
            return new Notificacao(
                    id, type, title, message, relatedEntityId, triggerDate, status,
                    destinatario, remetente, templateId, scheduledFor, source, canal,
                    attempts, lastError, metadata, sentAt, createdBy
            );
        }
    }
}

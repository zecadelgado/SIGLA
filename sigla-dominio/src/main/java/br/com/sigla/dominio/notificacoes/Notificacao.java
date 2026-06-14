package br.com.sigla.dominio.notificacoes;

import java.time.LocalDate;
import java.util.Objects;

public record Notificacao(
        String id,
        NotificacaoType type,
        String title,
        String message,
        String relatedEntityId,
        LocalDate triggerDate,
        NotificacaoStatus status
) {
    public Notificacao {
        id = requireText(id, "id");
        type = Objects.requireNonNull(type, "type is required");
        title = requireText(title, "title");
        message = requireText(message, "message");
        relatedEntityId = requireText(relatedEntityId, "relatedEntityId");
        triggerDate = Objects.requireNonNull(triggerDate, "triggerDate is required");
        status = Objects.requireNonNull(status, "status is required");
    }

    public enum NotificacaoType {
        CONTRACT_EXPIRING,
        CERTIFICATE_EXPIRING,
        INSTALLMENT_OVERDUE,
        VISIT_UPCOMING,
        VISIT_MISSED
    }

    public enum NotificacaoStatus {
        OPEN,
        RESOLVED,
        PENDING,
        SENT,
        FAILED
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}


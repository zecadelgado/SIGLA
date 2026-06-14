package br.com.sigla.dominio.auditoria;

import java.time.LocalDateTime;
import java.util.Objects;

public record EventoAuditoria(
        String id,
        String entidadeTipo,
        String entidadeId,
        String acao,
        String detalhe,
        String usuarioId,
        LocalDateTime createdAt
) {
    public EventoAuditoria {
        id = requireText(id, "id");
        entidadeTipo = requireText(entidadeTipo, "entidadeTipo");
        entidadeId = requireText(entidadeId, "entidadeId");
        acao = requireText(acao, "acao");
        detalhe = detalhe == null ? "" : detalhe.trim();
        usuarioId = usuarioId == null ? "" : usuarioId.trim();
        createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}

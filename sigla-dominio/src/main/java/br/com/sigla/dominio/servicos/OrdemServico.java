package br.com.sigla.dominio.servicos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public record OrdemServico(
        String id,
        Long numeroOs,
        String clienteId,
        String titulo,
        String descricao,
        String tipoServico,
        OrdemServicoStatus status,
        LocalDateTime dataAgendada,
        LocalDateTime dataInicio,
        LocalDateTime dataFim,
        String responsavelInternoId,
        String executadoPorId,
        boolean foiFeito,
        boolean pago,
        BigDecimal valorServico,
        String observacoes
) {
    public OrdemServico {
        id = requireText(id, "id");
        clienteId = requireText(clienteId, "clienteId");
        titulo = requireText(titulo, "titulo");
        descricao = normalizeOptional(descricao);
        tipoServico = requireText(tipoServico, "tipoServico");
        status = Objects.requireNonNullElse(status, OrdemServicoStatus.AGENDADA);
        responsavelInternoId = normalizeOptional(responsavelInternoId);
        executadoPorId = normalizeOptional(executadoPorId);
        valorServico = valorServico == null ? BigDecimal.ZERO : valorServico;
        if (valorServico.signum() < 0) {
            throw new IllegalArgumentException("valorServico must not be negative");
        }
        observacoes = normalizeOptional(observacoes);
    }

    public enum OrdemServicoStatus {
        AGENDADA,
        ABERTA,
        EM_ANDAMENTO,
        CONCLUIDA,
        CANCELADA,
        ATRASADA
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }
}

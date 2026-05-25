package br.com.sigla.dominio.financeiro;

import java.util.Objects;

public record FormaPagamentoFinanceira(
        String id,
        String nome,
        boolean ativo
) {
    public FormaPagamentoFinanceira {
        id = requireText(id, "id");
        nome = requireText(nome, "nome");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}

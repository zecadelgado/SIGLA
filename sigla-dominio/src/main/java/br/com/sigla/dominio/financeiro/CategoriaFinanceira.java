package br.com.sigla.dominio.financeiro;

import java.util.Objects;

public record CategoriaFinanceira(
        String id,
        String tipo,
        String nome,
        boolean ativo
) {
    public CategoriaFinanceira {
        id = requireText(id, "id");
        tipo = requireText(tipo, "tipo");
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

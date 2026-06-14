package br.com.sigla.dominio.usuarios;

import java.util.Objects;

public record Usuario(
        String id,
        String nome,
        String usuario,
        String email,
        String senhaHash,
        TipoUsuario tipo,
        boolean ativo
) {
    public Usuario {
        id = normalizeOptional(id);
        nome = requireText(nome, "nome");
        usuario = requireText(usuario, "usuario");
        email = normalizeOptional(email);
        senhaHash = requireText(senhaHash, "senhaHash");
        tipo = Objects.requireNonNullElse(tipo, TipoUsuario.OPERADOR);
    }

    public enum TipoUsuario {
        ADMIN,
        OPERADOR,
        FINANCEIRO,
        TECNICO
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}

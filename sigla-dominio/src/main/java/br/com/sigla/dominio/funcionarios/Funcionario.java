package br.com.sigla.dominio.funcionarios;

import java.util.Objects;

public record Funcionario(
        String id,
        String name,
        String role,
        String contact,
        FuncionarioStatus status
) {
    public Funcionario {
        id = requireText(id, "id");
        name = requireText(name, "name");
        role = requireText(role, "role");
        contact = requireText(contact, "contact");
        status = Objects.requireNonNull(status, "status is required");
    }

    public enum FuncionarioStatus {
        ACTIVE,
        INACTIVE,
        ON_LEAVE
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}


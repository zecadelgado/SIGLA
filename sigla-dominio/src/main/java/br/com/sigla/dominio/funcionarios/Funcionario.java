package br.com.sigla.dominio.funcionarios;

import java.util.Objects;

public record Funcionario(
        String id,
        String name,
        String cpf,
        String role,
        String telefone,
        String email,
        String cep,
        String rua,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado,
        FuncionarioStatus status
) {
    public Funcionario {
        id = requireText(id, "id");
        name = requireText(name, "name");
        role = requireText(role, "role");
        cpf = normalizeOptional(cpf);
        telefone = normalizeOptional(telefone);
        email = normalizeOptional(email);
        cep = normalizeOptional(cep);
        rua = normalizeOptional(rua);
        numero = normalizeOptional(numero);
        complemento = normalizeOptional(complemento);
        bairro = normalizeOptional(bairro);
        cidade = normalizeOptional(cidade);
        estado = normalizeOptional(estado);
        status = Objects.requireNonNull(status, "status is required");
        if (telefone.isBlank() && email.isBlank()) {
            throw new IllegalArgumentException("Informe telefone ou e-mail do funcionario.");
        }
    }

    /** Contato resumido para exibicao (telefone, com fallback para e-mail). */
    public String contato() {
        return telefone.isBlank() ? email : telefone;
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

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }
}

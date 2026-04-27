package br.com.sigla.dominio.clientes;

import java.util.List;
import java.util.Objects;

public record Cliente(
        String id,
        String name,
        String razaoSocial,
        String nomeFantasia,
        String cpf,
        String cnpj,
        String phone,
        String email,
        String cep,
        String rua,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado,
        List<ContactPerson> contacts,
        String notes,
        boolean ativo
) {
    public Cliente {
        id = requireText(id, "id");
        razaoSocial = normalize(razaoSocial);
        nomeFantasia = normalize(nomeFantasia);
        name = normalize(name);
        if (name.isBlank()) {
            name = !nomeFantasia.isBlank() ? nomeFantasia : razaoSocial;
        }
        name = requireText(name, "name");
        cpf = normalize(cpf);
        cnpj = normalize(cnpj);
        phone = normalize(phone);
        email = normalize(email);
        cep = normalize(cep);
        rua = normalize(rua);
        numero = normalize(numero);
        complemento = normalize(complemento);
        bairro = normalize(bairro);
        cidade = normalize(cidade);
        estado = normalize(estado);
        contacts = List.copyOf(Objects.requireNonNullElse(contacts, List.of()));
        notes = normalize(notes);
    }

    public Cliente(
            String id,
            String name,
            String location,
            String cnpj,
            String phone,
            List<ContactPerson> contacts,
            String notes
    ) {
        this(
                id,
                name,
                name,
                name,
                "",
                cnpj,
                phone,
                "",
                extractLocationPart(location, 6),
                extractLocationPart(location, 0),
                extractLocationPart(location, 1),
                extractLocationPart(location, 2),
                extractLocationPart(location, 3),
                extractLocationPart(location, 4),
                extractLocationPart(location, 5),
                contacts,
                notes,
                true
        );
    }

    public String location() {
        return String.join(" - ", List.of(rua, numero, complemento, bairro, cidade, estado, cep).stream()
                .filter(value -> value != null && !value.isBlank())
                .toList());
    }

    public record ContactPerson(
            String name,
            String role,
            String contact
    ) {
        public ContactPerson {
            name = requireText(name, "name");
            role = normalize(role);
            contact = normalize(contact);
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }

    private static String extractLocationPart(String location, int index) {
        if (location == null || location.isBlank()) {
            return "";
        }
        String[] parts = location.split(" - ");
        return parts.length > index ? parts[index] : "";
    }
}

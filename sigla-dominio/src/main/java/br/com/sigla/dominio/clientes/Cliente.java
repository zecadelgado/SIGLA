package br.com.sigla.dominio.clientes;

import java.util.List;
import java.util.Objects;

public record Cliente(
        String id,
        String name,
        String location,
        String cnpj,
        String phone,
        List<ContactPerson> contacts,
        String notes
) {
    public Cliente {
        id = requireText(id, "id");
        name = requireText(name, "name");
        location = requireText(location, "location");
        cnpj = requireText(cnpj, "cnpj");
        phone = requireText(phone, "phone");
        contacts = List.copyOf(Objects.requireNonNullElse(contacts, List.of()));
        notes = normalize(notes);
    }

    public record ContactPerson(
            String name,
            String role,
            String contact
    ) {
        public ContactPerson {
            name = requireText(name, "name");
            role = requireText(role, "role");
            contact = requireText(contact, "contact");
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
}


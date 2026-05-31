package br.com.sigla.dominio.clientes;

import java.util.List;
import java.util.Objects;

public record Cliente(
        String id,
        TipoCliente tipo,
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
        tipo = Objects.requireNonNullElse(tipo, TipoCliente.PESSOA_FISICA);
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
        this(
                id,
                cnpj == null || cnpj.isBlank() ? TipoCliente.PESSOA_FISICA : TipoCliente.PESSOA_JURIDICA,
                name,
                razaoSocial,
                nomeFantasia,
                cpf,
                cnpj,
                phone,
                email,
                cep,
                rua,
                numero,
                complemento,
                bairro,
                cidade,
                estado,
                contacts,
                notes,
                ativo
        );
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
                cnpj == null || cnpj.isBlank() ? TipoCliente.PESSOA_FISICA : TipoCliente.PESSOA_JURIDICA,
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

    public enum TipoCliente {
        PESSOA_FISICA,
        PESSOA_JURIDICA;

        public static TipoCliente from(String value) {
            if (value == null || value.isBlank()) {
                return PESSOA_FISICA;
            }
            String normalized = value.trim().toUpperCase().replace('-', '_');
            return switch (normalized) {
                case "PF", "FISICA", "PESSOA_FISICA", "CLIENTE_PF" -> PESSOA_FISICA;
                case "PJ", "JURIDICA", "PESSOA_JURIDICA", "CLIENTE_PJ", "CLIENTE" -> PESSOA_JURIDICA;
                default -> valueOf(normalized);
            };
        }
    }

    public String location() {
        return String.join(" - ", List.of(rua, numero, complemento, bairro, cidade, estado, cep).stream()
                .filter(value -> value != null && !value.isBlank())
                .toList());
    }

    public record ContactPerson(
            String id,
            String name,
            String role,
            String phone,
            String email,
            boolean principal
    ) {
        public ContactPerson {
            id = normalize(id);
            name = requireText(name, "name");
            role = normalize(role);
            phone = normalize(phone);
            email = normalize(email);
        }

        public ContactPerson(String name, String role, String contact) {
            this("", name, role, contact == null || contact.contains("@") ? "" : contact, contact != null && contact.contains("@") ? contact : "", false);
        }

        public String contact() {
            return phone.isBlank() ? email : phone;
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

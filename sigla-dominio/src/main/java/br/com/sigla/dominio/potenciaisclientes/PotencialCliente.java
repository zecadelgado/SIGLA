package br.com.sigla.dominio.potenciaisclientes;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record PotencialCliente(
        String id,
        String name,
        String contact,
        String origin,
        PotencialClienteStatus status,
        List<Interaction> interactionHistory
) {
    public PotencialCliente {
        id = requireText(id, "id");
        name = requireText(name, "name");
        contact = requireText(contact, "contact");
        origin = requireText(origin, "origin");
        status = Objects.requireNonNull(status, "status is required");
        interactionHistory = List.copyOf(Objects.requireNonNullElse(interactionHistory, List.of()));
    }

    public record Interaction(
            LocalDate interactionDate,
            String channel,
            String notes
    ) {
        public Interaction {
            interactionDate = Objects.requireNonNull(interactionDate, "interactionDate is required");
            channel = requireText(channel, "channel");
            notes = requireText(notes, "notes");
        }
    }

    public enum PotencialClienteStatus {
        NEW,
        CONTACTED,
        NEGOTIATING,
        WON,
        LOST
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}


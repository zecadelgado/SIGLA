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
        List<Interaction> interactionHistory,
        String clienteIndicadorId,
        LocalDate dataIndicacao,
        String observacoes
) {
    public PotencialCliente {
        id = requireText(id, "id");
        name = requireText(name, "name");
        contact = normalize(contact);
        origin = normalize(origin);
        status = Objects.requireNonNull(status, "status is required");
        interactionHistory = List.copyOf(Objects.requireNonNullElse(interactionHistory, List.of()));
        clienteIndicadorId = normalize(clienteIndicadorId);
        dataIndicacao = Objects.requireNonNullElse(dataIndicacao, LocalDate.now());
        observacoes = normalize(observacoes);
    }

    public PotencialCliente(
            String id,
            String name,
            String contact,
            String origin,
            PotencialClienteStatus status,
            List<Interaction> interactionHistory
    ) {
        this(
                id,
                name,
                contact,
                origin,
                status,
                interactionHistory,
                extractCustomerId(origin),
                interactionHistory == null || interactionHistory.isEmpty() ? LocalDate.now() : interactionHistory.getFirst().interactionDate(),
                interactionHistory == null || interactionHistory.isEmpty() ? "" : interactionHistory.getFirst().notes()
        );
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
        NOVO,
        CONTATADO,
        AGUARDANDO_RETORNO,
        CONVERTIDO,
        PERDIDO,
        CANCELADO,
        NEW,
        CONTACTED,
        NEGOTIATING,
        WON,
        LOST;

        public boolean isConvertido() {
            return this == CONVERTIDO || this == WON;
        }

        public boolean isEncerrado() {
            return isConvertido() || this == PERDIDO || this == CANCELADO || this == LOST;
        }

        public static PotencialClienteStatus normalizar(PotencialClienteStatus status) {
            if (status == null) {
                return NOVO;
            }
            return switch (status) {
                case NEW -> NOVO;
                case CONTACTED -> CONTATADO;
                case NEGOTIATING -> AGUARDANDO_RETORNO;
                case WON -> CONVERTIDO;
                case LOST -> PERDIDO;
                default -> status;
            };
        }

        public static PotencialClienteStatus from(String value) {
            if (value == null || value.isBlank()) {
                return NOVO;
            }
            String normalized = value.trim().toUpperCase().replace('-', '_');
            return normalizar(valueOf(normalized));
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

    private static String extractCustomerId(String origin) {
        if (origin == null || !origin.contains(":")) {
            return "";
        }
        return origin.substring(origin.indexOf(':') + 1).trim();
    }
}

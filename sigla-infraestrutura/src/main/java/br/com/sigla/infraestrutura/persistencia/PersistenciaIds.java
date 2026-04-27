package br.com.sigla.infraestrutura.persistencia;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class PersistenciaIds {

    private PersistenciaIds() {
    }

    public static UUID toUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ignored) {
            return UUID.nameUUIDFromBytes(value.trim().getBytes(StandardCharsets.UTF_8));
        }
    }

    public static UUID toUuidIfValid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static String toString(UUID value) {
        return value == null ? "" : value.toString();
    }
}

package br.com.sigla.interfacegrafica.util;

import br.com.sigla.interfacegrafica.modelo.OpcaoId;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public final class ResolvedorEntradaTexto {

    private ResolvedorEntradaTexto() {
    }

    public static String limpar(String value) {
        return value == null ? "" : value.trim();
    }

    public static boolean isBlank(String value) {
        return limpar(value).isBlank();
    }

    public static boolean parseBoolean(String value) {
        String normalized = normalize(value);
        return normalized.equals("SIM")
                || normalized.equals("S")
                || normalized.equals("TRUE")
                || normalized.equals("VERDADEIRO")
                || normalized.equals("1")
                || normalized.equals("YES");
    }

    public static OpcaoId resolveObrigatoria(List<OpcaoId> options, String rawValue, String message) {
        OpcaoId option = resolveOpcional(options, rawValue);
        if (option == null) {
            throw new IllegalArgumentException(message);
        }
        return option;
    }

    public static OpcaoId resolveOpcional(List<OpcaoId> options, String rawValue) {
        String normalized = normalize(rawValue);
        if (normalized.isBlank()) {
            return null;
        }
        return options.stream()
                .filter(option -> matches(option, normalized))
                .findFirst()
                .orElse(null);
    }

    public static <E extends Enum<E>> E parseEnum(Class<E> type, String rawValue, E fallback) {
        String normalized = normalize(rawValue);
        if (normalized.isBlank()) {
            return fallback;
        }
        for (E constant : type.getEnumConstants()) {
            if (normalize(constant.name()).equals(normalized)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Valor invalido: " + rawValue);
    }

    private static boolean matches(OpcaoId option, String normalized) {
        String id = normalize(option.id());
        String label = normalize(option.label());
        return id.equals(normalized)
                || label.equals(normalized)
                || label.startsWith(normalized)
                || label.contains(normalized);
    }

    private static String normalize(String value) {
        String cleaned = limpar(value);
        if (cleaned.isBlank()) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(cleaned, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return withoutAccents
                .replace('-', ' ')
                .replace('/', ' ')
                .replaceAll("\\s+", "_")
                .toUpperCase(Locale.ROOT);
    }
}

package br.com.sigla.infraestrutura.persistencia;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serializa/deserializa o mapa de metadata (variaveis do template) para uma coluna de texto,
 * sem depender de biblioteca JSON. Formato: linhas {@code chave=valor}, com escape de
 * {@code \\}, {@code =} (na chave) e quebras de linha.
 */
public final class MetadataMapCodec {

    private MetadataMapCodec() {
    }

    public static String encode(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean primeiro = true;
        for (Map.Entry<String, String> entrada : metadata.entrySet()) {
            if (entrada.getKey() == null) {
                continue;
            }
            if (!primeiro) {
                sb.append('\n');
            }
            primeiro = false;
            sb.append(escape(entrada.getKey(), true));
            sb.append('=');
            sb.append(escape(entrada.getValue() == null ? "" : entrada.getValue(), false));
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    public static Map<String, String> decode(String texto) {
        Map<String, String> resultado = new LinkedHashMap<>();
        if (texto == null || texto.isBlank()) {
            return resultado;
        }
        for (String linha : texto.split("\n", -1)) {
            if (linha.isEmpty()) {
                continue;
            }
            int separador = indiceSeparador(linha);
            if (separador < 0) {
                continue;
            }
            String chave = unescape(linha.substring(0, separador));
            String valor = unescape(linha.substring(separador + 1));
            resultado.put(chave, valor);
        }
        return resultado;
    }

    private static int indiceSeparador(String linha) {
        boolean escapando = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (escapando) {
                escapando = false;
            } else if (c == '\\') {
                escapando = true;
            } else if (c == '=') {
                return i;
            }
        }
        return -1;
    }

    private static String escape(String valor, boolean ehChave) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '=' -> sb.append(ehChave ? "\\=" : "=");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String unescape(String valor) {
        StringBuilder sb = new StringBuilder();
        boolean escapando = false;
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (escapando) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case '\\' -> sb.append('\\');
                    case '=' -> sb.append('=');
                    default -> sb.append(c);
                }
                escapando = false;
            } else if (c == '\\') {
                escapando = true;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}

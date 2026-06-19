package br.com.sigla.dominio.notificacoes;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renderiza templates de mensagem substituindo marcadores no formato {@code {{variavel}}}
 * pelos valores informados. Variaveis ausentes sao substituidas por vazio.
 */
public final class RenderizadorTemplate {

    private static final Pattern MARCADOR = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    private RenderizadorTemplate() {
    }

    public static String renderizar(String template, Map<String, String> variaveis) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        Map<String, String> valores = variaveis == null ? Map.of() : variaveis;
        Matcher matcher = MARCADOR.matcher(template);
        StringBuilder resultado = new StringBuilder();
        while (matcher.find()) {
            String chave = matcher.group(1);
            String valor = valores.getOrDefault(chave, "");
            matcher.appendReplacement(resultado, Matcher.quoteReplacement(valor == null ? "" : valor));
        }
        matcher.appendTail(resultado);
        return resultado.toString();
    }
}

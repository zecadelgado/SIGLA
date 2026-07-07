package br.com.sigla.dominio.notificacoes;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Conjunto de antecedencias de lembrete (em dias) escolhidas num cadastro.
 * Substitui o antigo campo numerico unico pelas opcoes marcaveis {@link #PRESETS} (30/15/7/1).
 *
 * <p>Convencao de estados:
 * <ul>
 *   <li>{@code null}  = nao configurado (usa o fallback legado do campo int da entidade);</li>
 *   <li>lista vazia   = configurado sem nenhum dia (lembrete desligado);</li>
 *   <li>lista com dias = lembretes nessas antecedencias.</li>
 * </ul>
 * A serializacao para o banco segue a mesma convencao: {@code null} -> coluna nula,
 * lista vazia -> string vazia, senao CSV ordenado (ex.: {@code "30,15,7,1"}).
 */
public final class DiasLembrete {

    /** Opcoes oferecidas na interface, da maior para a menor antecedencia. */
    public static final List<Integer> PRESETS = List.of(30, 15, 7, 1);

    private DiasLembrete() {
    }

    /** Normaliza uma lista: remove nulos/nao-positivos, deduplica e ordena do maior para o menor. */
    public static List<Integer> normalizar(List<Integer> dias) {
        if (dias == null) {
            return null;
        }
        return dias.stream()
                .filter(d -> d != null && d > 0)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    /** CSV -> lista (respeitando a convencao null/vazio/valores). */
    public static List<Integer> parse(String csv) {
        if (csv == null) {
            return null;
        }
        if (csv.isBlank()) {
            return List.of();
        }
        return normalizar(Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(DiasLembrete::parseInteiro)
                .filter(d -> d != null)
                .toList());
    }

    /** Lista -> CSV (respeitando a convencao null/vazio/valores). */
    public static String formatar(List<Integer> dias) {
        if (dias == null) {
            return null;
        }
        List<Integer> normalizados = normalizar(dias);
        return normalizados.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static Integer parseInteiro(String valor) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException excecao) {
            return null;
        }
    }
}

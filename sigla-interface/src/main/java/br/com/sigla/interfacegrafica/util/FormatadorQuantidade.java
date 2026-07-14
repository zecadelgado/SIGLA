package br.com.sigla.interfacegrafica.util;

import java.math.BigDecimal;

/**
 * Apresentação e leitura de quantidades fracionadas de estoque (numeric(19,4)).
 * Exibe sem zeros à direita e com vírgula decimal; aceita vírgula ou ponto na
 * digitação.
 */
public final class FormatadorQuantidade {

    private FormatadorQuantidade() {
    }

    public static String formatar(BigDecimal quantidade) {
        if (quantidade == null) {
            return "0";
        }
        BigDecimal normalizada = quantidade.stripTrailingZeros();
        if (normalizada.scale() < 0) {
            normalizada = normalizada.setScale(0);
        }
        return normalizada.toPlainString().replace('.', ',');
    }

    /** Converte texto digitado em quantidade; lança NumberFormatException se inválido. */
    public static BigDecimal parse(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new NumberFormatException("quantidade vazia");
        }
        return new BigDecimal(texto.trim().replace(',', '.'));
    }
}

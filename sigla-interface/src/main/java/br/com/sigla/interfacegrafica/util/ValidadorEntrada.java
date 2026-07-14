package br.com.sigla.interfacegrafica.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Acumula erros de validação de formulário e os apresenta numa única mensagem,
 * listando cada campo/problema encontrado em vez de parar no primeiro.
 *
 * <p>Uso típico em um {@code onConfirmar}: extrair/validar cada campo pelos
 * métodos abaixo (que devolvem o valor e acumulam o erro quando inválido) e, ao
 * final, chamar {@link #validar()} — que lança {@link IllegalArgumentException}
 * com todos os problemas. Os controladores já exibem essa mensagem no
 * {@code feedbackLabel} ou em um {@code Alert}.
 */
public final class ValidadorEntrada {

    private final List<String> erros = new ArrayList<>();

    private ValidadorEntrada() {
    }

    public static ValidadorEntrada nova() {
        return new ValidadorEntrada();
    }

    /** Acumula uma mensagem de erro já formatada. */
    public ValidadorEntrada erro(String mensagem) {
        if (mensagem != null && !mensagem.isBlank()) {
            erros.add(mensagem.trim());
        }
        return this;
    }

    /** Acumula a mensagem quando a condição não é satisfeita. */
    public ValidadorEntrada exigir(boolean condicao, String mensagemSeInvalido) {
        if (!condicao) {
            erro(mensagemSeInvalido);
        }
        return this;
    }

    /** Exige texto preenchido. Retorna o texto sem espaços (ou "" quando ausente). */
    public String texto(String valor, String descricao) {
        if (valor == null || valor.isBlank()) {
            erro("Informe " + descricao + ".");
            return "";
        }
        return valor.trim();
    }

    /** Exige uma seleção (combo, data, item de tabela). Retorna o valor (ou null). */
    public <T> T selecao(T valor, String descricao) {
        if (valor == null) {
            erro("Selecione " + descricao + ".");
        }
        return valor;
    }

    /** Exige inteiro maior que zero. Retorna o valor (ou 0 quando inválido). */
    public int inteiroPositivo(String valor, String descricao) {
        Integer numero = parseInteiro(valor, descricao);
        if (numero == null) {
            return 0;
        }
        if (numero <= 0) {
            erro(maiuscula(descricao) + " deve ser maior que zero.");
            return 0;
        }
        return numero;
    }

    /** Exige inteiro maior ou igual a zero. Retorna o valor (ou 0 quando inválido). */
    public int inteiroNaoNegativo(String valor, String descricao) {
        Integer numero = parseInteiro(valor, descricao);
        if (numero == null) {
            return 0;
        }
        if (numero < 0) {
            erro(maiuscula(descricao) + " não pode ser negativo.");
            return 0;
        }
        return numero;
    }

    /** Exige quantidade (até 4 casas decimais) maior que zero. Retorna o valor (ou ZERO quando inválido). */
    public BigDecimal quantidadePositiva(String valor, String descricao) {
        BigDecimal quantidade = parseQuantidade(valor, descricao);
        if (quantidade == null) {
            return BigDecimal.ZERO;
        }
        if (quantidade.signum() <= 0) {
            erro(maiuscula(descricao) + " deve ser maior que zero.");
            return BigDecimal.ZERO;
        }
        return quantidade;
    }

    /** Exige quantidade (até 4 casas decimais) maior ou igual a zero. Retorna o valor (ou ZERO quando inválido). */
    public BigDecimal quantidadeNaoNegativa(String valor, String descricao) {
        BigDecimal quantidade = parseQuantidade(valor, descricao);
        if (quantidade == null) {
            return BigDecimal.ZERO;
        }
        if (quantidade.signum() < 0) {
            erro(maiuscula(descricao) + " não pode ser negativa.");
            return BigDecimal.ZERO;
        }
        return quantidade;
    }

    private BigDecimal parseQuantidade(String valor, String descricao) {
        if (valor == null || valor.isBlank()) {
            erro("Informe " + descricao + ".");
            return null;
        }
        try {
            BigDecimal quantidade = new BigDecimal(valor.trim().replace(',', '.'));
            if (quantidade.stripTrailingZeros().scale() > 4) {
                erro(maiuscula(descricao) + " aceita no máximo 4 casas decimais.");
                return null;
            }
            return quantidade;
        } catch (NumberFormatException excecao) {
            erro(maiuscula(descricao) + " deve ser um número válido (use vírgula para casas decimais).");
            return null;
        }
    }

    /** Exige valor monetário maior que zero. Retorna o valor (ou ZERO quando inválido). */
    public BigDecimal valorPositivo(BigDecimal valor, String descricao) {
        if (valor == null || valor.signum() <= 0) {
            erro("Informe " + descricao + " (maior que zero).");
            return BigDecimal.ZERO;
        }
        return valor;
    }

    public boolean temErros() {
        return !erros.isEmpty();
    }

    /** Lança {@link IllegalArgumentException} com todos os erros acumulados, se houver. */
    public void validar() {
        if (erros.isEmpty()) {
            return;
        }
        if (erros.size() == 1) {
            throw new IllegalArgumentException(erros.get(0));
        }
        StringBuilder mensagem = new StringBuilder("Corrija os campos abaixo:");
        for (String erro : erros) {
            mensagem.append("\n• ").append(erro);
        }
        throw new IllegalArgumentException(mensagem.toString());
    }

    private Integer parseInteiro(String valor, String descricao) {
        if (valor == null || valor.isBlank()) {
            erro("Informe " + descricao + ".");
            return null;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException excecao) {
            erro(maiuscula(descricao) + " deve ser um número inteiro válido.");
            return null;
        }
    }

    private static String maiuscula(String descricao) {
        String limpo = descricao == null ? "" : descricao.trim();
        if (limpo.isEmpty()) {
            return limpo;
        }
        return Character.toUpperCase(limpo.charAt(0)) + limpo.substring(1);
    }
}

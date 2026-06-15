package br.com.sigla.relatorios.modelo;

import java.util.List;
import java.util.Objects;

/**
 * Modelo neutro de um documento a ser impresso (recibo, etiqueta, ordem de servico).
 * Os servicos de relatorio montam este modelo a partir dos dados de dominio e o
 * {@code GeradorPdfDocumento} o transforma num PDF real.
 */
public record DocumentoRelatorio(
        String titulo,
        String subtitulo,
        List<Campo> campos,
        List<String> itens,
        String rodape
) {
    public DocumentoRelatorio {
        titulo = titulo == null ? "" : titulo;
        subtitulo = subtitulo == null ? "" : subtitulo;
        campos = List.copyOf(Objects.requireNonNullElse(campos, List.of()));
        itens = List.copyOf(Objects.requireNonNullElse(itens, List.of()));
        rodape = rodape == null ? "" : rodape;
    }

    public record Campo(String rotulo, String valor) {
        public Campo {
            rotulo = rotulo == null ? "" : rotulo;
            valor = valor == null ? "" : valor;
        }
    }
}

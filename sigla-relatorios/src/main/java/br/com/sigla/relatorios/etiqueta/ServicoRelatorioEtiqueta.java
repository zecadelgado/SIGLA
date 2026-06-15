package br.com.sigla.relatorios.etiqueta;

import br.com.sigla.relatorios.impressao.DespachanteImpressao;
import br.com.sigla.relatorios.modelo.DocumentoRelatorio;
import br.com.sigla.relatorios.pdf.GeradorPdfDocumento;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

/**
 * Gera a etiqueta de um produto de estoque em PDF e a envia para impressao.
 */
@Service
public class ServicoRelatorioEtiqueta {

    private final GeradorPdfDocumento gerador;
    private final DespachanteImpressao despachante;

    public ServicoRelatorioEtiqueta(GeradorPdfDocumento gerador, DespachanteImpressao despachante) {
        this.gerador = gerador;
        this.despachante = despachante;
    }

    public byte[] gerar(DadosEtiqueta dados) {
        return gerador.gerar(montar(dados));
    }

    public Path imprimir(DadosEtiqueta dados) {
        return despachante.imprimir("etiqueta-" + dados.codigo(), gerar(dados));
    }

    private DocumentoRelatorio montar(DadosEtiqueta dados) {
        List<DocumentoRelatorio.Campo> campos = List.of(
                new DocumentoRelatorio.Campo("Produto", dados.nome()),
                new DocumentoRelatorio.Campo("Codigo", dados.codigo()),
                new DocumentoRelatorio.Campo("Unidade", dados.unidade()),
                new DocumentoRelatorio.Campo("Preco de venda", dados.preco())
        );
        return new DocumentoRelatorio("ETIQUETA", dados.nome(), campos, List.of(), "");
    }

    public record DadosEtiqueta(
            String codigo,
            String nome,
            String unidade,
            String preco
    ) {
    }
}

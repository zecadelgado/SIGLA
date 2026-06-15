package br.com.sigla.relatorios.ordemservico;

import br.com.sigla.relatorios.impressao.DespachanteImpressao;
import br.com.sigla.relatorios.modelo.DocumentoRelatorio;
import br.com.sigla.relatorios.pdf.GeradorPdfDocumento;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Gera o documento de uma ordem de servico em PDF e o envia para impressao.
 */
@Service
public class ServicoRelatorioOrdemServico {

    private final GeradorPdfDocumento gerador;
    private final DespachanteImpressao despachante;

    public ServicoRelatorioOrdemServico(GeradorPdfDocumento gerador, DespachanteImpressao despachante) {
        this.gerador = gerador;
        this.despachante = despachante;
    }

    public byte[] gerar(DadosOrdemServico dados) {
        return gerador.gerar(montar(dados));
    }

    public Path imprimir(DadosOrdemServico dados) {
        return despachante.imprimir("ordem-servico-" + dados.numero(), gerar(dados));
    }

    private DocumentoRelatorio montar(DadosOrdemServico dados) {
        List<DocumentoRelatorio.Campo> campos = new ArrayList<>();
        campos.add(new DocumentoRelatorio.Campo("Cliente", dados.cliente()));
        campos.add(new DocumentoRelatorio.Campo("Titulo", dados.titulo()));
        campos.add(new DocumentoRelatorio.Campo("Descricao", dados.descricao()));
        campos.add(new DocumentoRelatorio.Campo("Tipo de servico", dados.tipoServico()));
        campos.add(new DocumentoRelatorio.Campo("Responsavel", dados.responsavel()));
        campos.add(new DocumentoRelatorio.Campo("Data", dados.data()));
        campos.add(new DocumentoRelatorio.Campo("Situacao", dados.situacao()));
        campos.add(new DocumentoRelatorio.Campo("Valor total", dados.valor()));
        if (dados.observacoes() != null && !dados.observacoes().isBlank()) {
            campos.add(new DocumentoRelatorio.Campo("Observacoes", dados.observacoes()));
        }
        List<String> itens = dados.itens() == null ? List.of() : dados.itens();
        String rodape = "Emitido em " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return new DocumentoRelatorio("ORDEM DE SERVICO", "No " + dados.numero(), campos, itens, rodape);
    }

    public record DadosOrdemServico(
            String numero,
            String cliente,
            String titulo,
            String descricao,
            String tipoServico,
            String responsavel,
            String data,
            String situacao,
            String valor,
            List<String> itens,
            String observacoes
    ) {
    }
}

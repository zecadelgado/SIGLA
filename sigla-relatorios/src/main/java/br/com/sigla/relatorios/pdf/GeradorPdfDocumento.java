package br.com.sigla.relatorios.pdf;

import br.com.sigla.relatorios.modelo.DocumentoRelatorio;
import br.com.sigla.relatorios.modelo.ProvedorModeloRelatorio;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Renderiza um {@link DocumentoRelatorio} em PDF (A4) usando Apache PDFBox.
 * Layout simples e linear: cabecalho da empresa, titulo, subtitulo, lista de
 * campos rotulo/valor, lista de itens e rodape.
 */
@Component
public class GeradorPdfDocumento {

    private static final float MARGEM = 50f;
    private static final float TOPO = PDRectangle.A4.getHeight() - MARGEM;
    private static final float LARGURA_UTIL = PDRectangle.A4.getWidth() - (2 * MARGEM);

    private final ProvedorModeloRelatorio modelo;

    public GeradorPdfDocumento(ProvedorModeloRelatorio modelo) {
        this.modelo = modelo;
    }

    public byte[] gerar(DocumentoRelatorio documento) {
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            pdf.addPage(pagina);

            PDType1Font fonteNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fonteNegrito = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            try (PDPageContentStream conteudo = new PDPageContentStream(pdf, pagina)) {
                float y = TOPO;
                y = escrever(conteudo, fonteNegrito, 10, modelo.cabecalho(), y);
                y -= 14;
                y = escrever(conteudo, fonteNegrito, 18, documento.titulo(), y);
                if (!documento.subtitulo().isBlank()) {
                    y -= 2;
                    y = escrever(conteudo, fonteNormal, 11, documento.subtitulo(), y);
                }
                y -= 8;
                y = linha(conteudo, y);
                y -= 16;

                for (DocumentoRelatorio.Campo campo : documento.campos()) {
                    y = escrever(conteudo, fonteNegrito, 11, campo.rotulo() + ":", y);
                    y = escreverMultilinha(conteudo, fonteNormal, 11, campo.valor(), y);
                    y -= 6;
                }

                if (!documento.itens().isEmpty()) {
                    y -= 6;
                    y = escrever(conteudo, fonteNegrito, 11, "Itens", y);
                    y -= 2;
                    for (String item : documento.itens()) {
                        y = escreverMultilinha(conteudo, fonteNormal, 11, "- " + item, y);
                    }
                }

                if (!documento.rodape().isBlank()) {
                    escrever(conteudo, fonteNormal, 9, documento.rodape(), MARGEM + 10);
                }
            }

            pdf.save(saida);
            return saida.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException("Falha ao gerar PDF do relatorio", exception);
        }
    }

    private float escrever(PDPageContentStream conteudo, PDType1Font fonte, float tamanho, String texto, float y) throws IOException {
        conteudo.beginText();
        conteudo.setFont(fonte, tamanho);
        conteudo.newLineAtOffset(MARGEM, y);
        conteudo.showText(sanitizar(texto));
        conteudo.endText();
        return y - (tamanho + 4);
    }

    private float escreverMultilinha(PDPageContentStream conteudo, PDType1Font fonte, float tamanho, String texto, float y) throws IOException {
        float novaY = y;
        for (String linha : quebrar(sanitizar(texto), fonte, tamanho)) {
            novaY = escrever(conteudo, fonte, tamanho, linha, novaY);
        }
        return novaY;
    }

    private float linha(PDPageContentStream conteudo, float y) throws IOException {
        conteudo.moveTo(MARGEM, y);
        conteudo.lineTo(MARGEM + LARGURA_UTIL, y);
        conteudo.stroke();
        return y;
    }

    /** Quebra o texto em linhas que cabem na largura util (aproximacao por largura da fonte). */
    private java.util.List<String> quebrar(String texto, PDType1Font fonte, float tamanho) throws IOException {
        java.util.List<String> linhas = new java.util.ArrayList<>();
        for (String paragrafo : texto.split("\n", -1)) {
            StringBuilder atual = new StringBuilder();
            for (String palavra : paragrafo.split(" ")) {
                String candidato = atual.isEmpty() ? palavra : atual + " " + palavra;
                float largura = fonte.getStringWidth(candidato) / 1000 * tamanho;
                if (largura > LARGURA_UTIL && !atual.isEmpty()) {
                    linhas.add(atual.toString());
                    atual = new StringBuilder(palavra);
                } else {
                    atual = new StringBuilder(candidato);
                }
            }
            linhas.add(atual.toString());
        }
        return linhas;
    }

    /** PDFBox/Helvetica usa WinAnsi; troca caracteres fora do Latin-1 para evitar excecao. */
    private String sanitizar(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder seguro = new StringBuilder(texto.length());
        for (char caractere : texto.toCharArray()) {
            if (caractere == '\n' || (caractere >= 0x20 && caractere <= 0xFF)) {
                seguro.append(caractere);
            } else {
                seguro.append('?');
            }
        }
        return seguro.toString();
    }
}

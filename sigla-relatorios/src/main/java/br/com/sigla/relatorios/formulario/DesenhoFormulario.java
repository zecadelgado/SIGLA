package br.com.sigla.relatorios.formulario;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Primitivas de desenho sobre uma pagina PDF (PDFBox), com coordenadas medidas a
 * partir do TOPO da pagina (mais natural para formularios). Usado pelos formularios
 * LIDER (Ordem de Servico e Relatorio de Visita).
 */
final class DesenhoFormulario implements AutoCloseable {

    static final Color CINZA_BARRA = new Color(0xD9, 0xD9, 0xD9);
    static final Color CINZA_LINHA = new Color(0x55, 0x55, 0x55);

    private final PDDocument documento;
    private final PDPageContentStream conteudo;
    private final float largura;
    private final float altura;
    private final PDType1Font fonteNormal;
    private final PDType1Font fonteNegrito;

    DesenhoFormulario(PDRectangle tamanho) {
        try {
            this.documento = new PDDocument();
            PDPage pagina = new PDPage(tamanho);
            documento.addPage(pagina);
            this.largura = tamanho.getWidth();
            this.altura = tamanho.getHeight();
            this.conteudo = new PDPageContentStream(documento, pagina);
            this.fonteNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            this.fonteNegrito = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        } catch (IOException excecao) {
            throw new UncheckedIOException("Falha ao iniciar o PDF do formulario", excecao);
        }
    }

    float largura() {
        return largura;
    }

    private float y(float topo) {
        return altura - topo;
    }

    void texto(float x, float topo, float tamanho, boolean negrito, String valor) {
        texto(x, topo, tamanho, negrito, Color.BLACK, valor);
    }

    void texto(float x, float topo, float tamanho, boolean negrito, Color cor, String valor) {
        if (valor == null || valor.isEmpty()) {
            return;
        }
        PDType1Font fonte = negrito ? fonteNegrito : fonteNormal;
        try {
            conteudo.beginText();
            conteudo.setNonStrokingColor(cor);
            conteudo.setFont(fonte, tamanho);
            conteudo.newLineAtOffset(x, y(topo + tamanho));
            conteudo.showText(sanitizar(valor));
            conteudo.endText();
            conteudo.setNonStrokingColor(Color.BLACK);
        } catch (IOException excecao) {
            throw new UncheckedIOException("Falha ao escrever texto no PDF", excecao);
        }
    }

    void textoCentralizado(float centroX, float topo, float tamanho, boolean negrito, String valor) {
        float larguraTexto = larguraDoTexto(valor, tamanho, negrito);
        texto(centroX - larguraTexto / 2, topo, tamanho, negrito, valor);
    }

    float larguraDoTexto(String valor, float tamanho, boolean negrito) {
        if (valor == null || valor.isEmpty()) {
            return 0;
        }
        PDType1Font fonte = negrito ? fonteNegrito : fonteNormal;
        try {
            return fonte.getStringWidth(sanitizar(valor)) / 1000 * tamanho;
        } catch (IOException excecao) {
            return 0;
        }
    }

    void linha(float x1, float topo1, float x2, float topo2) {
        try {
            conteudo.setStrokingColor(Color.BLACK);
            conteudo.setLineWidth(0.7f);
            conteudo.moveTo(x1, y(topo1));
            conteudo.lineTo(x2, y(topo2));
            conteudo.stroke();
        } catch (IOException excecao) {
            throw new UncheckedIOException("Falha ao desenhar linha no PDF", excecao);
        }
    }

    void retangulo(float x, float topo, float larguraRet, float alturaRet) {
        try {
            conteudo.setStrokingColor(Color.BLACK);
            conteudo.setLineWidth(0.7f);
            conteudo.addRect(x, y(topo + alturaRet), larguraRet, alturaRet);
            conteudo.stroke();
        } catch (IOException excecao) {
            throw new UncheckedIOException("Falha ao desenhar retangulo no PDF", excecao);
        }
    }

    void preencher(float x, float topo, float larguraRet, float alturaRet, Color cor) {
        try {
            conteudo.setNonStrokingColor(cor);
            conteudo.addRect(x, y(topo + alturaRet), larguraRet, alturaRet);
            conteudo.fill();
            conteudo.setNonStrokingColor(Color.BLACK);
        } catch (IOException excecao) {
            throw new UncheckedIOException("Falha ao preencher retangulo no PDF", excecao);
        }
    }

    /** Barra de secao: faixa cinza com titulo centralizado em negrito. */
    void barraSecao(float x, float topo, float larguraRet, float alturaRet, String titulo) {
        preencher(x, topo, larguraRet, alturaRet, CINZA_BARRA);
        retangulo(x, topo, larguraRet, alturaRet);
        textoCentralizado(x + larguraRet / 2, topo + (alturaRet - 9) / 2, 9, true, titulo);
    }

    /** Quadrado de checkbox (vazio) seguido do rotulo. Devolve o x final apos o rotulo. */
    float checkbox(float x, float topo, String rotulo, float tamanho) {
        float lado = tamanho;
        retangulo(x, topo, lado, lado);
        float xTexto = x + lado + 4;
        texto(xTexto, topo + (lado - tamanho) / 2 + 0.5f, tamanho, false, rotulo);
        return xTexto + larguraDoTexto(rotulo, tamanho, false);
    }

    /** Rotulo em negrito + valor, com linha de base ao longo da largura. */
    void campoLinha(float x, float topo, float larguraCampo, String rotulo, String valor, float tamanho) {
        texto(x, topo, tamanho, true, rotulo);
        float xValor = x + larguraDoTexto(rotulo + " ", tamanho, true) + 2;
        texto(xValor, topo, tamanho, false, valor);
        float baseTopo = topo + tamanho + 2;
        linha(x, baseTopo, x + larguraCampo, baseTopo);
    }

    private String sanitizar(String valor) {
        StringBuilder seguro = new StringBuilder(valor.length());
        for (char caractere : valor.toCharArray()) {
            seguro.append(caractere >= 0x20 && caractere <= 0xFF ? caractere : '?');
        }
        return seguro.toString();
    }

    byte[] finalizar() {
        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            conteudo.close();
            documento.save(saida);
            return saida.toByteArray();
        } catch (IOException excecao) {
            throw new UncheckedIOException("Falha ao finalizar o PDF do formulario", excecao);
        } finally {
            try {
                documento.close();
            } catch (IOException ignorado) {
                // documento ja fechado
            }
        }
    }

    @Override
    public void close() {
        try {
            documento.close();
        } catch (IOException ignorado) {
            // nada a fazer
        }
    }
}

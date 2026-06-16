package br.com.sigla.relatorios.formulario;

import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.awt.Color;

/**
 * Desenha a Ordem de Servico no formato do formulario LIDER (paisagem):
 * cabecalho, grade de campos, tabela de 5 colunas (servicos/produtos) com
 * checkboxes e rodape (etapa/de, observacoes, assinatura).
 */
public final class FormularioOrdemServico {

    private static final float MARGEM = 24f;

    private FormularioOrdemServico() {
    }

    public static byte[] gerar(Dados dados, DadosEmpresa empresa) {
        PDRectangle paisagem = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
        DesenhoFormulario d = new DesenhoFormulario(paisagem);
        float dir = d.largura() - MARGEM;

        desenharCabecalho(d, empresa, dir);
        desenharCampos(d, dados, dir);
        desenharTabela(d, dir);
        desenharRodape(d, dados, dir);

        return d.finalizar();
    }

    private static void desenharCabecalho(DesenhoFormulario d, DadosEmpresa empresa, float dir) {
        // Logo (placeholder): elipse escura.
        d.preencher(MARGEM, 26f, 58f, 48f, new Color(0x1b, 0x2a, 0x4a));
        d.texto(MARGEM + 8, 44f, 10f, true, Color.WHITE, "LIDER");
        // Nome e subtitulo.
        d.texto(95f, 28f, 40f, true, empresa.nome());
        d.texto(290f, 34f, 12f, true, empresa.subtitulo());
        d.texto(290f, 50f, 9f, false, empresa.cnpj() + "  -  " + empresa.telefones());
        d.linha(MARGEM, 84f, dir, 84f);
    }

    private static void desenharCampos(DesenhoFormulario d, Dados dados, float dir) {
        float topo = 92f;
        float alturaLinha = 21f;
        float divisor = 430f;
        d.retangulo(MARGEM, topo, dir - MARGEM, alturaLinha * 4);
        for (int i = 1; i < 4; i++) {
            d.linha(MARGEM, topo + alturaLinha * i, dir, topo + alturaLinha * i);
        }
        d.linha(divisor, topo, divisor, topo + alturaLinha * 4);
        // Divisores da primeira linha (MANHA / TARDE).
        d.linha(170f, topo, 170f, topo + alturaLinha);
        d.linha(300f, topo, 300f, topo + alturaLinha);

        float ty = topo + 6f;
        campo(d, MARGEM + 5, ty, "DATA:", dados.data());
        campo(d, 176f, ty, "MANHA:", "");
        campo(d, 306f, ty, "TARDE:", "");
        campo(d, divisor + 6, ty, "FUNCIONARIO:", dados.funcionario());

        campo(d, MARGEM + 5, ty + alturaLinha, "CLIENTE:", dados.cliente());
        campo(d, divisor + 6, ty + alturaLinha, "INICIO:", dados.inicio());

        campo(d, MARGEM + 5, ty + alturaLinha * 2, "CNPJ:", dados.cnpj());
        campo(d, divisor + 6, ty + alturaLinha * 2, "TERMINO:", dados.termino());

        campo(d, MARGEM + 5, ty + alturaLinha * 3, "EMAIL:", dados.email());
        campo(d, divisor + 6, ty + alturaLinha * 3, "TELEFONE:", dados.telefone());
    }

    private static void campo(DesenhoFormulario d, float x, float topo, String rotulo, String valor) {
        d.texto(x, topo, 9f, true, rotulo);
        float xValor = x + d.larguraDoTexto(rotulo + "  ", 9f, true);
        d.texto(xValor, topo, 9f, false, valor == null ? "" : valor);
    }

    private static void desenharTabela(DesenhoFormulario d, float dir) {
        float topo = 188f;
        float alturaCabecalho = 18f;
        float alturaCorpo = 300f;
        float[] limites = {MARGEM, 200f, 380f, 560f, 720f, dir};
        String[] titulos = {
                "SERVICO APLICACAO GERAL", "PRODUTO UTILIZADO",
                "SERVICO DE MANUTENCAO", "PRODUTO", "QTDE"
        };

        // Cabecalho cinza.
        d.preencher(MARGEM, topo, dir - MARGEM, alturaCabecalho, DesenhoFormulario.CINZA_BARRA);
        for (int i = 0; i < titulos.length; i++) {
            d.textoCentralizado((limites[i] + limites[i + 1]) / 2, topo + 5f, 8f, true, titulos[i]);
        }
        // Bordas da tabela.
        d.retangulo(MARGEM, topo, dir - MARGEM, alturaCabecalho + alturaCorpo);
        d.linha(MARGEM, topo + alturaCabecalho, dir, topo + alturaCabecalho);
        for (int i = 1; i < limites.length - 1; i++) {
            d.linha(limites[i], topo, limites[i], topo + alturaCabecalho + alturaCorpo);
        }

        float corpoTopo = topo + alturaCabecalho + 14f;

        // Coluna 1: aplicacao geral.
        String[] aplicacao = {"DESINSETIZACAO", "DESRATIZACAO", "DESCUPINIZACAO", "DES. MORCEGO", "LIMP. CX D'AGUA"};
        float y1 = corpoTopo;
        for (String item : aplicacao) {
            d.checkbox(limites[0] + 8, y1, item, 9f);
            y1 += 28f;
        }

        // Coluna 2: produto utilizado (texto com lacunas).
        float y2 = corpoTopo;
        d.texto(limites[1] + 8, y2, 8.5f, false, "Quantidade do produto ________");
        d.texto(limites[1] + 8, y2 + 14, 8.5f, false, "diluido em ________ de calda.");
        d.texto(limites[1] + 8, y2 + 60, 8.5f, true, "PRODUTO UTILIZADO:");
        d.texto(limites[1] + 8, y2 + 76, 8.5f, false, "Quantidade do produto ________");
        d.texto(limites[1] + 8, y2 + 90, 8.5f, false, "diluido em ________ de calda.");

        // Coluna 3: servico de manutencao.
        String[] manutencao = {"Desinsetizacao", "Pulverizacao geral", "Desratizacao", "Implementacao de", "para roedores"};
        float y3 = corpoTopo;
        for (int i = 0; i < manutencao.length; i++) {
            if (i == 4) {
                d.texto(limites[2] + 24, y3, 9f, false, manutencao[i]);
            } else {
                d.checkbox(limites[2] + 8, y3, manutencao[i], 9f);
            }
            y3 += 28f;
        }

        // Coluna 4: produtos.
        String[] produtos = {"Bloco", "Milho", "Girassol", "Aveia", "Pastilhas", "Po de contato", "Isca cola", "Gel B.F"};
        float y4 = corpoTopo;
        for (String item : produtos) {
            d.checkbox(limites[3] + 8, y4, item, 9f);
            // Linha de QTDE alinhada.
            d.linha(limites[4] + 10, y4 + 11, limites[5] - 10, y4 + 11);
            y4 += 27f;
        }
    }

    private static void desenharRodape(DesenhoFormulario d, Dados dados, float dir) {
        float topo = 506f;
        // Etapa / De.
        d.retangulo(MARGEM, topo, dir - MARGEM, 20f);
        d.linha(430f, topo, 430f, topo + 20f);
        d.texto(MARGEM + 5, topo + 6, 9f, true, "ETAPA");
        d.texto(436f, topo + 6, 9f, true, "DE");

        // Observacoes.
        float obsTopo = topo + 24f;
        d.texto(MARGEM, obsTopo, 9f, true, "OBS.:");
        d.texto(MARGEM + 34, obsTopo, 9f, false, dados.observacoes() == null ? "" : dados.observacoes());
        d.linha(MARGEM, obsTopo + 14, dir, obsTopo + 14);

        // Assinatura.
        float assTopo = obsTopo + 26f;
        d.texto(MARGEM, assTopo, 9f, true, "Ass. Responsavel:");
        d.linha(MARGEM + 95, assTopo + 11, dir, assTopo + 11);
    }

    public record Dados(
            String data,
            String funcionario,
            String cliente,
            String cnpj,
            String email,
            String telefone,
            String inicio,
            String termino,
            String observacoes
    ) {
    }
}

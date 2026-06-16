package br.com.sigla.relatorios.formulario;

import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.awt.Color;

/**
 * Desenha o Relatorio de Visita no formato do formulario LIDER (retrato):
 * cabecalho da empresa, bloco do cliente, secoes Tipo de Visita, Desratizacao,
 * Desinsetizacao, Componente Ativo (checkboxes) e tres blocos de assinatura.
 */
public final class FormularioVisita {

    private static final float MARGEM = 22f;

    private FormularioVisita() {
    }

    public static byte[] gerar(Dados dados, DadosEmpresa empresa) {
        DesenhoFormulario d = new DesenhoFormulario(PDRectangle.A4);
        float dir = d.largura() - MARGEM;

        cabecalho(d, empresa, dados, dir);
        tituloEHorarios(d, dados, dir);
        blocoCliente(d, dados, dir);
        tipoVisita(d, dir);
        desratizacao(d, dir);
        desinsetizacao(d, dir);
        componenteAtivo(d, dir);
        assinaturas(d, dados, dir);
        rodape(d, empresa, dir);

        return d.finalizar();
    }

    private static void cabecalho(DesenhoFormulario d, DadosEmpresa empresa, Dados dados, float dir) {
        d.preencher(MARGEM, 20f, 60f, 42f, new Color(0x1b, 0x2a, 0x4a));
        d.texto(MARGEM + 9, 36f, 9f, true, Color.WHITE, "LIDER");
        d.texto(90f, 20f, 22f, true, empresa.nome());
        d.texto(90f, 44f, 8f, true, empresa.subtitulo());
        d.texto(250f, 22f, 7.5f, true, empresa.razaoSocial());
        d.texto(250f, 32f, 7f, false, "CNPJ: " + empresa.cnpj());
        d.texto(250f, 41f, 7f, false, empresa.telefones());
        d.texto(250f, 50f, 7f, false, empresa.endereco());
        // Campos topo-direita.
        campoCaixa(d, 410f, 18f, dir - 410f, "Data:", dados.data());
        campoCaixa(d, 410f, 36f, dir - 410f, "Funcionario(s):", dados.funcionarios());
    }

    private static void tituloEHorarios(DesenhoFormulario d, Dados dados, float dir) {
        float topo = 66f;
        d.barraSecao(MARGEM, topo, 300f, 16f, "RELATORIO DE VISITA");
        campoCaixa(d, 340f, topo + 1, 110f, "Hora Inicio:", dados.horaInicio());
        campoCaixa(d, 460f, topo + 1, dir - 460f, "Hora Termino:", dados.horaTermino());
    }

    private static void blocoCliente(DesenhoFormulario d, Dados dados, float dir) {
        float topo = 90f;
        d.campoLinha(MARGEM, topo, 360f, "Cliente", dados.cliente(), 9f);
        d.campoLinha(400f, topo, dir - 400f, "CNPJ", dados.cnpj(), 9f);
        d.campoLinha(MARGEM, topo + 18, 360f, "Endereco", dados.endereco(), 9f);
        d.campoLinha(400f, topo + 18, dir - 400f, "Fone", dados.fone(), 9f);
        d.campoLinha(MARGEM, topo + 36, 200f, "Municipio", dados.municipio(), 9f);
        d.campoLinha(240f, topo + 36, 120f, "Estado", dados.estado(), 9f);
        d.campoLinha(380f, topo + 36, dir - 380f, "Responsavel", dados.responsavel(), 9f);
    }

    private static void tipoVisita(DesenhoFormulario d, float dir) {
        float topo = 150f;
        d.barraSecao(MARGEM, topo, dir - MARGEM, 15f, "TIPO DE VISITA E OBJETIVO");
        float y = topo + 22;
        String[] itens = {"Periodica", "Extraordinaria", "Prevencao", "Correcao", "Erradicacao"};
        float x = MARGEM + 10;
        float passo = (dir - MARGEM) / itens.length;
        for (String item : itens) {
            d.checkbox(x, y, item, 8.5f);
            x += passo;
        }
    }

    private static void desratizacao(DesenhoFormulario d, float dir) {
        float topo = 188f;
        d.barraSecao(MARGEM, topo, dir - MARGEM, 15f, "DESRATIZACAO");
        float y = topo + 22;
        d.checkbox(MARGEM + 6, y, "Externamente", 8.5f);
        d.checkbox(MARGEM + 6, y + 16, "Internamente", 8.5f);
        d.campoLinha(150f, y + 6, dir - 150f, "Locais", "", 9f);

        float yt = y + 36;
        d.texto(MARGEM + 6, yt, 8.5f, true, "Tecnica de Tratamento:");
        d.checkbox(170f, yt, "Granulacao", 8.5f);
        d.checkbox(310f, yt, "Armadilhas/Iscas Adesivas", 8.5f);
        d.checkbox(460f, yt, "Po de contato", 8.5f);
        d.checkbox(170f, yt + 16, "Polvilhamento", 8.5f);
        d.checkbox(310f, yt + 16, "Iscagem", 8.5f);
        d.checkbox(460f, yt + 16, "Bloco", 8.5f);

        float yp = yt + 34;
        d.texto(MARGEM + 6, yp, 8.5f, true, "Praga Alvo:");
        d.checkbox(170f, yp, "Camundongo (Mus musculus)", 8.5f);
        d.checkbox(330f, yp, "Rato (Rattus rattus)", 8.5f);
        d.checkbox(455f, yp, "Ratazana (R. norvegicus)", 8.5f);
        d.retangulo(MARGEM, topo, dir - MARGEM, 104f);
    }

    private static void desinsetizacao(DesenhoFormulario d, float dir) {
        float topo = 298f;
        d.barraSecao(MARGEM, topo, dir - MARGEM, 15f, "DESINSETIZACAO");
        float y = topo + 22;
        d.checkbox(MARGEM + 6, y, "Externamente", 8.5f);
        d.checkbox(MARGEM + 6, y + 16, "Internamente", 8.5f);
        d.campoLinha(150f, y + 6, dir - 150f, "Locais", "", 9f);

        float yt = y + 36;
        d.texto(MARGEM + 6, yt, 8.5f, true, "Tecnica de Tratamento:");
        d.checkbox(170f, yt, "Pulverizacao", 8.5f);
        d.checkbox(310f, yt, "Armadilhas/Iscas Adesivas", 8.5f);
        d.checkbox(460f, yt, "Termonebulizacao", 8.5f);
        d.checkbox(170f, yt + 16, "Polvilhamento", 8.5f);
        d.checkbox(310f, yt + 16, "Iscagem", 8.5f);
        d.checkbox(460f, yt + 16, "Bloco", 8.5f);

        float yp = yt + 34;
        d.texto(MARGEM + 6, yp, 8.5f, true, "Praga Alvo:");
        String[] linha1 = {"Barata", "Formiga", "Mosca", "Aranha", "Mosquito"};
        float x = 110f;
        for (String item : linha1) {
            d.checkbox(x, yp, item, 8.5f);
            x += 90f;
        }
        d.checkbox(110f, yp + 16, "Descupinizacao", 8.5f);
        d.checkbox(250f, yp + 16, "Limpeza Caixa de agua", 8.5f);
        d.checkbox(420f, yp + 16, "Limpeza Reservatorio", 8.5f);
        d.retangulo(MARGEM, topo, dir - MARGEM, 122f);
    }

    private static void componenteAtivo(DesenhoFormulario d, float dir) {
        float topo = 428f;
        d.barraSecao(MARGEM, topo, dir - MARGEM, 15f, "COMPONENTE ATIVO");
        float y = topo + 22;
        String[] col1 = {"Coumatetralil", "Cumaclor", "Brodifacoum", "Bromadiolona", "Diferacoum", "Nao Toxico (Adesivo)"};
        String[] col2 = {"Sulfuramida", "Acido Ortoborico", "Azametifos", "Tereflto", "Imidacloprid", "Propoxur"};
        String[] col3 = {"Deltametrina", "Cipermetrina", "Lambda-cialotrina", "Hidrametilnona", "Diclorvos", "Diazinon"};
        String[] col4 = {"Nao Aplicado", "Fipronil"};
        desenharColuna(d, MARGEM + 8, y, col1);
        desenharColuna(d, 165f, y, col2);
        desenharColuna(d, 320f, y, col3);
        desenharColuna(d, 470f, y, col4);
        d.retangulo(MARGEM, topo, dir - MARGEM, 22 + 6 * 17 + 6);
    }

    private static void desenharColuna(DesenhoFormulario d, float x, float y, String[] itens) {
        float yy = y;
        for (String item : itens) {
            d.checkbox(x, yy, item, 8.5f);
            yy += 17f;
        }
    }

    private static void assinaturas(DesenhoFormulario d, Dados dados, float dir) {
        float topo = 600f;
        float larguraBloco = (dir - MARGEM - 40f) / 3f;
        bloco(d, MARGEM, topo, larguraBloco, "Cliente", dados.nomeCliente());
        bloco(d, MARGEM + larguraBloco + 20f, topo, larguraBloco, "Fiscalizacao", "");
        bloco(d, MARGEM + 2 * (larguraBloco + 20f), topo, larguraBloco, "Lider Desinsetizacao", dados.nomeLider());
    }

    private static void bloco(DesenhoFormulario d, float x, float topo, float larguraBloco, String titulo, String nome) {
        d.texto(x, topo, 9f, true, titulo);
        d.retangulo(x, topo + 14, larguraBloco, 50f);
        d.linha(x, topo + 78, x + larguraBloco, topo + 78);
        d.textoCentralizado(x + larguraBloco / 2, topo + 80, 8f, false, "Assinatura");
        d.texto(x, topo + 96, 8.5f, true, "Nome");
        d.texto(x + 32, topo + 96, 8.5f, false, nome == null ? "" : nome);
        d.linha(x + 30, topo + 107, x + larguraBloco, topo + 107);
    }

    private static void rodape(DesenhoFormulario d, DadosEmpresa empresa, float dir) {
        float topo = 730f;
        d.textoCentralizado((MARGEM + dir) / 2, topo, 8.5f, true, "EMERGENCIA LIDER DESINSETIZADORA");
        d.textoCentralizado((MARGEM + dir) / 2, topo + 12, 8f, false, empresa.telefones());
    }

    private static void campoCaixa(DesenhoFormulario d, float x, float topo, float larguraCaixa, String rotulo, String valor) {
        d.texto(x, topo, 8f, true, rotulo);
        float xv = x + d.larguraDoTexto(rotulo + " ", 8f, true);
        d.retangulo(xv, topo - 2, x + larguraCaixa - xv, 13f);
        d.texto(xv + 3, topo, 8f, false, valor == null ? "" : valor);
    }

    public record Dados(
            String data,
            String funcionarios,
            String horaInicio,
            String horaTermino,
            String cliente,
            String cnpj,
            String endereco,
            String fone,
            String municipio,
            String estado,
            String responsavel,
            String nomeCliente,
            String nomeLider
    ) {
    }
}

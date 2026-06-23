package br.com.sigla.relatorios.formulario;

import br.com.sigla.dominio.servicos.DadosFormularioServico;
import br.com.sigla.dominio.servicos.OpcoesFormularioServico;
import br.com.sigla.dominio.servicos.OpcoesFormularioServico.Opcao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Preenche a Ordem de Servico CARIMBANDO os valores por cima do modelo LIDER
 * pre-impresso (ordem-servico-modelo.pdf), garantindo layout identico ao modelo.
 * Coordenadas medidas a partir do topo da pagina (mesmo sistema de DesenhoFormulario).
 */
public final class FormularioOrdemServico {

    // Linhas (topo) dos campos do cabecalho.
    private static final float L1 = 63f;  // DATA / MANHA / TARDE / FUNCIONARIO
    private static final float L2 = 75f;  // CLIENTE / INICIO
    private static final float L3 = 87f;  // CNPJ / TERMINO
    private static final float L4 = 99f;  // EMAIL / TELEFONE

    // X dos checkboxes por coluna (topo de cada linha).
    private static final float[] COL1 = {130f, 144.3f, 158.6f, 172.9f, 187.2f};       // aplicacao geral (5)
    private static final float[] COL3 = {130f, 144.3f, 158.6f, 172.9f};                // manutencao (4) — mesmo ritmo da COL1
    private static final float[] COL4 = {127f, 135.7f, 144.4f, 153.1f, 161.9f, 170.6f, 179.3f, 188f}; // produto (8) — passo ~8.7

    private FormularioOrdemServico() {
    }

    public static byte[] gerar(Dados dados, DadosEmpresa empresa) {
        DesenhoFormulario d = new DesenhoFormulario(ModeloFormulario.carregar(ModeloFormulario.ORDEM_SERVICO));
        DadosFormularioServico.Os os = dados.os();

        // Cabecalho (valores).
        d.textoAjustado(41f, L1, 31f, 8f, dados.data());
        d.textoAjustado(264f, L1, 68f, 8f, dados.funcionario());
        d.textoAjustado(52f, L2, 154f, 8f, dados.cliente());
        d.texto(246f, L2, 8f, false, dados.inicio());
        d.textoAjustado(45f, L3, 160f, 8f, dados.cnpj());
        d.texto(258f, L3, 8f, false, dados.termino());
        d.textoAjustado(46f, L4, 160f, 7.5f, dados.email());
        d.texto(262f, L4, 8f, false, dados.telefone());

        // Periodo manha/tarde.
        if (os.manha()) {
            d.textoCentralizado(122f, L1, 8f, true, "X");
        }
        if (os.tarde()) {
            d.textoCentralizado(186f, L1, 8f, true, "X");
        }

        // Tabela: colunas com checkboxes (centro do X dentro do "( )" de cada linha).
        marcarColuna(d, 17f, COL1, OpcoesFormularioServico.OS_APLICACAO_GERAL, os.aplicacaoGeral());
        marcarColuna(d, 180f, COL3, OpcoesFormularioServico.OS_MANUTENCAO, os.manutencao());

        Map<String, String> qtdePorProduto = new LinkedHashMap<>();
        for (DadosFormularioServico.Produto produto : os.produtos()) {
            qtdePorProduto.put(produto.tipo(), produto.qtde());
        }
        for (int i = 0; i < OpcoesFormularioServico.OS_PRODUTO.size(); i++) {
            String chave = OpcoesFormularioServico.OS_PRODUTO.get(i).chave();
            if (qtdePorProduto.containsKey(chave)) {
                d.textoCentralizado(256f, COL4[i], 8f, true, "X");
                String qtde = qtdePorProduto.get(chave);
                if (qtde != null && !qtde.isBlank()) {
                    d.textoCentralizado(318f, COL4[i], 8f, false, qtde);
                }
            }
        }

        // Coluna PRODUTO UTILIZADO (texto nas lacunas "Quantidade __" e "diluido em __").
        d.texto(145f, 126.5f, 7.5f, false, os.produto1Qtd());
        d.texto(118f, 135.5f, 7.5f, false, os.produto1Calda());
        d.texto(145f, 171.5f, 7.5f, false, os.produto2Qtd());
        d.texto(118f, 180.5f, 7.5f, false, os.produto2Calda());

        // Rodape.
        d.texto(46f, 201.5f, 8f, false, os.etapa());
        d.texto(140f, 201.5f, 8f, false, os.etapaDe());
        d.textoAjustado(40f, 216f, 290f, 8f, dados.observacoes());

        return d.finalizar();
    }

    private static void marcarColuna(DesenhoFormulario d, float centroX, float[] linhas, List<Opcao> opcoes, List<String> marcados) {
        for (int i = 0; i < opcoes.size() && i < linhas.length; i++) {
            if (marcados.contains(opcoes.get(i).chave())) {
                d.textoCentralizado(centroX, linhas[i], 8f, true, "X");
            }
        }
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
            String observacoes,
            DadosFormularioServico.Os os
    ) {
        public Dados {
            os = os == null ? DadosFormularioServico.Os.vazio() : os;
        }
    }
}

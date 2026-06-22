package br.com.sigla.interfacegrafica.util;

import br.com.sigla.dominio.servicos.DadosFormularioServico;
import br.com.sigla.dominio.servicos.OpcoesFormularioServico.Opcao;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lista de produtos/iscas com checkbox + campo QTDE por item, espelhando a coluna
 * PRODUTO/QTDE da Ordem de Servico. Produz {@link DadosFormularioServico.Produto}
 * apenas para os itens marcados.
 */
public final class GrupoProdutosQtde {

    private final Map<String, CheckBox> caixas = new LinkedHashMap<>();
    private final Map<String, TextField> quantidades = new LinkedHashMap<>();
    private final GridPane painel = new GridPane();

    public GrupoProdutosQtde(List<Opcao> opcoes) {
        painel.setHgap(8);
        painel.setVgap(4);
        int linha = 0;
        for (Opcao opcao : opcoes) {
            CheckBox caixa = new CheckBox(opcao.rotulo());
            TextField qtde = new TextField();
            qtde.setPromptText("Qtde");
            qtde.setPrefWidth(72);
            caixas.put(opcao.chave(), caixa);
            quantidades.put(opcao.chave(), qtde);
            painel.add(caixa, 0, linha);
            painel.add(qtde, 1, linha);
            linha++;
        }
    }

    public Region no() {
        return painel;
    }

    public List<DadosFormularioServico.Produto> selecionados() {
        List<DadosFormularioServico.Produto> produtos = new ArrayList<>();
        caixas.forEach((chave, caixa) -> {
            if (caixa.isSelected()) {
                produtos.add(new DadosFormularioServico.Produto(chave, quantidades.get(chave).getText()));
            }
        });
        return produtos;
    }

    public void marcar(List<DadosFormularioServico.Produto> produtos) {
        if (produtos == null) {
            return;
        }
        for (DadosFormularioServico.Produto produto : produtos) {
            CheckBox caixa = caixas.get(produto.tipo());
            TextField qtde = quantidades.get(produto.tipo());
            if (caixa != null) {
                caixa.setSelected(true);
            }
            if (qtde != null) {
                qtde.setText(produto.qtde());
            }
        }
    }
}

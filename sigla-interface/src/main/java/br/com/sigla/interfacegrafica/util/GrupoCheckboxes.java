package br.com.sigla.interfacegrafica.util;

import br.com.sigla.dominio.servicos.OpcoesFormularioServico.Opcao;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Grupo de checkboxes construido a partir das opcoes canonicas
 * ({@link br.com.sigla.dominio.servicos.OpcoesFormularioServico}). Garante que os
 * rotulos da tela e as chaves persistidas/usadas no PDF nunca divirjam.
 */
public final class GrupoCheckboxes {

    private final Map<String, CheckBox> caixas = new LinkedHashMap<>();
    private final FlowPane painel = new FlowPane(14, 6);

    public GrupoCheckboxes(List<Opcao> opcoes) {
        for (Opcao opcao : opcoes) {
            CheckBox caixa = new CheckBox(opcao.rotulo());
            caixas.put(opcao.chave(), caixa);
            painel.getChildren().add(caixa);
        }
    }

    public Region no() {
        return painel;
    }

    public List<String> selecionados() {
        return caixas.entrySet().stream()
                .filter(entrada -> entrada.getValue().isSelected())
                .map(Map.Entry::getKey)
                .toList();
    }

    public void marcar(List<String> chaves) {
        caixas.forEach((chave, caixa) -> caixa.setSelected(chaves != null && chaves.contains(chave)));
    }
}

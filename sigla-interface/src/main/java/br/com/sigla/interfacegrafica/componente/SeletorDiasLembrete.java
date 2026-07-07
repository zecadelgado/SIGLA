package br.com.sigla.interfacegrafica.componente;

import br.com.sigla.dominio.notificacoes.DiasLembrete;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Seletor das antecedencias de lembrete (30/15/7/1 dias), varias marcaveis ao mesmo tempo.
 * Substitui o antigo campo numerico unico. Nenhuma opcao marcada = lembrete desligado.
 */
public class SeletorDiasLembrete extends HBox {

    private final Map<Integer, CheckBox> opcoes = new LinkedHashMap<>();

    public SeletorDiasLembrete() {
        setSpacing(12);
        setAlignment(Pos.CENTER_LEFT);
        for (Integer dia : DiasLembrete.PRESETS) {
            CheckBox caixa = new CheckBox(dia + " dias");
            opcoes.put(dia, caixa);
            getChildren().add(caixa);
        }
    }

    /** Marca as caixas correspondentes aos dias informados (valores fora dos presets sao ignorados). */
    public void setDias(List<Integer> dias) {
        List<Integer> selecionados = dias == null ? List.of() : dias;
        opcoes.forEach((dia, caixa) -> caixa.setSelected(selecionados.contains(dia)));
    }

    /** Dias marcados, do maior para o menor (ex.: [30, 7]). Lista vazia = lembrete desligado. */
    public List<Integer> getDias() {
        List<Integer> selecionados = new ArrayList<>();
        opcoes.forEach((dia, caixa) -> {
            if (caixa.isSelected()) {
                selecionados.add(dia);
            }
        });
        return DiasLembrete.normalizar(selecionados);
    }

    public boolean algumSelecionado() {
        return opcoes.values().stream().anyMatch(CheckBox::isSelected);
    }
}

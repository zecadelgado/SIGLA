package br.com.sigla.interfacegrafica.util;

import br.com.sigla.interfacegrafica.modelo.OpcaoId;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import java.util.List;

public final class UtilComboBox {

    private static final OpcaoId VAZIO = new OpcaoId("", "");

    private UtilComboBox() {
    }

    public static void preencher(ComboBox<OpcaoId> comboBox, List<OpcaoId> opcoes, boolean permitirVazio) {
        if (comboBox == null) {
            return;
        }
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(OpcaoId opcao) {
                return opcao == null ? "" : opcao.label();
            }

            @Override
            public OpcaoId fromString(String value) {
                return comboBox.getItems().stream()
                        .filter(opcao -> opcao.label().equals(value))
                        .findFirst()
                        .orElse(null);
            }
        });
        comboBox.setItems(FXCollections.observableArrayList());
        if (permitirVazio) {
            comboBox.getItems().add(VAZIO);
        }
        comboBox.getItems().addAll(opcoes == null ? List.of() : opcoes);
        if (permitirVazio) {
            comboBox.getSelectionModel().select(VAZIO);
        }
    }

    public static String idSelecionado(ComboBox<OpcaoId> comboBox) {
        OpcaoId selecionado = comboBox == null ? null : comboBox.getValue();
        return selecionado == null ? "" : selecionado.id();
    }

    public static OpcaoId selecionado(ComboBox<OpcaoId> comboBox) {
        return comboBox == null ? null : comboBox.getValue();
    }

    public static void selecionarPorId(ComboBox<OpcaoId> comboBox, String id) {
        if (comboBox == null || id == null) {
            return;
        }
        comboBox.getItems().stream()
                .filter(opcao -> id.equals(opcao.id()))
                .findFirst()
                .ifPresent(opcao -> comboBox.getSelectionModel().select(opcao));
    }
}

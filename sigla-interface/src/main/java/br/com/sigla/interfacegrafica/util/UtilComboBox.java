package br.com.sigla.interfacegrafica.util;

import br.com.sigla.interfacegrafica.modelo.OpcaoId;
import javafx.scene.control.ComboBox;

<<<<<<< Updated upstream
=======
import java.util.ArrayList;
>>>>>>> Stashed changes
import java.util.List;

public final class UtilComboBox {

    private static final OpcaoId VAZIO = new OpcaoId("", "");

    private UtilComboBox() {
    }

    public static void preencher(ComboBox<OpcaoId> comboBox, List<OpcaoId> opcoes, boolean permitirVazio) {
        if (comboBox == null) {
            return;
        }
<<<<<<< Updated upstream
        OpcaoId selecionado = comboBox.getValue();
        comboBox.getItems().clear();
        if (permitirVazio) {
            comboBox.getItems().add(VAZIO);
        }
        comboBox.getItems().addAll(opcoes);
        if (selecionado != null && !selecionado.id().isBlank()) {
            selecionarPorId(comboBox, selecionado.id());
        } else if (permitirVazio) {
            comboBox.getSelectionModel().select(VAZIO);
        } else if (!comboBox.getItems().isEmpty()) {
=======
        String selecionado = idSelecionado(comboBox);
        List<OpcaoId> itens = new ArrayList<>();
        if (permitirVazio) {
            itens.add(VAZIO);
        }
        if (opcoes != null) {
            itens.addAll(opcoes);
        }
        comboBox.getItems().setAll(itens);
        selecionarPorId(comboBox, selecionado);
        if (comboBox.getSelectionModel().isEmpty() && permitirVazio) {
>>>>>>> Stashed changes
            comboBox.getSelectionModel().selectFirst();
        }
    }

    public static String idSelecionado(ComboBox<OpcaoId> comboBox) {
<<<<<<< Updated upstream
        if (comboBox == null || comboBox.getValue() == null) {
=======
        if (comboBox == null || comboBox.getValue() == null || comboBox.getValue().id() == null) {
>>>>>>> Stashed changes
            return "";
        }
        return comboBox.getValue().id();
    }

    public static void selecionarPorId(ComboBox<OpcaoId> comboBox, String id) {
        if (comboBox == null) {
            return;
        }
<<<<<<< Updated upstream
        if (id == null || id.isBlank()) {
            selecionarVazioOuLimpar(comboBox);
            return;
        }
        boolean selected = comboBox.getItems().stream()
                .filter(option -> id.equals(option.id()))
                .findFirst()
                .map(option -> {
                    comboBox.getSelectionModel().select(option);
                    return true;
                })
                .orElse(false);
        if (!selected) {
            selecionarVazioOuLimpar(comboBox);
        }
    }

    public static OpcaoId obrigatorio(ComboBox<OpcaoId> comboBox, String mensagem) {
        OpcaoId value = comboBox == null ? null : comboBox.getValue();
        if (value == null || value.id().isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
        return value;
    }

    private static void selecionarVazioOuLimpar(ComboBox<OpcaoId> comboBox) {
        if (comboBox.getItems().contains(VAZIO)) {
            comboBox.getSelectionModel().select(VAZIO);
        } else {
            comboBox.getSelectionModel().clearSelection();
        }
=======
        String alvo = id == null ? "" : id;
        comboBox.getItems().stream()
                .filter(item -> alvo.equals(item.id()))
                .findFirst()
                .ifPresentOrElse(
                        item -> comboBox.getSelectionModel().select(item),
                        () -> {
                            if (!comboBox.getItems().isEmpty() && comboBox.getItems().getFirst().id().isBlank()) {
                                comboBox.getSelectionModel().selectFirst();
                            } else {
                                comboBox.getSelectionModel().clearSelection();
                            }
                        }
                );
    }

    public static OpcaoId obrigatorio(ComboBox<OpcaoId> comboBox, String mensagem) {
        OpcaoId opcao = comboBox == null ? null : comboBox.getValue();
        if (opcao == null || opcao.id() == null || opcao.id().isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
        return opcao;
>>>>>>> Stashed changes
    }
}

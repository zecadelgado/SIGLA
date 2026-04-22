package br.com.sigla.interfacegrafica.util;

import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class UtilJanela {

    private UtilJanela() {
    }

    public static void fecharJanela(Node node) {
        if (node == null || node.getScene() == null) {
            return;
        }

        Window window = node.getScene().getWindow();
        if (window instanceof Stage stage) {
            stage.close();
        }
    }
}

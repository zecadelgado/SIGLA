package br.com.sigla.interfacegrafica.aplicativo;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

final class JanelaPosicionador {

    private JanelaPosicionador() {
    }

    static void centralizarNaTelaAtiva(Stage stage) {
        if (stage == null) {
            return;
        }
        centralizar(stage, Screen.getPrimary().getVisualBounds());
    }

    private static void centralizar(Stage stage, Rectangle2D bounds) {
        double largura = resolverDimensao(stage.getWidth(), stage.getScene() != null ? stage.getScene().getWidth() : 0);
        double altura = resolverDimensao(stage.getHeight(), stage.getScene() != null ? stage.getScene().getHeight() : 0);

        stage.setX(bounds.getMinX() + ((bounds.getWidth() - largura) / 2));
        stage.setY(bounds.getMinY() + ((bounds.getHeight() - altura) / 2));
    }

    private static double resolverDimensao(double dimensaoStage, double dimensaoScene) {
        if (dimensaoStage > 0) {
            return dimensaoStage;
        }
        return Math.max(dimensaoScene, 1);
    }
}

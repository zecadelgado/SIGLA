package br.com.sigla.interfacegrafica.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * Diálogos padronizados. Remove o cabeçalho redundante do JavaFX (que mostrava
 * "Mensagem" repetido) e define um título claro, exibindo só a mensagem real.
 */
public final class DialogoUi {

    private DialogoUi() {
    }

    public static void informacao(String mensagem) {
        exibir(Alert.AlertType.INFORMATION, "Informação", mensagem);
    }

    public static void aviso(String mensagem) {
        exibir(Alert.AlertType.WARNING, "Atenção", mensagem);
    }

    public static void erro(String mensagem) {
        exibir(Alert.AlertType.ERROR, "Erro", mensagem);
    }

    private static void exibir(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alert = new Alert(tipo, mensagem == null || mensagem.isBlank() ? "Ação não realizada." : mensagem, ButtonType.OK);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}

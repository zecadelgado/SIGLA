package br.com.sigla.interfacegrafica.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.List;

/**
 * Notificacao "toast": um cartao discreto que desliza no canto superior direito
 * da janela ativa, permanece alguns segundos e some sozinho, sem bloquear o usuario.
 *
 * <p>Usada para feedback de sucesso/informacao leve (ex.: "Cadastro salvo").
 * Erros e avisos continuam em dialogo modal (ver {@link DialogoUi}). E somente a
 * camada visual; o estilo fica em {@code css/dialogos.css} (classes {@code .toast*}).
 */
public final class ToastNotificacao {

    private static final Duration ENTRADA = Duration.millis(220);
    private static final Duration VISIVEL = Duration.seconds(3);
    private static final Duration SAIDA = Duration.millis(260);
    private static final double MARGEM = 24;

    private ToastNotificacao() {
    }

    public static boolean sucesso(String mensagem) {
        return exibir("sucesso", "✓", mensagem);
    }

    public static boolean info(String mensagem) {
        return exibir("info", "ℹ", mensagem);
    }

    /**
     * Mostra o toast. Retorna {@code false} se nao houver janela visivel para
     * ancorar (cabe ao chamador decidir um fallback, ex.: dialogo modal).
     */
    public static boolean exibir(String tipo, String icone, String mensagem) {
        if (mensagem == null || mensagem.isBlank()) {
            return false;
        }
        Window janela = janelaAtiva();
        if (janela == null) {
            return false;
        }
        Platform.runLater(() -> mostrar(janela, tipo, icone, mensagem));
        return true;
    }

    private static void mostrar(Window janela, String tipo, String icone, String mensagem) {
        Label iconeLabel = new Label(icone);
        iconeLabel.getStyleClass().add("toast-icone");

        Label texto = new Label(mensagem);
        texto.getStyleClass().add("toast-texto");
        texto.setWrapText(true);
        texto.setMaxWidth(320);

        HBox cartao = new HBox(iconeLabel, texto);
        cartao.getStyleClass().addAll("toast", tipo);
        cartao.setAlignment(Pos.CENTER_LEFT);
        cartao.setMaxWidth(Region.USE_PREF_SIZE);

        Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.getContent().add(cartao);

        popup.setOnShown(e -> {
            if (popup.getScene() != null) {
                popup.getScene().getStylesheets().add(
                        ToastNotificacao.class.getResource("/css/dialogos.css").toExternalForm());
            }
            posicionar(popup, janela, cartao);
            animar(popup, cartao);
        });

        popup.show(janela);
    }

    private static void posicionar(Popup popup, Window janela, Region cartao) {
        double largura = cartao.prefWidth(-1);
        double x = janela.getX() + janela.getWidth() - largura - MARGEM;
        double y = janela.getY() + MARGEM;
        popup.setX(x);
        popup.setY(y);
    }

    private static void animar(Popup popup, Region cartao) {
        cartao.setOpacity(0);
        cartao.setTranslateY(-14);

        FadeTransition aparecer = new FadeTransition(ENTRADA, cartao);
        aparecer.setFromValue(0);
        aparecer.setToValue(1);

        TranslateTransition descer = new TranslateTransition(ENTRADA, cartao);
        descer.setFromY(-14);
        descer.setToY(0);
        aparecer.play();
        descer.play();

        PauseTransition espera = new PauseTransition(VISIVEL);
        FadeTransition sumir = new FadeTransition(SAIDA, cartao);
        sumir.setFromValue(1);
        sumir.setToValue(0);

        SequentialTransition fim = new SequentialTransition(espera, sumir);
        fim.setOnFinished(e -> popup.hide());
        fim.play();
    }

    private static Window janelaAtiva() {
        List<Window> janelas = Window.getWindows();
        Window visivel = null;
        for (Window janela : janelas) {
            if (!janela.isShowing() || janela instanceof Popup) {
                continue;
            }
            if (janela.isFocused()) {
                return janela;
            }
            if (visivel == null) {
                visivel = janela;
            }
        }
        return visivel;
    }
}

package br.com.sigla.interfacegrafica.async;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

/**
 * Tela de carregamento reutilizavel: um veu translucido sobre o conteudo com um
 * cartao central contendo um anel animado e uma mensagem ("Carregando...").
 *
 * <p>E apenas a parte visual. O ciclo de vida (quando aparecer/sumir, atraso para
 * nao "piscar" em cargas rapidas, animacao de fade) fica no {@link IndicadorCarregamento}.
 * Assim o mesmo componente serve tanto para o conteudo do shell quanto para a tela de login.
 */
public class SobreposicaoCarregamento extends StackPane {

    private static final double RAIO = 26;
    private static final double ESPESSURA = 5.5;

    private final Group anel;
    private final RotateTransition rotacao;
    private final Label mensagemLabel;

    public SobreposicaoCarregamento() {
        this("Carregando...");
    }

    public SobreposicaoCarregamento(String mensagem) {
        getStyleClass().add("overlay-carregamento");
        setAlignment(Pos.CENTER);

        Circle trilha = new Circle(RAIO);
        trilha.setFill(Color.TRANSPARENT);
        trilha.setStroke(Color.web("#d9e3f4"));
        trilha.setStrokeWidth(ESPESSURA);

        Arc arco = new Arc(0, 0, RAIO, RAIO, 90, -270);
        arco.setType(ArcType.OPEN);
        arco.setFill(Color.TRANSPARENT);
        arco.setStroke(Color.web("#2a59c7"));
        arco.setStrokeWidth(ESPESSURA);
        arco.setStrokeLineCap(StrokeLineCap.ROUND);

        // O anel inteiro gira em torno do proprio centro (0,0): o circulo de fundo
        // e simetrico (giro imperceptivel) e o arco azul roda formando o efeito de spinner.
        anel = new Group(trilha, arco);

        double lado = (RAIO + ESPESSURA) * 2;
        StackPane anelBox = new StackPane(anel);
        anelBox.setMinSize(lado, lado);
        anelBox.setPrefSize(lado, lado);
        anelBox.setMaxSize(lado, lado);

        rotacao = new RotateTransition(Duration.millis(950), anel);
        rotacao.setByAngle(360);
        rotacao.setInterpolator(Interpolator.LINEAR);
        rotacao.setCycleCount(Animation.INDEFINITE);

        mensagemLabel = new Label(mensagem);
        mensagemLabel.getStyleClass().add("overlay-carregamento-texto");

        VBox cartao = new VBox(16, anelBox, mensagemLabel);
        cartao.setAlignment(Pos.CENTER);
        cartao.getStyleClass().add("overlay-carregamento-cartao");
        cartao.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        getChildren().add(cartao);
    }

    public void iniciarAnimacao() {
        rotacao.playFromStart();
    }

    public void pararAnimacao() {
        rotacao.stop();
        anel.setRotate(0);
    }

    public void definirMensagem(String mensagem) {
        if (mensagem != null && !mensagem.isBlank()) {
            mensagemLabel.setText(mensagem);
        }
    }
}

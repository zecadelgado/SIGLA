package br.com.sigla.interfacegrafica.aplicativo;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

final class TelaAberturaAplicacao {

    private static final String VIDEO_ABERTURA = "/videos/Abertura_Sigla.mp4";
    private static final String LOGO_SIGLA = "/imagens/sigla.png";
    static final double LARGURA = 400;
    static final double ALTURA = 400;

    private MediaPlayer mediaPlayer;

    TelaAberturaAplicacao() {
    }

    Parent criarConteudo() {
        StackPane raiz = new StackPane();
        raiz.setMinSize(LARGURA, ALTURA);
        raiz.setPrefSize(LARGURA, ALTURA);
        raiz.setMaxSize(LARGURA, ALTURA);
        raiz.setStyle("-fx-background-color: #05070c;");
        raiz.getChildren().add(criarFallbackVisual());

        MediaView mediaView = criarVideoAbertura();
        if (mediaView != null) {
            mediaView.setFitWidth(LARGURA);
            mediaView.setFitHeight(ALTURA);
            raiz.getChildren().add(mediaView);
        }

        return raiz;
    }

    void reproduzir() {
        if (mediaPlayer != null) {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.READY) {
                mediaPlayer.play();
            } else {
                mediaPlayer.setOnReady(mediaPlayer::play);
            }
        }
    }

    void parar() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    private MediaView criarVideoAbertura() {
        try {
            Path videoTemporario = extrairVideoParaArquivoTemporario();
            Media media = new Media(videoTemporario.toUri().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setAutoPlay(false);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);

            MediaView mediaView = new MediaView(mediaPlayer);
            mediaView.setPreserveRatio(true);
            mediaView.setSmooth(true);
            return mediaView;
        } catch (IOException | RuntimeException exception) {
            return null;
        }
    }

    private Path extrairVideoParaArquivoTemporario() throws IOException {
        Path destino = Files.createTempFile("sigla-abertura-", ".mp4");
        destino.toFile().deleteOnExit();

        try (InputStream origem = TelaAberturaAplicacao.class.getResourceAsStream(VIDEO_ABERTURA)) {
            if (origem == null) {
                throw new IOException("Video de abertura nao encontrado em " + VIDEO_ABERTURA);
            }
            Files.copy(origem, destino, StandardCopyOption.REPLACE_EXISTING);
        }

        return destino;
    }

    private Node criarFallbackVisual() {
        StackPane fallback = new StackPane();
        fallback.setMinSize(LARGURA, ALTURA);
        fallback.setPrefSize(LARGURA, ALTURA);
        fallback.setMaxSize(LARGURA, ALTURA);
        fallback.setStyle("-fx-background-color: #05070c;");

        ImageView logo = new ImageView(new Image(
                Objects.requireNonNull(TelaAberturaAplicacao.class.getResourceAsStream(LOGO_SIGLA))
        ));
        logo.setPreserveRatio(true);
        logo.setFitWidth(180);
        fallback.getChildren().add(logo);
        return fallback;
    }

}

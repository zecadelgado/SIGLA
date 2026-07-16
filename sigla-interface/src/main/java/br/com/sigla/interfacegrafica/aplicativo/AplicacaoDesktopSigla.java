package br.com.sigla.interfacegrafica.aplicativo;

import br.com.sigla.interfacegrafica.inicializacao.AplicacaoSpringSigla;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AplicacaoDesktopSigla extends Application {

    private ConfigurableApplicationContext context;
    private ExecutorService executorInicializacao;
    private TelaAberturaAplicacao telaAbertura;
    private Stage splashStage;

    @Override
    public void init() {
        executorInicializacao = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "sigla-inicializacao-spring");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("S.I.G.L.A");

        stage.getIcons().add(new Image(
                AplicacaoDesktopSigla.class.getResourceAsStream("/imagens/sigla.png")
        ));

        telaAbertura = new TelaAberturaAplicacao();
        splashStage = new Stage(StageStyle.TRANSPARENT);
        splashStage.getIcons().addAll(stage.getIcons());
        splashStage.setTitle("S.I.G.L.A");

        Scene cenaAbertura = new Scene(
                telaAbertura.criarConteudo(),
                TelaAberturaAplicacao.LARGURA,
                TelaAberturaAplicacao.ALTURA,
                Color.TRANSPARENT
        );
        cenaAbertura.setFill(Color.TRANSPARENT);
        splashStage.setScene(cenaAbertura);
        splashStage.setResizable(false);
        splashStage.show();
        JanelaPosicionador.centralizarNaTelaAtiva(splashStage);
        Platform.runLater(() -> JanelaPosicionador.centralizarNaTelaAtiva(splashStage));
        telaAbertura.reproduzir();

        CompletableFuture
                .supplyAsync(this::inicializarContextoSpring, executorInicializacao)
                .whenComplete((contextoInicializado, falha) -> Platform.runLater(() -> {
                    if (falha != null) {
                        exibirFalhaInicializacao(falha);
                        return;
                    }
                    context = contextoInicializado;
                    abrirLogin(stage);
                }));
    }

    private ConfigurableApplicationContext inicializarContextoSpring() {
        return new SpringApplicationBuilder(AplicacaoSpringSigla.class)
                .headless(false)
                .run();
    }

    private void abrirLogin(Stage stage) {
        if (context == null) {
            return;
        }

        if (telaAbertura != null) {
            telaAbertura.parar();
            telaAbertura = null;
        }
        if (splashStage != null) {
            splashStage.close();
            splashStage = null;
        }

        FluxoAplicacao fluxoAplicacao = context.getBean(FluxoAplicacao.class);
        fluxoAplicacao.attachStage(stage);
        fluxoAplicacao.showLogin();
        stage.setOnShown(event -> fluxoAplicacao.ajustarJanelaInicial());
        stage.show();
        fluxoAplicacao.ajustarJanelaInicial();
    }

    private void exibirFalhaInicializacao(Throwable falha) {
        Throwable causa = falha.getCause() != null ? falha.getCause() : falha;
        causa.printStackTrace();
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("S.I.G.L.A");
        alerta.setHeaderText("Nao foi possivel iniciar o sistema.");
        alerta.setContentText(mensagemInicializacao(causa));
        alerta.showAndWait();

        // Mantém a identidade visual do SIGLA atrás do aviso de falha. A tela de
        // abertura só é encerrada após o usuário reconhecer a mensagem.
        if (telaAbertura != null) {
            telaAbertura.parar();
        }
        if (splashStage != null) {
            splashStage.close();
        }
        Platform.exit();
    }

    private String mensagemInicializacao(Throwable falha) {
        String texto = falha.getMessage() == null ? "" : falha.getMessage().toLowerCase();
        if (texto.contains("password authentication failed")
                || texto.contains("unable to obtain connection")
                || texto.contains("entitymanagerfactory")
                || texto.contains("flyway")) {
            return "Nao foi possivel conectar ao banco de dados do SIGLA. Verifique a conexao com a internet ou contate o suporte.";
        }
        return "O SIGLA encontrou um problema ao iniciar. Feche o sistema e tente abrir novamente.";
    }

    @Override
    public void stop() {
        if (telaAbertura != null) {
            telaAbertura.parar();
        }
        if (splashStage != null) {
            splashStage.close();
        }
        if (context != null) {
            context.close();
        }
        if (executorInicializacao != null) {
            executorInicializacao.shutdownNow();
        }
    }
}


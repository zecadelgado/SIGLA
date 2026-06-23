package br.com.sigla.interfacegrafica.aplicativo;

import br.com.sigla.interfacegrafica.inicializacao.AplicacaoSpringSigla;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class AplicacaoDesktopSigla extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(AplicacaoSpringSigla.class)
                .headless(false)
                .run();
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("S.I.G.L.A");

        stage.getIcons().add(new Image(
                AplicacaoDesktopSigla.class.getResourceAsStream("/imagens/sigla.png")
        ));

        FluxoAplicacao fluxoAplicacao = context.getBean(FluxoAplicacao.class);
        fluxoAplicacao.attachStage(stage);
        fluxoAplicacao.showLogin();
        stage.show();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }
}


package br.com.sigla.interfacegrafica.aplicativo;

import br.com.sigla.interfacegrafica.inicializacao.AplicacaoSpringSigla;
import javafx.application.Application;
import javafx.scene.Scene;
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
        EstruturaAplicacao appShell = new EstruturaAplicacao(context);
        Scene scene = new Scene(appShell.loadRoot(), 1280, 720);
        scene.getStylesheets().add(getClass().getResource("/css/base.css").toExternalForm());

        stage.setTitle("S.I.G.L.A");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }
}


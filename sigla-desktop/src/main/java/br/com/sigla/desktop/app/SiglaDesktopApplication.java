package br.com.sigla.desktop.app;

import br.com.sigla.desktop.bootstrap.SiglaSpringApplication;
import br.com.sigla.desktop.bootstrap.SpringContextHolder;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class SiglaDesktopApplication extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(SiglaSpringApplication.class)
                .headless(false)
                .run();
        SpringContextHolder.setContext(context);
    }

    @Override
    public void start(Stage stage) {
        AppShell appShell = new AppShell(context);
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

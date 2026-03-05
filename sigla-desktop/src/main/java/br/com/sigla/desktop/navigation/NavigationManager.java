package br.com.sigla.desktop.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ResourceBundle;

@Component
public class NavigationManager {

    private final ConfigurableApplicationContext context;
    private StackPane host;

    public NavigationManager(ConfigurableApplicationContext context) {
        this.context = context;
    }

    public void bindHost(StackPane host) {
        this.host = host;
    }

    public void navigateTo(AppView view) {
        if (host == null) {
            throw new IllegalStateException("Navigation host not bound");
        }

        try {
            ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages");
            FXMLLoader loader = new FXMLLoader(getClass().getResource(view.fxmlPath()), bundle);
            loader.setControllerFactory(context::getBean);
            Node content = loader.load();

            host.getChildren().setAll(content);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot navigate to " + view, e);
        }
    }
}

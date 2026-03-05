package br.com.sigla.desktop.app;

import br.com.sigla.desktop.controller.AppShellController;
import br.com.sigla.desktop.navigation.NavigationManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.util.ResourceBundle;

public class AppShell {

    private final ConfigurableApplicationContext context;

    public AppShell(ConfigurableApplicationContext context) {
        this.context = context;
    }

    public Parent loadRoot() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/layout/app-shell.fxml"), bundle);
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();

            AppShellController controller = loader.getController();
            NavigationManager navigationManager = context.getBean(NavigationManager.class);
            controller.bindNavigation(navigationManager);

            return root;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load AppShell FXML", exception);
        }
    }
}

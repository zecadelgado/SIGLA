package br.com.sigla.interfacegrafica.aplicativo;

import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ResourceBundle;

@Component
public class FluxoAplicacao {

    private final ConfigurableApplicationContext context;
    private final SessaoLocalAplicacao sessaoLocalAplicacao;
    private Stage stage;

    public FluxoAplicacao(
            ConfigurableApplicationContext context,
            SessaoLocalAplicacao sessaoLocalAplicacao
    ) {
        this.context = context;
        this.sessaoLocalAplicacao = sessaoLocalAplicacao;
    }

    public void attachStage(Stage stage) {
        this.stage = stage;
    }

    public void showLogin() {
        sessaoLocalAplicacao.logout();
        showView(VisaoAplicacao.LOGIN);
    }

    public void showShell() {
        showView(VisaoAplicacao.DASHBOARD);
    }

    public void showView(VisaoAplicacao view) {
        if (view != VisaoAplicacao.LOGIN && !sessaoLocalAplicacao.isAuthenticated()) {
            throw new IllegalStateException("Authentication required before opening the application");
        }
        setRoot(loadRoot(view));
    }

    private Parent loadRoot(VisaoAplicacao view) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("i18n.mensagens");
            FXMLLoader loader = new FXMLLoader(getClass().getResource(view.fxmlPath()), bundle);
            loader.setControllerFactory(context::getBean);
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load view " + view, exception);
        }
    }

    private void setRoot(Parent root) {
        if (stage == null) {
            throw new IllegalStateException("Primary stage not attached");
        }

        if (stage.getScene() == null) {
            Scene scene = new Scene(root, 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/css/base.css").toExternalForm());
            stage.setScene(scene);
            return;
        }

        stage.getScene().setRoot(root);
    }
}

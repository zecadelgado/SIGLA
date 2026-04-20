package br.com.sigla.interfacegrafica.aplicativo;

import br.com.sigla.interfacegrafica.controlador.ControladorEstruturaAplicacao;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.util.ResourceBundle;

public class EstruturaAplicacao {

    private final ConfigurableApplicationContext context;

    public EstruturaAplicacao(ConfigurableApplicationContext context) {
        this.context = context;
    }

    public Parent loadRoot() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("i18n.mensagens");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/telas/TelaPrincipal.fxml"), bundle);
            loader.setControllerFactory(context::getBean);
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load EstruturaAplicacao FXML", exception);
        }
    }
}


package br.com.sigla.interfacegrafica.navegacao;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ResourceBundle;

@Component
public class GerenciadorNavegacao {

    private final ConfigurableApplicationContext context;
    private final br.com.sigla.interfacegrafica.aplicativo.FluxoAplicacao fluxoAplicacao;
    private VisaoAplicacao currentView;

    public GerenciadorNavegacao(
            ConfigurableApplicationContext context,
            br.com.sigla.interfacegrafica.aplicativo.FluxoAplicacao fluxoAplicacao
    ) {
        this.context = context;
        this.fluxoAplicacao = fluxoAplicacao;
    }

    public void navigateTo(VisaoAplicacao view) {
        fluxoAplicacao.showView(view);
        currentView = view;
    }

    public Node loadShellContent(VisaoAplicacao view) {
        if (!view.isShellContent()) {
            throw new IllegalArgumentException("View is not shell content: " + view);
        }
        Node content = loadView(view);
        currentView = view;
        return content;
    }

    public Node loadView(VisaoAplicacao view) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("i18n.mensagens");
            FXMLLoader loader = new FXMLLoader(getClass().getResource(view.fxmlPath()), bundle);
            loader.setControllerFactory(context::getBean);
            return loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot navigate to " + view, e);
        }
    }

    public VisaoAplicacao currentView() {
        return currentView;
    }
}


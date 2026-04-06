package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import org.springframework.stereotype.Component;

@Component
public class ControladorEstruturaAplicacao {

    @FXML
    private StackPane contentHost;
    private GerenciadorNavegacao navigationManager;

    public void bindNavigation(GerenciadorNavegacao navigationManager) {
        this.navigationManager = navigationManager;
        this.navigationManager.bindHost(contentHost);
        this.navigationManager.navigateTo(VisaoAplicacao.DASHBOARD);
    }

    @FXML
    private void onDashboardClick() {
        navigate(VisaoAplicacao.DASHBOARD);
    }

    @FXML
    private void onFinanceiroClick() {
        navigate(VisaoAplicacao.FINANCE);
    }

    @FXML
    private void onClientesClick() {
        navigate(VisaoAplicacao.CUSTOMERS);
    }

    @FXML
    private void onServicosClick() {
        navigate(VisaoAplicacao.SERVICES);
    }

    @FXML
    private void onEstoqueClick() {
        navigate(VisaoAplicacao.INVENTORY);
    }

    private void navigate(VisaoAplicacao view) {
        if (navigationManager != null) {
            navigationManager.navigateTo(view);
        }
    }
}


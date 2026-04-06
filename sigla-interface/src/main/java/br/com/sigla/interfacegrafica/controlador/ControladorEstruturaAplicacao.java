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
        this.navigationManager.navigateTo(VisaoAplicacao.CUSTOMERS);
    }

    @FXML
    private void onClientesClick() {
        navigate(VisaoAplicacao.CUSTOMERS);
    }

    @FXML
    private void onContratosClick() {
        navigate(VisaoAplicacao.CONTRACTS);
    }

    @FXML
    private void onSchedulingClick() {
        navigate(VisaoAplicacao.SCHEDULING);
    }

    @FXML
    private void onServicesClick() {
        navigate(VisaoAplicacao.SERVICES);
    }

    @FXML
    private void onFinanceClick() {
        navigate(VisaoAplicacao.FINANCE);
    }

    @FXML
    private void onInventoryClick() {
        navigate(VisaoAplicacao.INVENTORY);
    }

    @FXML
    private void onCertificadosClick() {
        navigate(VisaoAplicacao.CERTIFICATES);
    }

    @FXML
    private void onPotencialClientesClick() {
        navigate(VisaoAplicacao.LEADS);
    }

    @FXML
    private void onNotificacaosClick() {
        navigate(VisaoAplicacao.NOTIFICATIONS);
    }

    @FXML
    private void onFuncionariosClick() {
        navigate(VisaoAplicacao.EMPLOYEES);
    }

    private void navigate(VisaoAplicacao view) {
        if (navigationManager != null) {
            navigationManager.navigateTo(view);
        }
    }
}


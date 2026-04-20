package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import org.springframework.stereotype.Component;

@Component
public class ControladorEstruturaAplicacao {

    private final GerenciadorNavegacao navigationManager;

    public ControladorEstruturaAplicacao(GerenciadorNavegacao navigationManager) {
        this.navigationManager = navigationManager;
    }

    @javafx.fxml.FXML
    private void onDashboardClick() {
        navigate(VisaoAplicacao.DASHBOARD);
    }

    @javafx.fxml.FXML
    private void onCadastrosClick() {
        navigate(VisaoAplicacao.REGISTRY);
    }

    @javafx.fxml.FXML
    private void onFinanceiroClick() {
        navigate(VisaoAplicacao.FINANCE);
    }

    @javafx.fxml.FXML
    private void onClientesClick() {
        navigate(VisaoAplicacao.CUSTOMERS);
    }

    @javafx.fxml.FXML
    private void onServicosClick() {
        navigate(VisaoAplicacao.SERVICES);
    }

    @javafx.fxml.FXML
    private void onOrdemServicoClick() {
        navigate(VisaoAplicacao.SERVICE_ORDER);
    }

    @javafx.fxml.FXML
    private void onEstoqueClick() {
        navigate(VisaoAplicacao.INVENTORY);
    }

    private void navigate(VisaoAplicacao view) {
        navigationManager.navigateTo(view);
    }
}


package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import javafx.fxml.FXML;

public abstract class ControladorComMenuPrincipal {

    private final GerenciadorNavegacao gerenciadorNavegacao;

    protected ControladorComMenuPrincipal(GerenciadorNavegacao gerenciadorNavegacao) {
        this.gerenciadorNavegacao = gerenciadorNavegacao;
    }

    @FXML
    protected void onDashboardClick() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.DASHBOARD);
    }

    @FXML
    protected void onCadastrosClick() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.REGISTRY);
    }

    @FXML
    protected void onFinanceiroClick() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.FINANCE);
    }

    @FXML
    protected void onServicosClick() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.SERVICES);
    }

    @FXML
    protected void onOrdemServicoClick() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.SERVICE_ORDER);
    }

    @FXML
    protected void onEstoqueClick() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.INVENTORY);
    }

    @FXML
    protected void onClientesClick() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.CUSTOMERS);
    }
}

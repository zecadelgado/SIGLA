package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.interfacegrafica.aplicativo.FluxoAplicacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import org.springframework.stereotype.Component;

@Component
public class ControladorEstruturaAplicacao {

    private final GerenciadorNavegacao navigationManager;
    private final FluxoAplicacao fluxoAplicacao;

    @FXML
    private BorderPane contentHost;

    public ControladorEstruturaAplicacao(GerenciadorNavegacao navigationManager, FluxoAplicacao fluxoAplicacao) {
        this.navigationManager = navigationManager;
        this.fluxoAplicacao = fluxoAplicacao;
    }

    @FXML
    public void initialize() {
        navigationManager.registerShellContentHost(contentHost);
        navigate(VisaoAplicacao.DASHBOARD);
    }

    @FXML
    private void onDashboardClick() {
        navigate(VisaoAplicacao.DASHBOARD);
    }

    @FXML
    private void onCadastrosClick() {
        navigate(VisaoAplicacao.REGISTRY);
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
    private void onAgendaClick() {
        navigate(VisaoAplicacao.AGENDA);
    }

    @FXML
    private void onOrdemServicoClick() {
        navigate(VisaoAplicacao.SERVICE_ORDER);
    }

    @FXML
    private void onEstoqueClick() {
        navigate(VisaoAplicacao.INVENTORY);
    }

    @FXML
    private void onContratosCertificadosClick() {
        navigate(VisaoAplicacao.CONTRACTS_CERTIFICATES);
    }

    @FXML
    private void onLogoutClick() {
        navigationManager.registerShellContentHost(null);
        fluxoAplicacao.showLogin();
    }

    private void navigate(VisaoAplicacao view) {
        navigationManager.navigateTo(view);
    }
}


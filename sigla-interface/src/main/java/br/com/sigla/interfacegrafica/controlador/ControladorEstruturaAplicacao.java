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
    private void onOrdemServicoClick() {
        navigate(VisaoAplicacao.SERVICE_ORDER);
    }

    @FXML
    private void onEstoqueClick() {
        navigate(VisaoAplicacao.INVENTORY);
    }

    @FXML
    private void onContratosClick() {
        navigate(VisaoAplicacao.CONTRACTS);
    }

    @FXML
    private void onCertificadosClick() {
        navigate(VisaoAplicacao.CERTIFICATES);
    }

    @FXML
    private void onNotificacoesClick() {
        navigate(VisaoAplicacao.NOTIFICATIONS);
    }

    @FXML
    private void onUsuariosClick() {
        navigate(VisaoAplicacao.USERS_ADMIN);
    }

    @FXML
    private void onLogoutClick() {
        fluxoAplicacao.showLogin();
    }

    private void navigate(VisaoAplicacao view) {
        if (view.isSobreposta()) {
            fluxoAplicacao.showView(view);
            return;
        }
        if (view.isShellContent() && contentHost != null) {
            contentHost.setCenter(navigationManager.loadShellContent(view));
            return;
        }
        navigationManager.navigateTo(view);
    }
}


package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.interfacegrafica.aplicativo.FluxoAplicacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class ControladorEstruturaAplicacao {

    private final GerenciadorNavegacao navigationManager;
    private final FluxoAplicacao fluxoAplicacao;

    private static final String CLASSE_ATIVO = "active";

    @FXML
    private BorderPane contentHost;
    @FXML
    private Region menuLateral;
    @FXML
    private Button navDashboard;
    @FXML
    private Button navCadastros;
    @FXML
    private Button navFinanceiro;
    @FXML
    private Button navClientes;
    @FXML
    private Button navServicos;
    @FXML
    private Button navAgenda;
    @FXML
    private Button navOrdemServico;
    @FXML
    private Button navEstoque;
    @FXML
    private Button navContratos;
    @FXML
    private Button navUsuarios;
    @FXML
    private Button navSair;

    private final Map<VisaoAplicacao, Button> botoesNavegacao = new EnumMap<>(VisaoAplicacao.class);

    public ControladorEstruturaAplicacao(GerenciadorNavegacao navigationManager, FluxoAplicacao fluxoAplicacao) {
        this.navigationManager = navigationManager;
        this.fluxoAplicacao = fluxoAplicacao;
    }

    @FXML
    public void initialize() {
        mapearBotoesNavegacao();
        navigationManager.registerShellContentHost(contentHost);
        navigationManager.registerShellMenu(menuLateral);
        navigate(VisaoAplicacao.DASHBOARD);
    }

    private void mapearBotoesNavegacao() {
        registrarBotao(VisaoAplicacao.DASHBOARD, navDashboard);
        registrarBotao(VisaoAplicacao.REGISTRY, navCadastros);
        registrarBotao(VisaoAplicacao.FINANCE, navFinanceiro);
        registrarBotao(VisaoAplicacao.CUSTOMERS, navClientes);
        registrarBotao(VisaoAplicacao.SERVICES, navServicos);
        registrarBotao(VisaoAplicacao.AGENDA, navAgenda);
        registrarBotao(VisaoAplicacao.SERVICE_ORDER, navOrdemServico);
        registrarBotao(VisaoAplicacao.INVENTORY, navEstoque);
        registrarBotao(VisaoAplicacao.CONTRACTS_CERTIFICATES, navContratos);
        registrarBotao(VisaoAplicacao.USERS, navUsuarios);
    }

    private void registrarBotao(VisaoAplicacao view, Button botao) {
        if (botao != null) {
            botoesNavegacao.put(view, botao);
        }
    }

    @FXML
    private void onAlternarMenuLateral() {
        navigationManager.alternarMenuLateral();
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
    private void onUsuariosClick() {
        navigate(VisaoAplicacao.USERS);
    }

    @FXML
    private void onLogoutClick() {
        navigationManager.registerShellContentHost(null);
        fluxoAplicacao.showLogin();
    }

    private void navigate(VisaoAplicacao view) {
        navigationManager.navigateTo(view);
        marcarBotaoAtivo(view);
    }

    private void marcarBotaoAtivo(VisaoAplicacao view) {
        botoesNavegacao.values().forEach(botao -> botao.getStyleClass().remove(CLASSE_ATIVO));
        Button ativo = botoesNavegacao.get(view);
        if (ativo != null && !ativo.getStyleClass().contains(CLASSE_ATIVO)) {
            ativo.getStyleClass().add(CLASSE_ATIVO);
        }
    }
}


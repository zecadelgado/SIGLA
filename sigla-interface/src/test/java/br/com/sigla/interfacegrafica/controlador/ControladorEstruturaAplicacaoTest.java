package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.interfacegrafica.aplicativo.FluxoAplicacao;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import javafx.scene.layout.BorderPane;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControladorEstruturaAplicacaoTest {

    @Test
    void shouldNavigateToDashboardFromMainMenu() throws Exception {
        SpyGerenciadorNavegacao navigationManager = new SpyGerenciadorNavegacao();
        ControladorEstruturaAplicacao controller = new ControladorEstruturaAplicacao(navigationManager, new SpyFluxoAplicacao());

        invoke(controller, "onDashboardClick");
        assertEquals(VisaoAplicacao.DASHBOARD, navigationManager.lastView);
    }

    @Test
    void shouldRegisterShellContentHostBeforeInitialDashboardNavigation() throws Exception {
        SpyGerenciadorNavegacao navigationManager = new SpyGerenciadorNavegacao();
        ControladorEstruturaAplicacao controller = new ControladorEstruturaAplicacao(navigationManager, new SpyFluxoAplicacao());
        BorderPane contentHost = new BorderPane();
        setField(controller, "contentHost", contentHost);

        controller.initialize();

        assertEquals(contentHost, navigationManager.registeredHost);
        assertEquals(VisaoAplicacao.DASHBOARD, navigationManager.lastView);
    }

    @Test
    void shouldNavigateThroughMainMenuActions() throws Exception {
        SpyGerenciadorNavegacao navigationManager = new SpyGerenciadorNavegacao();
        ControladorEstruturaAplicacao controller = new ControladorEstruturaAplicacao(navigationManager, new SpyFluxoAplicacao());

        invoke(controller, "onCadastrosClick");
        assertEquals(VisaoAplicacao.REGISTRY, navigationManager.lastView);

        invoke(controller, "onClientesClick");
        assertEquals(VisaoAplicacao.CUSTOMERS, navigationManager.lastView);

        invoke(controller, "onFinanceiroClick");
        assertEquals(VisaoAplicacao.FINANCE, navigationManager.lastView);

        invoke(controller, "onServicosClick");
        assertEquals(VisaoAplicacao.SERVICES, navigationManager.lastView);

        invoke(controller, "onAgendaClick");
        assertEquals(VisaoAplicacao.AGENDA, navigationManager.lastView);

        invoke(controller, "onOrdemServicoClick");
        assertEquals(VisaoAplicacao.SERVICE_ORDER, navigationManager.lastView);

        invoke(controller, "onEstoqueClick");
        assertEquals(VisaoAplicacao.INVENTORY, navigationManager.lastView);

        invoke(controller, "onContratosCertificadosClick");
        assertEquals(VisaoAplicacao.CONTRACTS_CERTIFICATES, navigationManager.lastView);
    }

    @Test
    void shouldLogoutFromMainMenu() throws Exception {
        SpyFluxoAplicacao applicationFlow = new SpyFluxoAplicacao();
        SpyGerenciadorNavegacao navigationManager = new SpyGerenciadorNavegacao();
        ControladorEstruturaAplicacao controller = new ControladorEstruturaAplicacao(navigationManager, applicationFlow);

        invoke(controller, "onLogoutClick");

        assertNull(navigationManager.registeredHost);
        assertTrue(applicationFlow.loginShown);
    }

    private void invoke(ControladorEstruturaAplicacao controller, String methodName) throws Exception {
        Method method = ControladorEstruturaAplicacao.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(controller);
    }

    private void setField(ControladorEstruturaAplicacao controller, String fieldName, Object value) throws Exception {
        Field field = ControladorEstruturaAplicacao.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    private static final class SpyGerenciadorNavegacao extends GerenciadorNavegacao {

        private VisaoAplicacao lastView;
        private BorderPane registeredHost;

        private SpyGerenciadorNavegacao() {
            super(null, null);
        }

        @Override
        public void navigateTo(VisaoAplicacao view) {
            this.lastView = view;
        }

        @Override
        public void registerShellContentHost(BorderPane shellContentHost) {
            this.registeredHost = shellContentHost;
        }
    }

    private static final class SpyFluxoAplicacao extends FluxoAplicacao {

        private boolean loginShown;

        private SpyFluxoAplicacao() {
            super(null, null);
        }

        @Override
        public void showLogin() {
            loginShown = true;
        }
    }
}

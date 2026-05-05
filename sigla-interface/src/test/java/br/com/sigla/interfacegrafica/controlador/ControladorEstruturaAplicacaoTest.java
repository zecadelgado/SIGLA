package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.interfacegrafica.aplicativo.FluxoAplicacao;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        invoke(controller, "onOrdemServicoClick");
        assertEquals(VisaoAplicacao.SERVICE_ORDER, navigationManager.lastView);

        invoke(controller, "onEstoqueClick");
        assertEquals(VisaoAplicacao.INVENTORY, navigationManager.lastView);

        invoke(controller, "onContratosClick");
        assertEquals(VisaoAplicacao.CONTRACTS, navigationManager.lastView);

        invoke(controller, "onCertificadosClick");
        assertEquals(VisaoAplicacao.CERTIFICATES, navigationManager.lastView);

        invoke(controller, "onNotificacoesClick");
        assertEquals(VisaoAplicacao.NOTIFICATIONS, navigationManager.lastView);

        invoke(controller, "onUsuariosClick");
        assertEquals(VisaoAplicacao.USERS_ADMIN, navigationManager.lastView);
    }

    @Test
    void shouldLogoutFromMainMenu() throws Exception {
        SpyFluxoAplicacao applicationFlow = new SpyFluxoAplicacao();
        ControladorEstruturaAplicacao controller = new ControladorEstruturaAplicacao(new SpyGerenciadorNavegacao(), applicationFlow);

        invoke(controller, "onLogoutClick");

        assertTrue(applicationFlow.loginShown);
    }

    private void invoke(ControladorEstruturaAplicacao controller, String methodName) throws Exception {
        Method method = ControladorEstruturaAplicacao.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(controller);
    }

    private static final class SpyGerenciadorNavegacao extends GerenciadorNavegacao {

        private VisaoAplicacao lastView;

        private SpyGerenciadorNavegacao() {
            super(null, null);
        }

        @Override
        public void navigateTo(VisaoAplicacao view) {
            this.lastView = view;
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

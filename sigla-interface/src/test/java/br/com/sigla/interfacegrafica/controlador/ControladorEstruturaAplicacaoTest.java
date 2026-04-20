package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControladorEstruturaAplicacaoTest {

    @Test
    void shouldNavigateToDashboardFromMainMenu() throws Exception {
        SpyGerenciadorNavegacao navigationManager = new SpyGerenciadorNavegacao();
        ControladorEstruturaAplicacao controller = new ControladorEstruturaAplicacao(navigationManager);

        invoke(controller, "onDashboardClick");
        assertEquals(VisaoAplicacao.DASHBOARD, navigationManager.lastView);
    }

    @Test
    void shouldNavigateThroughMainMenuActions() throws Exception {
        SpyGerenciadorNavegacao navigationManager = new SpyGerenciadorNavegacao();
        ControladorEstruturaAplicacao controller = new ControladorEstruturaAplicacao(navigationManager);

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
}

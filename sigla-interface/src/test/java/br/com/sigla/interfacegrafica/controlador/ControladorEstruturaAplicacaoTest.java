package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ControladorEstruturaAplicacaoTest {

    @Test
    void shouldBindHostAndOpenDashboardByDefault() throws Exception {
        ControladorEstruturaAplicacao controller = new ControladorEstruturaAplicacao();
        StackPane host = new StackPane();
        SpyGerenciadorNavegacao navigationManager = new SpyGerenciadorNavegacao();

        Field contentHostField = ControladorEstruturaAplicacao.class.getDeclaredField("contentHost");
        contentHostField.setAccessible(true);
        contentHostField.set(controller, host);

        controller.bindNavigation(navigationManager);

        assertSame(host, navigationManager.boundHost);
        assertEquals(VisaoAplicacao.DASHBOARD, navigationManager.lastView);
    }

    private static final class SpyGerenciadorNavegacao extends GerenciadorNavegacao {

        private StackPane boundHost;
        private VisaoAplicacao lastView;

        private SpyGerenciadorNavegacao() {
            super(null);
        }

        @Override
        public void bindHost(StackPane host) {
            this.boundHost = host;
        }

        @Override
        public void navigateTo(VisaoAplicacao view) {
            this.lastView = view;
        }
    }
}

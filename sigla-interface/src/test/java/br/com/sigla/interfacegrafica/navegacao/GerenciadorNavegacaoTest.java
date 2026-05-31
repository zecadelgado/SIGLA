package br.com.sigla.interfacegrafica.navegacao;

import br.com.sigla.interfacegrafica.aplicativo.FluxoAplicacao;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GerenciadorNavegacaoTest {

    @Test
    void shouldOpenFloatingViewsThroughApplicationFlowEvenWhenShellHostExists() {
        for (VisaoAplicacao view : VisaoAplicacao.values()) {
            if (!view.isSobreposta()) {
                continue;
            }

            SpyFluxoAplicacao fluxoAplicacao = new SpyFluxoAplicacao();
            GerenciadorNavegacao gerenciadorNavegacao = new GerenciadorNavegacao(null, fluxoAplicacao);
            BorderPane shellContentHost = new BorderPane();

            gerenciadorNavegacao.registerShellContentHost(shellContentHost);
            gerenciadorNavegacao.navigateTo(view);

            assertEquals(view, fluxoAplicacao.lastView);
            assertNull(shellContentHost.getCenter());
        }
    }

    @Test
    void shouldMakeShellContentResponsiveToHostSize() {
        GerenciadorNavegacao gerenciadorNavegacao = new GerenciadorNavegacao(null, null);
        VBox innerContent = new VBox();
        ScrollPane scrollPane = new ScrollPane(innerContent);

        gerenciadorNavegacao.configureResponsiveContent(scrollPane);

        assertTrue(scrollPane.isFitToWidth());
        assertTrue(scrollPane.isFitToHeight());
        assertEquals(0.0, scrollPane.getMinWidth());
        assertEquals(0.0, scrollPane.getMinHeight());
        assertEquals(Double.MAX_VALUE, scrollPane.getMaxWidth());
        assertEquals(Double.MAX_VALUE, scrollPane.getMaxHeight());
        assertEquals(0.0, innerContent.getMinWidth());
        assertEquals(0.0, innerContent.getMinHeight());
        assertEquals(Double.MAX_VALUE, innerContent.getMaxWidth());
        assertEquals(Double.MAX_VALUE, innerContent.getMaxHeight());
    }

    private static final class SpyFluxoAplicacao extends FluxoAplicacao {

        private VisaoAplicacao lastView;

        private SpyFluxoAplicacao() {
            super(null, null);
        }

        @Override
        public void showView(VisaoAplicacao view) {
            this.lastView = view;
        }
    }
}

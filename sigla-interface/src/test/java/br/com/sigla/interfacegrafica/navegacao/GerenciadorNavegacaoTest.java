package br.com.sigla.interfacegrafica.navegacao;

import br.com.sigla.interfacegrafica.aplicativo.FluxoAplicacao;
import javafx.scene.layout.BorderPane;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

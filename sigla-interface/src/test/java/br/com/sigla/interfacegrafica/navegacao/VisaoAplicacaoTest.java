package br.com.sigla.interfacegrafica.navegacao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisaoAplicacaoTest {

    @Test
    void shouldExposeFxmlPath() {
        assertEquals("/fxml/telas/TelaDashboard.fxml", VisaoAplicacao.DASHBOARD.fxmlPath());
        assertTrue(VisaoAplicacao.CUSTOMERS.fxmlPath().startsWith("/fxml/telas/"));
    }

    @Test
    void shouldKeepCanonicalRoutesInsideTelaFolder() {
        assertEquals(20, VisaoAplicacao.values().length);
        for (VisaoAplicacao view : VisaoAplicacao.values()) {
            assertTrue(view.fxmlPath().startsWith("/fxml/telas/"));
        }
    }

    @Test
    void shouldMarkLoginAndShellAsNonShellContentOnly() {
        assertFalse(VisaoAplicacao.LOGIN.isShellContent());
        assertFalse(VisaoAplicacao.SHELL.isShellContent());
        assertTrue(VisaoAplicacao.DASHBOARD.isShellContent());
        assertTrue(VisaoAplicacao.NEW_SERVICE_ORDER.isShellContent());
    }

    @Test
    void shouldMarkNewViewsAsFloatingWindows() {
        assertTrue(VisaoAplicacao.NEW_REGISTRY.isSobreposta());
        assertTrue(VisaoAplicacao.NEW_TRANSACTION.isSobreposta());
        assertFalse(VisaoAplicacao.LOGIN.isSobreposta());
        assertFalse(VisaoAplicacao.CUSTOMERS.isSobreposta());
    }
}


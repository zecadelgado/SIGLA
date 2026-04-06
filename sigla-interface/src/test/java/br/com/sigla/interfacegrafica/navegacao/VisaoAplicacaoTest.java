package br.com.sigla.interfacegrafica.navegacao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VisaoAplicacaoTest {

    @Test
    void shouldExposeFxmlPath() {
        assertTrue(VisaoAplicacao.CUSTOMERS.fxmlPath().startsWith("/fxml/"));
    }
}


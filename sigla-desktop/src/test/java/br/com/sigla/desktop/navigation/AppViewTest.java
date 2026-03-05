package br.com.sigla.desktop.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AppViewTest {

    @Test
    void shouldExposeFxmlPath() {
        assertTrue(AppView.INVENTORY.fxmlPath().startsWith("/fxml/"));
    }
}

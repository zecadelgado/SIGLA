package br.com.sigla.interfacegrafica.fxml;

import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelaFxmlResourcesTest {

    @Test
    void shouldKeepEveryCanonicalFxmlAvailableAndBoundToAController() throws IOException {
        for (VisaoAplicacao view : VisaoAplicacao.values()) {
            try (InputStream stream = VisaoAplicacao.class.getResourceAsStream(view.fxmlPath())) {
                assertNotNull(stream, "Missing FXML for " + view);
                String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                assertFalse(content.contains("CONTROLLER_NAME"), "Placeholder controller left in " + view);
                assertTrue(content.contains("fx:controller=\"br.com.sigla.interfacegrafica.controlador."),
                        "Expected real controller binding in " + view);
            }
        }
    }

    @Test
    void shouldRemoveRetiredDuplicateScreensFromSourceTree() {
        assertFalse(Files.exists(Path.of("src/main/resources/fxml/telas/Financeiro.fxml")));
        assertFalse(Files.exists(Path.of("src/main/resources/fxml/telas/OrdemdeServiço.fxml")));
        assertFalse(Files.exists(Path.of("src/main/resources/fxml/telas/TelaEmConstrucao.fxml")));
    }
}

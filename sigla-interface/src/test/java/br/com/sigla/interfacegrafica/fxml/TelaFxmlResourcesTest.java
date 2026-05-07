package br.com.sigla.interfacegrafica.fxml;

import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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

    @Test
    void shouldKeepFxmlBindingsResolvableInControllers() throws Exception {
        Pattern controllerPattern = Pattern.compile("fx:controller=\"([^\"]+)\"");
        Pattern idPattern = Pattern.compile("fx:id=\"([^\"]+)\"");
        Pattern actionPattern = Pattern.compile("onAction=\"#([^\"]+)\"");

        for (VisaoAplicacao view : VisaoAplicacao.values()) {
            try (InputStream stream = VisaoAplicacao.class.getResourceAsStream(view.fxmlPath())) {
                assertNotNull(stream, "Missing FXML for " + view);
                String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                var controllerMatcher = controllerPattern.matcher(content);
                assertTrue(controllerMatcher.find(), "Missing controller in " + view);
                Class<?> controllerClass = Class.forName(controllerMatcher.group(1));

                var idMatcher = idPattern.matcher(content);
                while (idMatcher.find()) {
                    if (idMatcher.group(1).startsWith("txt")) {
                        continue;
                    }
                    assertTrue(hasField(controllerClass, idMatcher.group(1)),
                            "Missing @FXML field " + idMatcher.group(1) + " in " + controllerClass.getSimpleName());
                }

                var actionMatcher = actionPattern.matcher(content);
                while (actionMatcher.find()) {
                    assertTrue(hasNoArgMethod(controllerClass, actionMatcher.group(1)),
                            "Missing onAction method " + actionMatcher.group(1) + " in " + controllerClass.getSimpleName());
                }
            }
        }
    }

    @Test
    void shouldExposeFunctionalControllerFieldsInRestoredScreens() throws IOException {
        Map<VisaoAplicacao, List<String>> requiredFields = Map.of(
                VisaoAplicacao.DASHBOARD, List.of(
                        "osAbertasLabel", "servicosAtrasadosLabel", "contasVencidasLabel", "estoqueBaixoLabel",
                        "indicacoesPendentesLabel", "receitasPeriodoLabel", "despesasPeriodoLabel", "saldoAtualLabel",
                        "receberLabel", "pagarLabel", "financeiroChart", "estoqueChart", "servicosChart"
                ),
                VisaoAplicacao.REGISTRY, List.of(
                        "searchField", "ativoFiltroChoice", "cadastroTable", "nomeColumn", "cpfColumn", "cnpjColumn",
                        "razaoSocialColumn", "telefoneColumn", "emailColumn", "cepColumn", "cidadeColumn", "statusColumn",
                        "responsaveisTable", "responsavelNomeColumn", "responsavelCargoColumn", "responsavelTelefoneColumn",
                        "responsavelEmailColumn", "responsavelPrincipalColumn"
                ),
                VisaoAplicacao.CUSTOMERS, List.of(
                        "totalClientesLabel", "totalIndicacoesLabel", "faturamentoLabel", "indicacoesPendentesLabel",
                        "rankingTable", "rankingClienteColumn", "rankingEmailColumn", "rankingServicosColumn",
                        "rankingFaturamentoColumn", "rankingIndicacoesColumn", "indicacoesTable", "indicacaoNomeColumn",
                        "indicacaoContatoColumn", "indicacaoClienteColumn", "indicacaoDataColumn", "indicacaoStatusColumn",
                        "indicacaoBuscaField", "indicacaoClienteFiltroField", "indicacaoStatusFiltro",
                        "indicacaoInicioPicker", "indicacaoFimPicker"
                ),
                VisaoAplicacao.NEW_INDICATION, List.of("observacoesArea", "feedbackLabel"),
                VisaoAplicacao.NEW_PRODUCT, List.of("skuField", "unidadeCombo", "feedbackLabel"),
                VisaoAplicacao.NEW_SERVICE_ORDER, List.of("feedbackLabel")
        );

        for (Map.Entry<VisaoAplicacao, List<String>> entry : requiredFields.entrySet()) {
            try (InputStream stream = VisaoAplicacao.class.getResourceAsStream(entry.getKey().fxmlPath())) {
                assertNotNull(stream, "Missing FXML for " + entry.getKey());
                String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                for (String field : entry.getValue()) {
                    assertTrue(content.contains("fx:id=\"" + field + "\""),
                            "Missing functional fx:id " + field + " in " + entry.getKey());
                }
            }
        }
    }

    private boolean hasField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getName().equals(name)) {
                    return true;
                }
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private boolean hasNoArgMethod(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == 0) {
                    return true;
                }
            }
            current = current.getSuperclass();
        }
        return Arrays.stream(type.getMethods())
                .anyMatch(method -> method.getName().equals(name) && method.getParameterCount() == 0);
    }
}

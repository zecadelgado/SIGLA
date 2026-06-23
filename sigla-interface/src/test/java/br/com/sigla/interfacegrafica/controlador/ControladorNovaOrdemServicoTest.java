package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.interfacegrafica.consulta.ContextoEdicaoOrdemServico;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaReferencias;
import br.com.sigla.interfacegrafica.formatador.FormatadorMascaraMoeda;
import br.com.sigla.interfacegrafica.modelo.OpcaoId;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.util.UtilComboBox;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControladorNovaOrdemServicoTest {

    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void mantemContratoSelecionadoQuandoClientePossuiOContrato() throws Exception {
        runFx(() -> {
            ComboBox<OpcaoId> clienteCombo = new ComboBox<>();
            ComboBox<OpcaoId> contratoCombo = new ComboBox<>();
            ControladorNovaOrdemServico controlador = controlador(new ReferenciasFake());
            setField(controlador, "clienteCombo", clienteCombo);
            setField(controlador, "contratoCombo", contratoCombo);

            controlador.initialize();
            UtilComboBox.selecionarPorId(contratoCombo, "CTR-1");
            UtilComboBox.selecionarPorId(clienteCombo, "CLI-1");

            assertEquals("CTR-1", UtilComboBox.idSelecionado(contratoCombo));
            assertEquals(List.of("", "CTR-1"), contratoCombo.getItems().stream().map(OpcaoId::id).toList());
        });
    }

    @Test
    void mantemContratosDisponiveisQuandoFiltroDoClienteVemVazio() throws Exception {
        runFx(() -> {
            ComboBox<OpcaoId> clienteCombo = new ComboBox<>();
            ComboBox<OpcaoId> contratoCombo = new ComboBox<>();
            ControladorNovaOrdemServico controlador = controlador(new ReferenciasFake());
            setField(controlador, "clienteCombo", clienteCombo);
            setField(controlador, "contratoCombo", contratoCombo);

            controlador.initialize();
            UtilComboBox.selecionarPorId(clienteCombo, "CLI-SEM-CONTRATO");

            assertEquals("", UtilComboBox.idSelecionado(contratoCombo));
            assertEquals(List.of("", "CTR-1", "CTR-2"), contratoCombo.getItems().stream().map(OpcaoId::id).toList());
        });
    }

    private ControladorNovaOrdemServico controlador(ServicoConsultaReferencias referencias) {
        return new ControladorNovaOrdemServico(
                proxy(CasoDeUsoOrdemServico.class),
                referencias,
                new SpyGerenciadorNavegacao(),
                new FormatadorMascaraMoeda(),
                new ContextoEdicaoOrdemServico()
        );
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getReturnType() == List.class) {
                        return List.of();
                    }
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    if (method.getReturnType() == int.class || method.getReturnType() == long.class) {
                        return 0;
                    }
                    return null;
                }
        );
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void runFx(ThrowingRunnable runnable) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> erro = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                erro.set(throwable);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        Throwable throwable = erro.get();
        if (throwable instanceof Exception exception) {
            throw exception;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        if (throwable != null) {
            throw new RuntimeException(throwable);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class ReferenciasFake extends ServicoConsultaReferencias {

        private ReferenciasFake() {
            super(null, null, null, null, null, null);
        }

        @Override
        public List<OpcaoId> clientes() {
            return List.of(
                    new OpcaoId("CLI-1", "Cliente 1"),
                    new OpcaoId("CLI-SEM-CONTRATO", "Cliente sem contrato")
            );
        }

        @Override
        public List<OpcaoId> funcionarios() {
            return List.of();
        }

        @Override
        public List<OpcaoId> contratos() {
            return List.of(
                    new OpcaoId("CTR-1", "Cliente 1 - Mensal"),
                    new OpcaoId("CTR-2", "Cliente 2 - Avulso")
            );
        }

        @Override
        public List<OpcaoId> contratosDoCliente(String clienteId) {
            if ("CLI-1".equals(clienteId)) {
                return List.of(new OpcaoId("CTR-1", "Cliente 1 - Mensal"));
            }
            if ("CLI-SEM-CONTRATO".equals(clienteId)) {
                return List.of();
            }
            return contratos();
        }
    }

    private static final class SpyGerenciadorNavegacao extends GerenciadorNavegacao {
        private SpyGerenciadorNavegacao() {
            super(null, null);
        }
    }
}

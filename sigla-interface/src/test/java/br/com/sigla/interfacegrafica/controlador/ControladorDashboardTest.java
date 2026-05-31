package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.potenciaisclientes.porta.entrada.CasoDeUsoPotencialCliente;
import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.dominio.estoque.ItemEstoque;
import br.com.sigla.dominio.financeiro.LancamentoFinanceiro;
import br.com.sigla.dominio.potenciaisclientes.PotencialCliente;
import br.com.sigla.dominio.servicos.OrdemServico;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorMoeda;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import javafx.application.Platform;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControladorDashboardTest {

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
    void shouldPopulateDashboardChartsFromRealUseCases() throws Exception {
        LocalDate hoje = LocalDate.now();
        var financeiroChart = lineChart();
        var estoqueChart = barChart();
        var servicosChart = barChart();
        ControladorDashboard controller = controller(
                List.of(
                        lancamento("REC-ANT", LancamentoFinanceiro.Tipo.ENTRY, new BigDecimal("200.00"), hoje.minusMonths(1), LancamentoFinanceiro.Status.PAID),
                        lancamento("REC-ATUAL", LancamentoFinanceiro.Tipo.ENTRY, new BigDecimal("300.00"), hoje, LancamentoFinanceiro.Status.PAID),
                        lancamento("REC-PEND", LancamentoFinanceiro.Tipo.ENTRY, new BigDecimal("999.00"), null, LancamentoFinanceiro.Status.PENDING),
                        lancamento("DESP-ATUAL", LancamentoFinanceiro.Tipo.EXPENSE, new BigDecimal("150.00"), hoje, LancamentoFinanceiro.Status.PAID)
                ),
                List.of(
                        item("P1", "Produto 1", 10),
                        item("P2", "Produto 2", 50),
                        item("P3", "Produto 3", 30),
                        item("P4", "Produto 4", 70),
                        item("P5", "Produto 5", 90),
                        item("P6", "Produto 6", 20)
                ),
                List.of(
                        ordem("OS-1", OrdemServico.OrdemServicoStatus.ABERTA),
                        ordem("OS-2", OrdemServico.OrdemServicoStatus.ABERTA),
                        ordem("OS-3", OrdemServico.OrdemServicoStatus.CONCLUIDA)
                )
        );
        setField(controller, "financeiroChart", financeiroChart);
        setField(controller, "estoqueChart", estoqueChart);
        setField(controller, "servicosChart", servicosChart);

        controller.refresh();

        assertEquals(6, financeiroChart.getData().getFirst().getData().size());
        assertEquals(0, new BigDecimal("300.00").compareTo((BigDecimal) financeiroChart.getData().getFirst().getData().get(5).getYValue()));
        assertEquals(5, estoqueChart.getData().getFirst().getData().size());
        assertEquals("Produto 5", estoqueChart.getData().getFirst().getData().getFirst().getXValue());
        assertEquals(90, estoqueChart.getData().getFirst().getData().getFirst().getYValue().intValue());
        assertEquals(2, servicosChart.getData().getFirst().getData().stream()
                .filter(data -> "Aberta".equals(data.getXValue()))
                .findFirst()
                .orElseThrow()
                .getYValue()
                .intValue());
    }

    @Test
    void shouldRenderEmptyDashboardChartsWithoutErrors() throws Exception {
        var financeiroChart = lineChart();
        var estoqueChart = barChart();
        var servicosChart = barChart();
        ControladorDashboard controller = controller(List.of(), List.of(), List.of());
        setField(controller, "financeiroChart", financeiroChart);
        setField(controller, "estoqueChart", estoqueChart);
        setField(controller, "servicosChart", servicosChart);

        controller.refresh();

        assertEquals(6, financeiroChart.getData().getFirst().getData().size());
        assertEquals(0, estoqueChart.getData().getFirst().getData().size());
        assertEquals(OrdemServico.OrdemServicoStatus.values().length, servicosChart.getData().getFirst().getData().size());
    }

    private ControladorDashboard controller(
            List<LancamentoFinanceiro> lancamentos,
            List<ItemEstoque> itens,
            List<OrdemServico> ordens
    ) {
        return new ControladorDashboard(
                proxy(CasoDeUsoOrdemServico.class, Map.of("listAll", ordens)),
                proxy(CasoDeUsoFinanceiro.class, Map.of("listLancamentos", lancamentos)),
                proxy(CasoDeUsoEstoque.class, Map.of("listAll", itens)),
                proxy(CasoDeUsoPotencialCliente.class, Map.of("listAll", List.<PotencialCliente>of())),
                new SpyGerenciadorNavegacao(),
                new ApresentadorMoeda()
        );
    }

    private LineChart<String, Number> lineChart() {
        return new LineChart<>(new CategoryAxis(), new NumberAxis());
    }

    private BarChart<String, Number> barChart() {
        return new BarChart<>(new CategoryAxis(), new NumberAxis());
    }

    private LancamentoFinanceiro lancamento(
            String id,
            LancamentoFinanceiro.Tipo tipo,
            BigDecimal valor,
            LocalDate dataPagamento,
            LancamentoFinanceiro.Status status
    ) {
        LocalDate emissao = dataPagamento == null ? LocalDate.now() : dataPagamento;
        return new LancamentoFinanceiro(
                id,
                tipo,
                "",
                tipo.name(),
                "",
                "",
                "Lancamento " + id,
                "",
                "",
                valor,
                emissao,
                emissao,
                dataPagamento,
                status,
                false,
                1,
                "",
                "",
                List.of()
        );
    }

    private ItemEstoque item(String id, String nome, int quantidade) {
        return new ItemEstoque(id, nome, "", "", BigDecimal.ONE, BigDecimal.ONE, quantidade, 1, "un", true, List.of());
    }

    private OrdemServico ordem(String id, OrdemServico.OrdemServicoStatus status) {
        return new OrdemServico(
                id,
                1L,
                "CUS-1",
                "Servico " + id,
                "Descricao",
                "Controle",
                status,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                "",
                "",
                status == OrdemServico.OrdemServicoStatus.CONCLUIDA,
                false,
                BigDecimal.TEN,
                ""
        );
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, Map<String, Object> returns) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (returns.containsKey(method.getName())) {
                        return returns.get(method.getName());
                    }
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

    private static final class SpyGerenciadorNavegacao extends GerenciadorNavegacao {
        private SpyGerenciadorNavegacao() {
            super(null, null);
        }
    }
}

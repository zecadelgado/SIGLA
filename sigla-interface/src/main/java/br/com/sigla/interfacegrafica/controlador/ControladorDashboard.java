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
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ControladorDashboard {

    private static final DateTimeFormatter MES_FORMATTER = DateTimeFormatter.ofPattern("MMM", Locale.of("pt", "BR"));

    private final CasoDeUsoOrdemServico casoDeUsoOrdemServico;
    private final CasoDeUsoFinanceiro casoDeUsoFinanceiro;
    private final CasoDeUsoEstoque casoDeUsoEstoque;
    private final CasoDeUsoPotencialCliente casoDeUsoPotencialCliente;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final ApresentadorMoeda apresentadorMoeda;

    @FXML
    private Label osAbertasLabel;
    @FXML
    private Label servicosAtrasadosLabel;
    @FXML
    private Label contasVencidasLabel;
    @FXML
    private Label estoqueBaixoLabel;
    @FXML
    private Label indicacoesPendentesLabel;
    @FXML
    private Label receitasPeriodoLabel;
    @FXML
    private Label despesasPeriodoLabel;
    @FXML
    private Label saldoAtualLabel;
    @FXML
    private Label receberLabel;
    @FXML
    private Label pagarLabel;
    @FXML
    private LineChart<String, Number> financeiroChart;
    @FXML
    private BarChart<String, Number> estoqueChart;
    @FXML
    private BarChart<String, Number> servicosChart;

    public ControladorDashboard(
            CasoDeUsoOrdemServico casoDeUsoOrdemServico,
            CasoDeUsoFinanceiro casoDeUsoFinanceiro,
            CasoDeUsoEstoque casoDeUsoEstoque,
            CasoDeUsoPotencialCliente casoDeUsoPotencialCliente,
            GerenciadorNavegacao gerenciadorNavegacao,
            ApresentadorMoeda apresentadorMoeda
    ) {
        this.casoDeUsoOrdemServico = casoDeUsoOrdemServico;
        this.casoDeUsoFinanceiro = casoDeUsoFinanceiro;
        this.casoDeUsoEstoque = casoDeUsoEstoque;
        this.casoDeUsoPotencialCliente = casoDeUsoPotencialCliente;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
        this.apresentadorMoeda = apresentadorMoeda;
    }

    @FXML
    public void initialize() {
        configurarAtalhos();
        refresh();
    }

    public void refresh() {
        LocalDate hoje = LocalDate.now();
        YearMonth mesAtual = YearMonth.from(hoje);
        List<OrdemServico> ordens = casoDeUsoOrdemServico.listAll();
        List<LancamentoFinanceiro> lancamentos = casoDeUsoFinanceiro.listLancamentos(null);
        List<ItemEstoque> itens = casoDeUsoEstoque.listAll();
        List<PotencialCliente> indicacoes = casoDeUsoPotencialCliente.listAll();

        long osAbertas = ordens.stream()
                .filter(os -> os.status() != OrdemServico.OrdemServicoStatus.CONCLUIDA)
                .filter(os -> os.status() != OrdemServico.OrdemServicoStatus.CANCELADA)
                .count();
        long servicosAtrasados = ordens.stream()
                .filter(os -> os.status() != OrdemServico.OrdemServicoStatus.CONCLUIDA)
                .filter(os -> os.status() != OrdemServico.OrdemServicoStatus.CANCELADA)
                .filter(os -> os.dataAgendada() != null && os.dataAgendada().toLocalDate().isBefore(hoje))
                .count();
        long contasVencidas = lancamentos.stream()
                .filter(lancamento -> lancamento.vencido(hoje) || lancamento.parcelas().stream().anyMatch(parcela -> parcela.vencida(hoje)))
                .count();
        long estoqueBaixo = itens.stream()
                .filter(item -> item.ativo() && item.isLowStock())
                .count();
        long indicacoesPendentes = indicacoes.stream()
                .filter(indicacao -> !indicacao.status().isEncerrado())
                .count();
        BigDecimal receitas = soma(lancamentos, mesAtual, LancamentoFinanceiro.Tipo.ENTRY, true);
        BigDecimal despesas = soma(lancamentos, mesAtual, LancamentoFinanceiro.Tipo.EXPENSE, true);
        BigDecimal receber = somaAberto(lancamentos, LancamentoFinanceiro.Tipo.ENTRY);
        BigDecimal pagar = somaAberto(lancamentos, LancamentoFinanceiro.Tipo.EXPENSE);

        setText(osAbertasLabel, String.valueOf(osAbertas));
        setText(servicosAtrasadosLabel, String.valueOf(servicosAtrasados));
        setText(contasVencidasLabel, String.valueOf(contasVencidas));
        setText(estoqueBaixoLabel, String.valueOf(estoqueBaixo));
        setText(indicacoesPendentesLabel, String.valueOf(indicacoesPendentes));
        setText(receitasPeriodoLabel, apresentadorMoeda.format(receitas));
        setText(despesasPeriodoLabel, apresentadorMoeda.format(despesas));
        setText(saldoAtualLabel, apresentadorMoeda.format(receitas.subtract(despesas)));
        setText(receberLabel, apresentadorMoeda.format(receber));
        setText(pagarLabel, apresentadorMoeda.format(pagar));
        carregarGraficoFinanceiro(lancamentos, mesAtual);
        carregarGraficoEstoque(itens);
        carregarGraficoServicos(ordens);
    }

    private BigDecimal soma(List<LancamentoFinanceiro> lancamentos, YearMonth mes, LancamentoFinanceiro.Tipo tipo, boolean somentePago) {
        return lancamentos.stream()
                .filter(lancamento -> lancamento.tipo() == tipo)
                .filter(lancamento -> !somentePago || lancamento.status() == LancamentoFinanceiro.Status.PAID)
                .filter(lancamento -> lancamento.dataPagamento() != null && YearMonth.from(lancamento.dataPagamento()).equals(mes))
                .map(LancamentoFinanceiro::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somaAberto(List<LancamentoFinanceiro> lancamentos, LancamentoFinanceiro.Tipo tipo) {
        return lancamentos.stream()
                .filter(lancamento -> lancamento.tipo() == tipo)
                .filter(lancamento -> lancamento.status() != LancamentoFinanceiro.Status.PAID)
                .filter(lancamento -> lancamento.status() != LancamentoFinanceiro.Status.CANCELLED)
                .map(lancamento -> lancamento.valorTotal().subtract(lancamento.valorPago()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void setText(Label label, String text) {
        if (label != null) {
            label.setText(text);
        }
    }

    private void configurarAtalhos() {
        navegar(osAbertasLabel, VisaoAplicacao.SERVICE_ORDER);
        navegar(servicosAtrasadosLabel, VisaoAplicacao.SERVICE_ORDER);
        navegar(contasVencidasLabel, VisaoAplicacao.FINANCE);
        navegar(estoqueBaixoLabel, VisaoAplicacao.INVENTORY);
        navegar(indicacoesPendentesLabel, VisaoAplicacao.CUSTOMERS);
        navegar(receitasPeriodoLabel, VisaoAplicacao.FINANCE);
        navegar(despesasPeriodoLabel, VisaoAplicacao.FINANCE);
        navegar(saldoAtualLabel, VisaoAplicacao.FINANCE);
        navegar(receberLabel, VisaoAplicacao.FINANCE);
        navegar(pagarLabel, VisaoAplicacao.FINANCE);
    }

    private void carregarGraficoFinanceiro(List<LancamentoFinanceiro> lancamentos, YearMonth mesAtual) {
        if (financeiroChart == null) {
            return;
        }
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (int offset = 5; offset >= 0; offset--) {
            YearMonth mes = mesAtual.minusMonths(offset);
            BigDecimal total = soma(lancamentos, mes, LancamentoFinanceiro.Tipo.ENTRY, true);
            serie.getData().add(new XYChart.Data<>(rotuloMes(mes), total));
        }
        financeiroChart.getData().setAll(serie);
        ajustarEixoNumerico(financeiroChart, serie);
    }

    private void carregarGraficoEstoque(List<ItemEstoque> itens) {
        if (estoqueChart == null) {
            return;
        }
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        itens.stream()
                .filter(ItemEstoque::ativo)
                .sorted(Comparator.comparingInt(ItemEstoque::quantity).reversed())
                .limit(5)
                .forEach(item -> serie.getData().add(new XYChart.Data<>(rotuloItem(item.name()), item.quantity())));
        estoqueChart.getData().setAll(serie);
        ajustarEixoNumerico(estoqueChart, serie);
    }

    private void carregarGraficoServicos(List<OrdemServico> ordens) {
        if (servicosChart == null) {
            return;
        }
        Map<OrdemServico.OrdemServicoStatus, Long> porStatus = ordens.stream()
                .collect(Collectors.groupingBy(OrdemServico::status, Collectors.counting()));
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (OrdemServico.OrdemServicoStatus status : OrdemServico.OrdemServicoStatus.values()) {
            long total = porStatus.getOrDefault(status, 0L);
            if (total > 0 || ordens.isEmpty()) {
                serie.getData().add(new XYChart.Data<>(rotuloStatus(status), total));
            }
        }
        servicosChart.getData().setAll(serie);
        ajustarEixoNumerico(servicosChart, serie);
    }

    private String rotuloMes(YearMonth mes) {
        String rotulo = mes.format(MES_FORMATTER).replace(".", "");
        return rotulo.substring(0, 1).toUpperCase(Locale.ROOT) + rotulo.substring(1);
    }

    private String rotuloItem(String nome) {
        if (nome == null || nome.isBlank()) {
            return "-";
        }
        String trimmed = nome.trim();
        return trimmed.length() <= 12 ? trimmed : trimmed.substring(0, 12);
    }

    private String rotuloStatus(OrdemServico.OrdemServicoStatus status) {
        return switch (status) {
            case AGENDADA -> "Agendada";
            case ABERTA -> "Aberta";
            case EM_ANDAMENTO -> "Andamento";
            case CONCLUIDA -> "Concluida";
            case CANCELADA -> "Cancelada";
            case ATRASADA -> "Atrasada";
        };
    }

    private void ajustarEixoNumerico(XYChart<String, Number> chart, XYChart.Series<String, Number> serie) {
        if (!(chart.getYAxis() instanceof NumberAxis eixo)) {
            return;
        }
        double maximo = serie.getData().stream()
                .map(XYChart.Data::getYValue)
                .mapToDouble(Number::doubleValue)
                .max()
                .orElse(0.0);
        double limite = maximo <= 0.0 ? 1.0 : Math.ceil(maximo * 1.15);
        eixo.setAutoRanging(false);
        eixo.setLowerBound(0.0);
        eixo.setUpperBound(limite);
        eixo.setTickUnit(Math.max(1.0, Math.ceil(limite / 5.0)));
    }

    @FXML
    private void onAcessarFinanceiro() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.FINANCE);
    }

    @FXML
    private void onAcessarEstoque() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.INVENTORY);
    }

    @FXML
    private void onAcessarServicos() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.SERVICES);
    }

    private void navegar(Label label, VisaoAplicacao visao) {
        if (label != null) {
            label.setOnMouseClicked(event -> gerenciadorNavegacao.navigateTo(visao));
        }
    }
}

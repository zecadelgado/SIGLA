package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.potenciaisclientes.porta.entrada.CasoDeUsoPotencialCliente;
import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoServicoPrestado;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.dominio.potenciaisclientes.PotencialCliente;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorData;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorMoeda;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ControladorClientes extends ControladorComMenuPrincipal {

    private final CasoDeUsoCliente casoDeUsoCliente;
    private final CasoDeUsoPotencialCliente casoDeUsoPotencialCliente;
    private final CasoDeUsoServicoPrestado casoDeUsoServicoPrestado;
    private final CasoDeUsoFinanceiro casoDeUsoFinanceiro;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final ApresentadorMoeda apresentadorMoeda;
    private final ApresentadorData apresentadorData;

    @FXML
    private Label totalClientesLabel;
    @FXML
    private Label totalIndicacoesLabel;
    @FXML
    private Label faturamentoLabel;
    @FXML
    private Label indicacoesPendentesLabel;
    @FXML
    private TableView<ClienteRankingRow> rankingTable;
    @FXML
    private TableColumn<ClienteRankingRow, String> rankingClienteColumn;
    @FXML
    private TableColumn<ClienteRankingRow, String> rankingEmailColumn;
    @FXML
    private TableColumn<ClienteRankingRow, String> rankingServicosColumn;
    @FXML
    private TableColumn<ClienteRankingRow, String> rankingFaturamentoColumn;
    @FXML
    private TableColumn<ClienteRankingRow, String> rankingIndicacoesColumn;
    @FXML
    private TableView<IndicacaoRow> indicacoesTable;
    @FXML
    private TableColumn<IndicacaoRow, String> indicacaoNomeColumn;
    @FXML
    private TableColumn<IndicacaoRow, String> indicacaoContatoColumn;
    @FXML
    private TableColumn<IndicacaoRow, String> indicacaoClienteColumn;
    @FXML
    private TableColumn<IndicacaoRow, String> indicacaoDataColumn;
    @FXML
    private TableColumn<IndicacaoRow, String> indicacaoStatusColumn;

    public ControladorClientes(
            CasoDeUsoCliente casoDeUsoCliente,
            CasoDeUsoPotencialCliente casoDeUsoPotencialCliente,
            CasoDeUsoServicoPrestado casoDeUsoServicoPrestado,
            CasoDeUsoFinanceiro casoDeUsoFinanceiro,
            GerenciadorNavegacao gerenciadorNavegacao,
            ApresentadorMoeda apresentadorMoeda,
            ApresentadorData apresentadorData
    ) {
        super(gerenciadorNavegacao);
        this.casoDeUsoCliente = casoDeUsoCliente;
        this.casoDeUsoPotencialCliente = casoDeUsoPotencialCliente;
        this.casoDeUsoServicoPrestado = casoDeUsoServicoPrestado;
        this.casoDeUsoFinanceiro = casoDeUsoFinanceiro;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
        this.apresentadorMoeda = apresentadorMoeda;
        this.apresentadorData = apresentadorData;
    }

    @FXML
    public void initialize() {
        configureTables();
        refresh();
    }

    @FXML
    private void onNovaIndicacao() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.NEW_INDICATION);
    }

    private void refresh() {
        Map<String, Cliente> clienteMap = casoDeUsoCliente.listAll().stream()
                .collect(Collectors.toMap(Cliente::id, customer -> customer));
        Map<String, String> clientes = clienteMap.values().stream()
                .collect(Collectors.toMap(Cliente::id, Cliente::name));
        List<PotencialCliente> indicacoes = casoDeUsoPotencialCliente.listAll().stream()
                .filter(lead -> lead.origin().startsWith("INDICACAO"))
                .toList();
        Map<String, Long> indicacoesPorCliente = indicacoes.stream()
                .collect(Collectors.groupingBy(this::extractCustomerId, Collectors.counting()));
        Map<String, BigDecimal> faturamentoPorCliente = casoDeUsoFinanceiro.listTransactions().stream()
                .filter(transaction -> transaction.type() == CasoDeUsoFinanceiro.TransactionType.ENTRY)
                .collect(Collectors.groupingBy(
                        transaction -> transaction.customerId() == null ? "" : transaction.customerId(),
                        Collectors.reducing(BigDecimal.ZERO, CasoDeUsoFinanceiro.TransacaoFinanceiraView::amount, BigDecimal::add)
                ));
        Map<String, Long> servicosPorCliente = casoDeUsoServicoPrestado.listAll().stream()
                .collect(Collectors.groupingBy(service -> service.customerId(), Collectors.counting()));

        if (totalClientesLabel != null) {
            totalClientesLabel.setText(String.valueOf(clientes.size()));
        }
        if (totalIndicacoesLabel != null) {
            totalIndicacoesLabel.setText(String.valueOf(indicacoes.size()));
        }
        if (faturamentoLabel != null) {
            faturamentoLabel.setText(apresentadorMoeda.format(faturamentoPorCliente.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)));
        }
        if (indicacoesPendentesLabel != null) {
            indicacoesPendentesLabel.setText(String.valueOf(indicacoes.stream().filter(this::isPendingLead).count()));
        }

        if (rankingTable != null) {
            rankingTable.getItems().setAll(clientes.entrySet().stream()
                    .map(entry -> new ClienteRankingRow(
                            entry.getValue(),
                            extractContato(clienteMap.get(entry.getKey())),
                            servicosPorCliente.getOrDefault(entry.getKey(), 0L),
                            faturamentoPorCliente.getOrDefault(entry.getKey(), BigDecimal.ZERO),
                            indicacoesPorCliente.getOrDefault(entry.getKey(), 0L)
                    ))
                    .sorted((left, right) -> right.faturamento().compareTo(left.faturamento()))
                    .limit(10)
                    .toList());
        }

        if (indicacoesTable != null) {
            indicacoesTable.getItems().setAll(indicacoes.stream()
                    .map(lead -> new IndicacaoRow(
                            lead.name(),
                            lead.contact(),
                            clientes.getOrDefault(extractCustomerId(lead), "Origem externa"),
                            lead.interactionHistory().isEmpty() ? "-" : apresentadorData.format(lead.interactionHistory().getFirst().interactionDate()),
                            lead.status().name()
                    ))
                    .toList());
        }
    }

    private boolean isPendingLead(PotencialCliente lead) {
        return lead.status() != PotencialCliente.PotencialClienteStatus.WON
                && lead.status() != PotencialCliente.PotencialClienteStatus.LOST;
    }

    private String extractCustomerId(PotencialCliente lead) {
        String origin = lead.origin();
        if (origin == null || !origin.contains(":")) {
            return "";
        }
        return origin.substring(origin.indexOf(':') + 1).trim();
    }

    private String extractContato(Cliente cliente) {
        if (cliente == null || cliente.contacts().isEmpty()) {
            return "-";
        }
        return cliente.contacts().getFirst().contact();
    }

    private void configureTables() {
        configureRankingColumn(rankingClienteColumn, 0, row -> row.cliente());
        configureRankingColumn(rankingEmailColumn, 1, row -> row.contato());
        configureRankingColumn(rankingServicosColumn, 2, row -> String.valueOf(row.servicos()));
        configureRankingColumn(rankingFaturamentoColumn, 3, row -> apresentadorMoeda.format(row.faturamento()));
        configureRankingColumn(rankingIndicacoesColumn, 4, row -> String.valueOf(row.indicacoes()));

        configureIndicacaoColumn(indicacaoNomeColumn, 0, row -> row.nome());
        configureIndicacaoColumn(indicacaoContatoColumn, 1, row -> row.contato());
        configureIndicacaoColumn(indicacaoClienteColumn, 2, row -> row.clienteIndicador());
        configureIndicacaoColumn(indicacaoDataColumn, 3, row -> row.data());
        configureIndicacaoColumn(indicacaoStatusColumn, 4, row -> row.status());
    }

    private void configureRankingColumn(TableColumn<ClienteRankingRow, String> column, int fallbackIndex, java.util.function.Function<ClienteRankingRow, String> getter) {
        TableColumn<ClienteRankingRow, String> target = column != null ? column : getRankingColumn(fallbackIndex);
        if (target != null) {
            target.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        }
    }

    private void configureIndicacaoColumn(TableColumn<IndicacaoRow, String> column, int fallbackIndex, java.util.function.Function<IndicacaoRow, String> getter) {
        TableColumn<IndicacaoRow, String> target = column != null ? column : getIndicacaoColumn(fallbackIndex);
        if (target != null) {
            target.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        }
    }

    @SuppressWarnings("unchecked")
    private TableColumn<ClienteRankingRow, String> getRankingColumn(int index) {
        if (rankingTable == null || rankingTable.getColumns().size() <= index) {
            return null;
        }
        return (TableColumn<ClienteRankingRow, String>) rankingTable.getColumns().get(index);
    }

    @SuppressWarnings("unchecked")
    private TableColumn<IndicacaoRow, String> getIndicacaoColumn(int index) {
        if (indicacoesTable == null || indicacoesTable.getColumns().size() <= index) {
            return null;
        }
        return (TableColumn<IndicacaoRow, String>) indicacoesTable.getColumns().get(index);
    }

    private record ClienteRankingRow(String cliente, String contato, long servicos, BigDecimal faturamento, long indicacoes) {
    }

    private record IndicacaoRow(String nome, String contato, String clienteIndicador, String data, String status) {
    }
}

package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.potenciaisclientes.porta.entrada.CasoDeUsoPotencialCliente;
import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoServicoPrestado;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.dominio.potenciaisclientes.PotencialCliente;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorData;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorMoeda;
<<<<<<< Updated upstream
=======
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaOrdemServico;
import br.com.sigla.interfacegrafica.formatador.FormatadorMascaraCpf;
import br.com.sigla.interfacegrafica.modelo.OpcaoId;
>>>>>>> Stashed changes
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.UtilComboBox;
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
<<<<<<< Updated upstream
=======
    @FXML
    private TextField indicacaoBuscaField;
    @FXML
    private ComboBox<OpcaoId> indicacaoClienteFiltroField;
    @FXML
    private ChoiceBox<String> indicacaoStatusFiltro;
    @FXML
    private DatePicker indicacaoInicioPicker;
    @FXML
    private DatePicker indicacaoFimPicker;
>>>>>>> Stashed changes

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
<<<<<<< Updated upstream
=======
        if (indicacaoStatusFiltro != null) {
            indicacaoStatusFiltro.getItems().setAll("TODOS", "NOVO", "CONTATADO", "AGUARDANDO_RETORNO", "CONVERTIDO", "PERDIDO", "CANCELADO");
            indicacaoStatusFiltro.getSelectionModel().select("TODOS");
        }
        UtilComboBox.preencher(indicacaoClienteFiltroField, clientesOpcoes(), true);
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
=======
    @FXML
    private void onFiltrarIndicacoes() {
        refresh();
    }

    @FXML
    private void onEditarIndicacao() {
        PotencialCliente lead = indicacaoSelecionada();
        if (lead == null) {
            return;
        }
        Optional<CasoDeUsoPotencialCliente.RegisterPotencialClienteCommand> command = abrirDialogoIndicacao(lead);
        command.ifPresent(value -> {
            executar(() -> casoDeUsoPotencialCliente.update(value));
            refresh();
        });
    }

    @FXML
    private void onAlterarStatusIndicacao() {
        PotencialCliente lead = indicacaoSelecionada();
        if (lead == null) {
            return;
        }
        Optional<CasoDeUsoPotencialCliente.AlterarStatusIndicacaoCommand> command = abrirDialogoStatus(lead);
        command.ifPresent(value -> {
            executar(() -> casoDeUsoPotencialCliente.alterarStatus(value));
            refresh();
        });
    }

    @FXML
    private void onConverterIndicacao() {
        PotencialCliente lead = indicacaoSelecionada();
        if (lead == null) {
            return;
        }
        Optional<CasoDeUsoCliente.RegisterClienteCommand> command = abrirDialogoConversao(lead);
        command.ifPresent(value -> {
            executar(() -> casoDeUsoPotencialCliente.converterEmCliente(new CasoDeUsoPotencialCliente.ConverterIndicacaoCommand(lead.id(), value.id(), value)));
            refresh();
        });
    }

    private CasoDeUsoPotencialCliente.FiltroIndicacao filtroIndicacao() {
        String statusText = indicacaoStatusFiltro == null ? "" : indicacaoStatusFiltro.getValue();
        PotencialCliente.PotencialClienteStatus status = statusText == null || statusText.isBlank() || "TODOS".equals(statusText)
                ? null
                : PotencialCliente.PotencialClienteStatus.from(statusText);
        String indicadorId = UtilComboBox.idSelecionado(indicacaoClienteFiltroField);
        return new CasoDeUsoPotencialCliente.FiltroIndicacao(
                indicacaoBuscaField == null ? "" : indicacaoBuscaField.getText(),
                status,
                indicacaoInicioPicker == null ? null : indicacaoInicioPicker.getValue(),
                indicacaoFimPicker == null ? null : indicacaoFimPicker.getValue(),
                indicadorId
        );
    }

    private PotencialCliente indicacaoSelecionada() {
        IndicacaoRow row = indicacoesTable == null ? null : indicacoesTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            mostrar("Selecione uma indicacao.");
            return null;
        }
        return casoDeUsoPotencialCliente.listAll().stream()
                .filter(lead -> lead.id().equals(row.id()))
                .findFirst()
                .orElse(null);
    }

    private Optional<CasoDeUsoPotencialCliente.RegisterPotencialClienteCommand> abrirDialogoIndicacao(PotencialCliente lead) {
        Dialog<CasoDeUsoPotencialCliente.RegisterPotencialClienteCommand> dialog = new Dialog<>();
        dialog.setTitle("Editar Indicacao");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField nome = field(lead.name());
        TextField telefone = field(lead.contact());
        formatadorMascaraCpf.aplicarTelefone(telefone);
        ComboBox<OpcaoId> indicador = new ComboBox<>();
        UtilComboBox.preencher(indicador, clientesOpcoes(), true);
        UtilComboBox.selecionarPorId(indicador, lead.clienteIndicadorId());
        DatePicker data = new DatePicker(lead.dataIndicacao());
        ComboBox<PotencialCliente.PotencialClienteStatus> status = new ComboBox<>();
        status.getItems().setAll(PotencialCliente.PotencialClienteStatus.NOVO, PotencialCliente.PotencialClienteStatus.CONTATADO,
                PotencialCliente.PotencialClienteStatus.AGUARDANDO_RETORNO, PotencialCliente.PotencialClienteStatus.CONVERTIDO,
                PotencialCliente.PotencialClienteStatus.PERDIDO, PotencialCliente.PotencialClienteStatus.CANCELADO);
        status.getSelectionModel().select(PotencialCliente.PotencialClienteStatus.normalizar(lead.status()));
        TextArea observacoes = new TextArea(lead.observacoes());
        observacoes.setPrefRowCount(4);
        GridPane grid = grid();
        grid.addRow(0, new Label("Nome"), nome);
        grid.addRow(1, new Label("Telefone"), telefone);
        grid.addRow(2, new Label("Cliente indicador"), indicador);
        grid.addRow(3, new Label("Data"), data);
        grid.addRow(4, new Label("Status"), status);
        grid.addRow(5, new Label("Observacoes"), observacoes);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? new CasoDeUsoPotencialCliente.RegisterPotencialClienteCommand(
                lead.id(), nome.getText(), telefone.getText(), "INDICACAO:" + UtilComboBox.idSelecionado(indicador), UtilComboBox.idSelecionado(indicador),
                status.getValue(), data.getValue(), "Indicacao", observacoes.getText()) : null);
        return dialog.showAndWait();
    }

    private Optional<CasoDeUsoPotencialCliente.AlterarStatusIndicacaoCommand> abrirDialogoStatus(PotencialCliente lead) {
        Dialog<CasoDeUsoPotencialCliente.AlterarStatusIndicacaoCommand> dialog = new Dialog<>();
        dialog.setTitle("Alterar Status");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ComboBox<PotencialCliente.PotencialClienteStatus> status = new ComboBox<>();
        status.getItems().setAll(PotencialCliente.PotencialClienteStatus.NOVO, PotencialCliente.PotencialClienteStatus.CONTATADO,
                PotencialCliente.PotencialClienteStatus.AGUARDANDO_RETORNO, PotencialCliente.PotencialClienteStatus.CONVERTIDO,
                PotencialCliente.PotencialClienteStatus.PERDIDO, PotencialCliente.PotencialClienteStatus.CANCELADO);
        status.getSelectionModel().select(PotencialCliente.PotencialClienteStatus.normalizar(lead.status()));
        TextArea motivo = new TextArea();
        motivo.setPrefRowCount(3);
        GridPane grid = grid();
        grid.addRow(0, new Label("Status"), status);
        grid.addRow(1, new Label("Motivo/observacao"), motivo);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? new CasoDeUsoPotencialCliente.AlterarStatusIndicacaoCommand(lead.id(), status.getValue(), motivo.getText()) : null);
        return dialog.showAndWait();
    }

    private Optional<CasoDeUsoCliente.RegisterClienteCommand> abrirDialogoConversao(PotencialCliente lead) {
        Dialog<CasoDeUsoCliente.RegisterClienteCommand> dialog = new Dialog<>();
        dialog.setTitle("Converter Indicacao em Cliente");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ComboBox<Cliente.TipoCliente> tipo = new ComboBox<>();
        tipo.getItems().setAll(Cliente.TipoCliente.values());
        tipo.getSelectionModel().select(Cliente.TipoCliente.PESSOA_FISICA);
        TextField nome = field(lead.name());
        TextField razao = field("");
        TextField fantasia = field("");
        TextField cpf = field("");
        TextField cnpj = field("");
        TextField telefone = field(lead.contact());
        formatadorMascaraCpf.aplicarCpf(cpf);
        formatadorMascaraCpf.aplicarCnpj(cnpj);
        formatadorMascaraCpf.aplicarTelefone(telefone);
        TextField email = field("");
        TextArea observacoes = new TextArea("Cliente convertido da indicacao " + lead.id());
        observacoes.setPrefRowCount(3);
        GridPane grid = grid();
        grid.addRow(0, new Label("Tipo"), tipo);
        grid.addRow(1, new Label("Nome"), nome);
        grid.addRow(2, new Label("Razao social"), razao);
        grid.addRow(3, new Label("Nome fantasia"), fantasia);
        grid.addRow(4, new Label("CPF"), cpf);
        grid.addRow(5, new Label("CNPJ"), cnpj);
        grid.addRow(6, new Label("Telefone"), telefone);
        grid.addRow(7, new Label("E-mail"), email);
        grid.addRow(8, new Label("Observacoes"), observacoes);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? new CasoDeUsoCliente.RegisterClienteCommand(
                UUID.randomUUID().toString(), tipo.getValue(), nome.getText(), razao.getText(), fantasia.getText(), cpf.getText(), cnpj.getText(),
                telefone.getText(), email.getText(), "", "", "", "", "", "", "", List.of(), observacoes.getText(), true) : null);
        return dialog.showAndWait();
    }

    private TextField field(String value) {
        return new TextField(value == null ? "" : value);
    }

    private GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        return grid;
    }

    private List<OpcaoId> clientesOpcoes() {
        return casoDeUsoCliente.listAll().stream()
                .filter(Cliente::ativo)
                .map(cliente -> new OpcaoId(cliente.id(), cliente.name()))
                .toList();
    }

    private void executar(Runnable runnable) {
        try {
            runnable.run();
        } catch (IllegalArgumentException exception) {
            mostrar(exception.getMessage());
        }
    }

    private void mostrar(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.showAndWait();
    }

>>>>>>> Stashed changes
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

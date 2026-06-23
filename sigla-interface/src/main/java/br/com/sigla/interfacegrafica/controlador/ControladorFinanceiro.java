package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.relatorios.recibo.ServicoRelatorioRecibo;
import br.com.sigla.dominio.financeiro.CategoriaFinanceira;
import br.com.sigla.dominio.financeiro.FormaPagamentoFinanceira;
import br.com.sigla.dominio.financeiro.LancamentoFinanceiro;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorData;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorMoeda;
import br.com.sigla.interfacegrafica.async.ExecutorTarefasUi;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.TradutorInterface;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ControladorFinanceiro extends ControladorComMenuPrincipal {

    private final CasoDeUsoCliente casoDeUsoCliente;
    private final CasoDeUsoFinanceiro casoDeUsoFinanceiro;
    private final CasoDeUsoOrdemServico casoDeUsoOrdemServico;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final ApresentadorMoeda apresentadorMoeda;
    private final ApresentadorData apresentadorData;
    private final ServicoRelatorioRecibo servicoRelatorioRecibo;
    private final ExecutorTarefasUi executorTarefasUi;

    private Map<String, String> clienteNomes = Map.of();
    private Map<String, String> ordemNumeros = Map.of();
    private int geracaoRefresh;

    @FXML
    private Button botaoTodos;
    @FXML
    private Button botaoPagos;
    @FXML
    private Button botaoPendentes;
    @FXML
    private Button botaoVencidos;
    @FXML
    private Label receitasLabel;
    @FXML
    private Label despesasLabel;
    @FXML
    private Label saldoLabel;
    @FXML
    private Label receberLabel;
    @FXML
    private TableView<CasoDeUsoFinanceiro.TransacaoFinanceiraView> transacoesTable;
    @FXML
    private TableColumn<CasoDeUsoFinanceiro.TransacaoFinanceiraView, String> tipoColumn;
    @FXML
    private TableColumn<CasoDeUsoFinanceiro.TransacaoFinanceiraView, String> categoriaColumn;
    @FXML
    private TableColumn<CasoDeUsoFinanceiro.TransacaoFinanceiraView, String> descricaoColumn;
    @FXML
    private TableColumn<CasoDeUsoFinanceiro.TransacaoFinanceiraView, String> clienteColumn;
    @FXML
    private TableColumn<CasoDeUsoFinanceiro.TransacaoFinanceiraView, String> ordemColumn;
    @FXML
    private TableColumn<CasoDeUsoFinanceiro.TransacaoFinanceiraView, String> valorColumn;
    @FXML
    private TableColumn<CasoDeUsoFinanceiro.TransacaoFinanceiraView, String> emissaoColumn;
    @FXML
    private TableColumn<CasoDeUsoFinanceiro.TransacaoFinanceiraView, String> vencimentoColumn;
    @FXML
    private TableColumn<CasoDeUsoFinanceiro.TransacaoFinanceiraView, String> pagamentoColumn;
    @FXML
    private TableColumn<CasoDeUsoFinanceiro.TransacaoFinanceiraView, String> formaPagamentoColumn;
    @FXML
    private TableColumn<CasoDeUsoFinanceiro.TransacaoFinanceiraView, String> parcelasColumn;
    @FXML
    private TableColumn<CasoDeUsoFinanceiro.TransacaoFinanceiraView, String> observacoesColumn;
    @FXML
    private TableColumn<CasoDeUsoFinanceiro.TransacaoFinanceiraView, String> statusColumn;

    private CasoDeUsoFinanceiro.FiltroFinanceiro filtroAtual;

    public ControladorFinanceiro(
            CasoDeUsoCliente casoDeUsoCliente,
            CasoDeUsoFinanceiro casoDeUsoFinanceiro,
            CasoDeUsoOrdemServico casoDeUsoOrdemServico,
            GerenciadorNavegacao gerenciadorNavegacao,
            ApresentadorMoeda apresentadorMoeda,
            ApresentadorData apresentadorData,
            ServicoRelatorioRecibo servicoRelatorioRecibo,
            ExecutorTarefasUi executorTarefasUi
    ) {
        super(gerenciadorNavegacao);
        this.casoDeUsoCliente = casoDeUsoCliente;
        this.casoDeUsoFinanceiro = casoDeUsoFinanceiro;
        this.casoDeUsoOrdemServico = casoDeUsoOrdemServico;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
        this.apresentadorMoeda = apresentadorMoeda;
        this.apresentadorData = apresentadorData;
        this.servicoRelatorioRecibo = servicoRelatorioRecibo;
        this.executorTarefasUi = executorTarefasUi;
    }

    @FXML
    public void initialize() {
        configureTable();
        br.com.sigla.interfacegrafica.util.DestaqueFiltro.destacar(botaoTodos, botaoTodos, botaoPagos, botaoPendentes, botaoVencidos);
        refresh();
    }

    @FXML
    private void onNovaTransacao() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.NEW_TRANSACTION);
    }

    @FXML
    private void onTodos() {
        filtroAtual = null;
        br.com.sigla.interfacegrafica.util.DestaqueFiltro.destacar(botaoTodos, botaoTodos, botaoPagos, botaoPendentes, botaoVencidos);
        refresh();
    }

    @FXML
    private void onPagos() {
        filtroAtual = new CasoDeUsoFinanceiro.FiltroFinanceiro(null, null, null, CasoDeUsoFinanceiro.TransactionStatus.PAID, "", "", "", "", false);
        br.com.sigla.interfacegrafica.util.DestaqueFiltro.destacar(botaoPagos, botaoTodos, botaoPagos, botaoPendentes, botaoVencidos);
        refresh();
    }

    @FXML
    private void onPendentes() {
        filtroAtual = new CasoDeUsoFinanceiro.FiltroFinanceiro(null, null, null, CasoDeUsoFinanceiro.TransactionStatus.PENDING, "", "", "", "", false);
        br.com.sigla.interfacegrafica.util.DestaqueFiltro.destacar(botaoPendentes, botaoTodos, botaoPagos, botaoPendentes, botaoVencidos);
        refresh();
    }

    @FXML
    private void onVencidos() {
        filtroAtual = new CasoDeUsoFinanceiro.FiltroFinanceiro(null, null, null, null, "", "", "", "", true);
        br.com.sigla.interfacegrafica.util.DestaqueFiltro.destacar(botaoVencidos, botaoTodos, botaoPagos, botaoPendentes, botaoVencidos);
        refresh();
    }

    @FXML
    private void onFiltrar() {
        abrirDialogoFiltro().ifPresent(filtro -> {
            filtroAtual = filtro;
            refresh();
        });
    }

    @FXML
    private void onEditarTransacao() {
        var selected = selecionada();
        if (selected == null || selected.status() == CasoDeUsoFinanceiro.TransactionStatus.CANCELLED) {
            return;
        }
        abrirDialogoEdicao(selected).ifPresent(command -> executar(() -> casoDeUsoFinanceiro.updateLancamento(command)));
    }

    @FXML
    private void onMarcarPago() {
        var selected = selecionada();
        if (selected == null || selected.status() == CasoDeUsoFinanceiro.TransactionStatus.PAID) {
            return;
        }
        executar(() -> casoDeUsoFinanceiro.markPaid(selected.id(), LocalDate.now()));
    }

    @FXML
    private void onBaixarParcela() {
        var selected = selecionada();
        if (selected == null) {
            return;
        }
        var parcelas = casoDeUsoFinanceiro.listParcelas(selected.id()).stream()
                .filter(parcela -> parcela.status() != LancamentoFinanceiro.Status.PAID)
                .filter(parcela -> parcela.status() != LancamentoFinanceiro.Status.CANCELLED)
                .toList();
        if (parcelas.isEmpty()) {
            mostrar("Não há parcela pendente para baixar.");
            return;
        }
        Dialog<LancamentoFinanceiro.ParcelaFinanceira> dialog = new Dialog<>();
        br.com.sigla.interfacegrafica.util.DialogoUi.estilizar(dialog);
        dialog.setTitle("Baixar parcela");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ComboBox<LancamentoFinanceiro.ParcelaFinanceira> combo = new ComboBox<>();
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(LancamentoFinanceiro.ParcelaFinanceira parcela) {
                return parcela == null ? "" : parcela.numeroParcela() + " - " + apresentadorMoeda.format(parcela.valorParcela()) + " - " + apresentadorData.format(parcela.dataVencimento());
            }

            @Override
            public LancamentoFinanceiro.ParcelaFinanceira fromString(String string) {
                return null;
            }
        });
        combo.getItems().setAll(parcelas);
        combo.getSelectionModel().selectFirst();
        dialog.getDialogPane().setContent(combo);
        dialog.setResultConverter(button -> button == ButtonType.OK ? combo.getValue() : null);
        dialog.showAndWait().ifPresent(parcela -> executar(() -> casoDeUsoFinanceiro.baixarParcela(selected.id(), parcela.id(), LocalDate.now())));
    }

    @FXML
    private void onEstornarPagamento() {
        var selected = selecionada();
        if (selected == null) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Estornar pagamento");
        dialog.setHeaderText("Informe o motivo do estorno");
        dialog.showAndWait().ifPresent(motivo -> {
            if (motivo.isBlank()) {
                mostrar("Informe o motivo do estorno.");
                return;
            }
            executar(() -> casoDeUsoFinanceiro.estornarPagamento(selected.id(), motivo));
        });
    }

    @FXML
    private void onCancelarTransacao() {
        var selected = selecionada();
        if (selected == null) {
            return;
        }
        if (selected.status() == CasoDeUsoFinanceiro.TransactionStatus.CANCELLED) {
            mostrar("Este lançamento já está cancelado.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Cancelar lançamento");
        dialog.setHeaderText("Informe o motivo do cancelamento");
        dialog.showAndWait().ifPresent(motivo -> {
            if (motivo.isBlank()) {
                mostrar("Informe o motivo do cancelamento.");
                return;
            }
            executar(() -> casoDeUsoFinanceiro.cancel(selected.id(), motivo));
        });
    }

    @FXML
    private void onImprimirRecibo() {
        var selected = selecionada();
        if (selected == null) {
            return;
        }
        if (selected.type() != CasoDeUsoFinanceiro.TransactionType.ENTRY) {
            mostrar("O recibo é emitido apenas para recebimentos (entradas).");
            return;
        }
        try {
            Path arquivo = servicoRelatorioRecibo.imprimir(new ServicoRelatorioRecibo.DadosRecibo(
                    selected.id(),
                    resolveCliente(selected.customerId()),
                    blankAsDash(selected.description()),
                    apresentadorMoeda.format(selected.amount()),
                    blankAsDash(selected.paymentMethod()),
                    apresentadorData.format(selected.paymentDate()),
                    TradutorInterface.texto(selected.status()),
                    selected.notes()
            ));
            br.com.sigla.interfacegrafica.util.DialogoUi.informacao("Recibo gerado em:\n" + arquivo);
        } catch (Exception exception) {
            mostrar(br.com.sigla.interfacegrafica.util.MensagensErro.descrever("Não foi possível gerar o recibo:", exception));
        }
    }

    private void refresh() {
        var filtro = filtroAtual;
        int geracao = ++geracaoRefresh;
        executorTarefasUi.executar(
                () -> carregar(filtro),
                dados -> {
                    if (geracao == geracaoRefresh) {
                        aplicar(dados);
                    }
                });
    }

    private FinanceiroSnapshot carregar(CasoDeUsoFinanceiro.FiltroFinanceiro filtro) {
        var lancamentos = casoDeUsoFinanceiro.listLancamentos(null);
        var transacoes = casoDeUsoFinanceiro.listTransactions(filtro);
        Map<String, String> nomes = casoDeUsoCliente.listAll().stream()
                .collect(Collectors.toMap(cliente -> cliente.id(), cliente -> cliente.name(), (a, b) -> a));
        Map<String, String> ordens = casoDeUsoOrdemServico.listAll().stream()
                .collect(Collectors.toMap(
                        ordem -> ordem.id(),
                        ordem -> ordem.numeroOs() == null ? ordem.id() : String.valueOf(ordem.numeroOs()),
                        (a, b) -> a));
        return new FinanceiroSnapshot(lancamentos, transacoes, nomes, ordens);
    }

    private record FinanceiroSnapshot(
            java.util.List<LancamentoFinanceiro> lancamentos,
            java.util.List<CasoDeUsoFinanceiro.TransacaoFinanceiraView> transacoes,
            Map<String, String> clienteNomes,
            Map<String, String> ordemNumeros
    ) {
    }

    private void aplicar(FinanceiroSnapshot dados) {
        clienteNomes = dados.clienteNomes();
        ordemNumeros = dados.ordemNumeros();
        var lancamentos = dados.lancamentos();
        BigDecimal receitas = lancamentos.stream()
                .filter(lancamento -> lancamento.tipo() == LancamentoFinanceiro.Tipo.ENTRY)
                .filter(lancamento -> lancamento.status() == LancamentoFinanceiro.Status.PAID)
                .map(LancamentoFinanceiro::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal despesas = lancamentos.stream()
                .filter(lancamento -> lancamento.tipo() == LancamentoFinanceiro.Tipo.EXPENSE)
                .filter(lancamento -> lancamento.status() == LancamentoFinanceiro.Status.PAID)
                .map(LancamentoFinanceiro::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receber = lancamentos.stream()
                .filter(lancamento -> lancamento.tipo() == LancamentoFinanceiro.Tipo.ENTRY)
                .filter(lancamento -> lancamento.status() != LancamentoFinanceiro.Status.PAID)
                .filter(lancamento -> lancamento.status() != LancamentoFinanceiro.Status.CANCELLED)
                .map(lancamento -> lancamento.valorTotal().subtract(lancamento.valorPago()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        receitasLabel.setText(apresentadorMoeda.format(receitas));
        despesasLabel.setText(apresentadorMoeda.format(despesas));
        saldoLabel.setText(apresentadorMoeda.format(receitas.subtract(despesas)));
        receberLabel.setText(apresentadorMoeda.format(receber));
        transacoesTable.getItems().setAll(dados.transacoes());
    }

    private void configureTable() {
        configureColumn(tipoColumn, 0, row -> TradutorInterface.texto(row.type()));
        configureColumn(categoriaColumn, 1, row -> blankAsDash(TradutorInterface.texto(row.category())));
        configureColumn(descricaoColumn, 2, row -> blankAsDash(row.description()));
        configureColumn(clienteColumn, 3, row -> resolveCliente(row.customerId()));
        configureColumn(ordemColumn, 4, row -> resolveOrdem(row.orderReference()));
        configureColumn(valorColumn, 5, row -> apresentadorMoeda.format(row.amount()));
        configureColumn(emissaoColumn, 6, row -> apresentadorData.format(row.issueDate()));
        configureColumn(vencimentoColumn, 7, row -> apresentadorData.format(row.dueDate()));
        configureColumn(pagamentoColumn, 8, row -> apresentadorData.format(row.paymentDate()));
        configureColumn(formaPagamentoColumn, 9, row -> blankAsDash(row.paymentMethod()));
        configureColumn(parcelasColumn, 10, row -> row.installment() ? row.installmentCount() + " (" + apresentadorMoeda.format(row.paidAmount()) + " pago)" : "-");
        configureColumn(observacoesColumn, 11, row -> blankAsDash(row.notes()));
        configureColumn(statusColumn, 12, row -> TradutorInterface.texto(row.overdue() && row.status() == CasoDeUsoFinanceiro.TransactionStatus.PENDING ? "OVERDUE" : row.status()));
    }

    private Optional<CasoDeUsoFinanceiro.SalvarLancamentoFinanceiroCommand> abrirDialogoEdicao(CasoDeUsoFinanceiro.TransacaoFinanceiraView selected) {
        Dialog<CasoDeUsoFinanceiro.SalvarLancamentoFinanceiroCommand> dialog = new Dialog<>();
        br.com.sigla.interfacegrafica.util.DialogoUi.estilizar(dialog);
        dialog.setTitle("Editar lançamento");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ComboBox<CategoriaFinanceira> categoria = comboCategoria(selected.type(), selected.categoryId());
        ComboBox<FormaPagamentoFinanceira> forma = comboForma(selected.paymentMethodId());
        TextField descricao = new TextField(selected.description());
        TextField valor = new TextField(selected.amount().toPlainString());
        DatePicker emissao = new DatePicker(selected.issueDate());
        DatePicker vencimento = new DatePicker(selected.dueDate());
        TextArea observacoes = new TextArea(selected.notes());
        observacoes.setPrefRowCount(3);
        GridPane grid = grid();
        grid.addRow(0, new Label("Categoria"), categoria);
        grid.addRow(1, new Label("Forma"), forma);
        grid.addRow(2, new Label("Descrição"), descricao);
        grid.addRow(3, new Label("Valor"), valor);
        grid.addRow(4, new Label("Emissão"), emissao);
        grid.addRow(5, new Label("Vencimento"), vencimento);
        grid.addRow(6, new Label("Observações"), observacoes);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? new CasoDeUsoFinanceiro.SalvarLancamentoFinanceiroCommand(
                selected.id(), selected.type(), categoria.getValue().id(), forma.getValue().id(), descricao.getText(),
                selected.customerId(), selected.orderReference(), parseMoney(valor.getText()), emissao.getValue(),
                vencimento.getValue(), selected.paymentDate(), selected.installment(), selected.installmentCount(),
                "", observacoes.getText(), selected.status()) : null);
        return dialog.showAndWait();
    }

    private Optional<CasoDeUsoFinanceiro.FiltroFinanceiro> abrirDialogoFiltro() {
        Dialog<CasoDeUsoFinanceiro.FiltroFinanceiro> dialog = new Dialog<>();
        br.com.sigla.interfacegrafica.util.DialogoUi.estilizar(dialog);
        dialog.setTitle("Filtros financeiros");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        DatePicker inicio = new DatePicker();
        DatePicker fim = new DatePicker();
        ComboBox<CasoDeUsoFinanceiro.TransactionType> tipo = new ComboBox<>();
        TradutorInterface.aplicar(tipo);
        tipo.getItems().setAll(CasoDeUsoFinanceiro.TransactionType.values());
        ComboBox<CasoDeUsoFinanceiro.TransactionStatus> status = new ComboBox<>();
        TradutorInterface.aplicar(status);
        status.getItems().setAll(CasoDeUsoFinanceiro.TransactionStatus.values());
        ComboBox<CategoriaFinanceira> categoria = comboCategoria(null, "");
        ComboBox<FormaPagamentoFinanceira> forma = comboForma("");
        TextField cliente = new TextField();
        TextField texto = new TextField();
        CheckBox vencidos = new CheckBox("Somente vencidos");
        GridPane grid = grid();
        grid.addRow(0, new Label("Início"), inicio);
        grid.addRow(1, new Label("Fim"), fim);
        grid.addRow(2, new Label("Tipo"), tipo);
        grid.addRow(3, new Label("Status"), status);
        grid.addRow(4, new Label("Categoria"), categoria);
        grid.addRow(5, new Label("Forma"), forma);
        grid.addRow(6, new Label("Cliente"), cliente);
        grid.addRow(7, new Label("Texto"), texto);
        grid.addRow(8, new Label("Vencidos"), vencidos);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? new CasoDeUsoFinanceiro.FiltroFinanceiro(
                inicio.getValue(), fim.getValue(), tipo.getValue(), status.getValue(), cliente.getText(),
                forma.getValue() == null ? "" : forma.getValue().id(), categoria.getValue() == null ? "" : categoria.getValue().id(),
                texto.getText(), vencidos.isSelected()) : null);
        return dialog.showAndWait();
    }

    private ComboBox<CategoriaFinanceira> comboCategoria(CasoDeUsoFinanceiro.TransactionType tipo, String selectedId) {
        ComboBox<CategoriaFinanceira> combo = new ComboBox<>();
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(CategoriaFinanceira categoria) {
                return categoria == null ? "" : TradutorInterface.texto(categoria.nome());
            }

            @Override
            public CategoriaFinanceira fromString(String string) {
                return null;
            }
        });
        combo.getItems().setAll(casoDeUsoFinanceiro.listCategoriasAtivas().stream()
                .filter(categoria -> tipo == null || (tipo == CasoDeUsoFinanceiro.TransactionType.EXPENSE
                        ? categoria.tipo().equalsIgnoreCase("EXPENSE")
                        : categoria.tipo().equalsIgnoreCase("ENTRY")))
                .toList());
        combo.getItems().stream().filter(item -> item.id().equals(selectedId)).findFirst().ifPresentOrElse(combo.getSelectionModel()::select, () -> {
            if (!combo.getItems().isEmpty()) {
                combo.getSelectionModel().selectFirst();
            }
        });
        return combo;
    }

    private ComboBox<FormaPagamentoFinanceira> comboForma(String selectedId) {
        ComboBox<FormaPagamentoFinanceira> combo = new ComboBox<>();
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(FormaPagamentoFinanceira forma) {
                return forma == null ? "" : forma.nome();
            }

            @Override
            public FormaPagamentoFinanceira fromString(String string) {
                return null;
            }
        });
        combo.getItems().setAll(casoDeUsoFinanceiro.listFormasPagamentoAtivas());
        combo.getItems().stream().filter(item -> item.id().equals(selectedId)).findFirst().ifPresentOrElse(combo.getSelectionModel()::select, () -> {
            if (!combo.getItems().isEmpty()) {
                combo.getSelectionModel().selectFirst();
            }
        });
        return combo;
    }

    private void configureColumn(TableColumn<CasoDeUsoFinanceiro.TransacaoFinanceiraView, String> column, int fallbackIndex, java.util.function.Function<CasoDeUsoFinanceiro.TransacaoFinanceiraView, String> getter) {
        TableColumn<CasoDeUsoFinanceiro.TransacaoFinanceiraView, String> target = column != null ? column : getColumn(fallbackIndex);
        if (target != null) {
            target.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        }
    }

    @SuppressWarnings("unchecked")
    private TableColumn<CasoDeUsoFinanceiro.TransacaoFinanceiraView, String> getColumn(int index) {
        if (transacoesTable == null || transacoesTable.getColumns().size() <= index) {
            return null;
        }
        return (TableColumn<CasoDeUsoFinanceiro.TransacaoFinanceiraView, String>) transacoesTable.getColumns().get(index);
    }

    private CasoDeUsoFinanceiro.TransacaoFinanceiraView selecionada() {
        CasoDeUsoFinanceiro.TransacaoFinanceiraView selecionada = transacoesTable == null
                ? null
                : transacoesTable.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            mostrar("Selecione um lançamento na tabela.");
        }
        return selecionada;
    }

    private void executar(Runnable runnable) {
        try {
            runnable.run();
            refresh();
        } catch (Exception exception) {
            mostrar(br.com.sigla.interfacegrafica.util.MensagensErro.descrever(exception));
        }
    }

    private void mostrar(String message) {
        br.com.sigla.interfacegrafica.util.DialogoUi.aviso(message);
    }

    private GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        return grid;
    }

    private BigDecimal parseMoney(String value) {
        return new BigDecimal(value == null || value.isBlank() ? "0" : value.replace(",", "."));
    }

    private String resolveCliente(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return "-";
        }
        return clienteNomes.getOrDefault(customerId, customerId);
    }

    private String resolveOrdem(String orderReference) {
        if (orderReference == null || orderReference.isBlank()) {
            return "-";
        }
        return ordemNumeros.getOrDefault(orderReference, orderReference);
    }

    private String blankAsDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}

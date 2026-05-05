package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.dominio.financeiro.PlanoParcelamento;
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

@Component
public class ControladorFinanceiro extends ControladorComMenuPrincipal {

    private final CasoDeUsoCliente casoDeUsoCliente;
    private final CasoDeUsoFinanceiro casoDeUsoFinanceiro;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final ApresentadorMoeda apresentadorMoeda;
    private final ApresentadorData apresentadorData;

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

    private CasoDeUsoFinanceiro.TransactionStatus filtroAtual;

    public ControladorFinanceiro(
            CasoDeUsoCliente casoDeUsoCliente,
            CasoDeUsoFinanceiro casoDeUsoFinanceiro,
            GerenciadorNavegacao gerenciadorNavegacao,
            ApresentadorMoeda apresentadorMoeda,
            ApresentadorData apresentadorData
    ) {
        super(gerenciadorNavegacao);
        this.casoDeUsoCliente = casoDeUsoCliente;
        this.casoDeUsoFinanceiro = casoDeUsoFinanceiro;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
        this.apresentadorMoeda = apresentadorMoeda;
        this.apresentadorData = apresentadorData;
    }

    @FXML
    public void initialize() {
        configureTable();
        refresh();
    }

    @FXML
    private void onNovaTransacao() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.NEW_TRANSACTION);
    }

    @FXML
    private void onTodos() {
        filtroAtual = null;
        refresh();
    }

    @FXML
    private void onPagos() {
        filtroAtual = CasoDeUsoFinanceiro.TransactionStatus.PAID;
        refresh();
    }

    @FXML
    private void onPendentes() {
        filtroAtual = CasoDeUsoFinanceiro.TransactionStatus.PENDING;
        refresh();
    }

    @FXML
    private void onMarcarPago() {
        CasoDeUsoFinanceiro.TransacaoFinanceiraView selected = transacoesTable == null
                ? null
                : transacoesTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.status() == CasoDeUsoFinanceiro.TransactionStatus.PAID) {
            return;
        }
        casoDeUsoFinanceiro.markPaid(selected.id(), java.time.LocalDate.now());
        refresh();
    }

    @FXML
    private void onCancelarTransacao() {
        CasoDeUsoFinanceiro.TransacaoFinanceiraView selected = transacoesTable == null
                ? null
                : transacoesTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.status() == CasoDeUsoFinanceiro.TransactionStatus.CANCELLED) {
            return;
        }
        casoDeUsoFinanceiro.cancel(selected.id());
        refresh();
    }

    private void refresh() {
        BigDecimal receitas = casoDeUsoFinanceiro.listTransactions().stream()
                .filter(transaction -> transaction.type() == CasoDeUsoFinanceiro.TransactionType.ENTRY)
                .filter(transaction -> transaction.status() == CasoDeUsoFinanceiro.TransactionStatus.PAID)
                .map(CasoDeUsoFinanceiro.TransacaoFinanceiraView::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal despesas = casoDeUsoFinanceiro.listTransactions().stream()
                .filter(transaction -> transaction.type() == CasoDeUsoFinanceiro.TransactionType.EXPENSE)
                .filter(transaction -> transaction.status() == CasoDeUsoFinanceiro.TransactionStatus.PAID)
                .map(CasoDeUsoFinanceiro.TransacaoFinanceiraView::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receber = casoDeUsoFinanceiro.listPlanoParcelamentos().stream()
                .filter(plan -> plan.status() != PlanoParcelamento.InstallmentStatus.PAID)
                .map(PlanoParcelamento::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (receitasLabel != null) {
            receitasLabel.setText(apresentadorMoeda.format(receitas));
        }
        if (despesasLabel != null) {
            despesasLabel.setText(apresentadorMoeda.format(despesas));
        }
        if (saldoLabel != null) {
            saldoLabel.setText(apresentadorMoeda.format(casoDeUsoFinanceiro.currentBalance()));
        }
        if (receberLabel != null) {
            receberLabel.setText(apresentadorMoeda.format(receber));
        }

        if (transacoesTable != null) {
            transacoesTable.getItems().setAll(casoDeUsoFinanceiro.listTransactions().stream()
                    .filter(transaction -> filtroAtual == null || transaction.status() == filtroAtual)
                    .toList());
        }
    }

    private void configureTable() {
        configureColumn(tipoColumn, 0, row -> row.type().name());
        configureColumn(categoriaColumn, 1, row -> blankAsDash(row.category()));
        configureColumn(descricaoColumn, 2, row -> blankAsDash(row.description()));
        configureColumn(clienteColumn, 3, row -> resolveCliente(row.customerId()));
        configureColumn(ordemColumn, 4, row -> blankAsDash(row.orderReference()));
        configureColumn(valorColumn, 5, row -> apresentadorMoeda.format(row.amount()));
        configureColumn(emissaoColumn, 6, row -> apresentadorData.format(row.issueDate()));
        configureColumn(vencimentoColumn, 7, row -> apresentadorData.format(row.dueDate()));
        configureColumn(pagamentoColumn, 8, row -> apresentadorData.format(row.paymentDate()));
        configureColumn(formaPagamentoColumn, 9, row -> blankAsDash(row.paymentMethod()));
        configureColumn(parcelasColumn, 10, row -> "-");
        configureColumn(observacoesColumn, 11, row -> blankAsDash(row.createdBy()));
        configureColumn(statusColumn, 12, row -> row.status().name());
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

    private String resolveCliente(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return "-";
        }
        return casoDeUsoCliente.listAll().stream()
                .filter(customer -> customer.id().equals(customerId))
                .map(customer -> customer.name())
                .findFirst()
                .orElse(customerId);
    }

    private String blankAsDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}

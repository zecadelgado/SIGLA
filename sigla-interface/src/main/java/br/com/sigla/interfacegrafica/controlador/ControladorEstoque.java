package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
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
public class ControladorEstoque extends ControladorComMenuPrincipal {

    private final CasoDeUsoCliente casoDeUsoCliente;
    private final CasoDeUsoEstoque casoDeUsoEstoque;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final ApresentadorMoeda apresentadorMoeda;
    private final ApresentadorData apresentadorData;

    @FXML
    private Label totalProdutosLabel;
    @FXML
    private Label valorTotalLabel;
    @FXML
    private Label produtosBaixaLabel;
    @FXML
    private Label alertaLabel;
    @FXML
    private TableView<ProdutoRow> produtosTable;
    @FXML
    private TableColumn<ProdutoRow, String> produtoNomeColumn;
    @FXML
    private TableColumn<ProdutoRow, String> produtoDescricaoColumn;
    @FXML
    private TableColumn<ProdutoRow, String> produtoCustoColumn;
    @FXML
    private TableColumn<ProdutoRow, String> produtoVendaColumn;
    @FXML
    private TableColumn<ProdutoRow, String> produtoQuantidadeColumn;
    @FXML
    private TableColumn<ProdutoRow, String> produtoMinimoColumn;
    @FXML
    private TableView<CasoDeUsoEstoque.InventoryMovementView> movimentacoesTable;
    @FXML
    private TableColumn<CasoDeUsoEstoque.InventoryMovementView, String> movimentoProdutoColumn;
    @FXML
    private TableColumn<CasoDeUsoEstoque.InventoryMovementView, String> movimentoTipoColumn;
    @FXML
    private TableColumn<CasoDeUsoEstoque.InventoryMovementView, String> movimentoQuantidadeColumn;
    @FXML
    private TableColumn<CasoDeUsoEstoque.InventoryMovementView, String> movimentoValorColumn;
    @FXML
    private TableColumn<CasoDeUsoEstoque.InventoryMovementView, String> movimentoUsuarioColumn;
    @FXML
    private TableColumn<CasoDeUsoEstoque.InventoryMovementView, String> movimentoClienteColumn;
    @FXML
    private TableColumn<CasoDeUsoEstoque.InventoryMovementView, String> movimentoOrdemColumn;
    @FXML
    private TableColumn<CasoDeUsoEstoque.InventoryMovementView, String> movimentoDestinoColumn;
    @FXML
    private TableColumn<CasoDeUsoEstoque.InventoryMovementView, String> movimentoObservacoesColumn;

    public ControladorEstoque(
            CasoDeUsoCliente casoDeUsoCliente,
            CasoDeUsoEstoque casoDeUsoEstoque,
            GerenciadorNavegacao gerenciadorNavegacao,
            ApresentadorMoeda apresentadorMoeda,
            ApresentadorData apresentadorData
    ) {
        super(gerenciadorNavegacao);
        this.casoDeUsoCliente = casoDeUsoCliente;
        this.casoDeUsoEstoque = casoDeUsoEstoque;
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
    private void onNovoProduto() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.NEW_PRODUCT);
    }

    @FXML
    private void onMovimentar() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.NEW_MOVEMENT);
    }

    private void refresh() {
        var items = casoDeUsoEstoque.listAll();
        if (totalProdutosLabel != null) {
            totalProdutosLabel.setText(String.valueOf(items.size()));
        }
        BigDecimal valorTotal = items.stream()
                .map(item -> item.salePrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (valorTotalLabel != null) {
            valorTotalLabel.setText(apresentadorMoeda.format(valorTotal));
        }
        long lowCount = items.stream().filter(item -> item.isLowStock()).count();
        if (produtosBaixaLabel != null) {
            produtosBaixaLabel.setText(String.valueOf(lowCount));
        }
        if (alertaLabel != null) {
            alertaLabel.setText(lowCount == 0
                    ? "Nenhum produto em baixa."
                    : items.stream()
                    .filter(item -> item.isLowStock())
                    .map(item -> item.name() + " (" + item.quantity() + "/" + item.minimumQuantity() + ")")
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("-"));
        }

        if (produtosTable != null) {
            produtosTable.getItems().setAll(items.stream()
                    .map(item -> new ProdutoRow(
                            item.name(),
                            item.description(),
                            item.costPrice(),
                            item.salePrice(),
                            item.quantity(),
                            item.minimumQuantity()
                    ))
                    .toList());
        }
        if (movimentacoesTable != null) {
            movimentacoesTable.getItems().setAll(casoDeUsoEstoque.listMovements());
        }
    }

    private void configureTables() {
        configureProdutoColumn(produtoNomeColumn, 0, row -> row.nome());
        configureProdutoColumn(produtoDescricaoColumn, 1, row -> row.descricao());
        configureProdutoColumn(produtoCustoColumn, 2, row -> apresentadorMoeda.format(row.custo()));
        configureProdutoColumn(produtoVendaColumn, 3, row -> apresentadorMoeda.format(row.venda()));
        configureProdutoColumn(produtoQuantidadeColumn, 4, row -> String.valueOf(row.quantidade()));
        configureProdutoColumn(produtoMinimoColumn, 5, row -> String.valueOf(row.minimo()));

        configureMovimentoColumn(movimentoProdutoColumn, 0, row -> row.itemName());
        configureMovimentoColumn(movimentoTipoColumn, 1, row -> row.type().name());
        configureMovimentoColumn(movimentoQuantidadeColumn, 2, row -> String.valueOf(row.amount()));
        configureMovimentoColumn(movimentoValorColumn, 4, row -> apresentadorMoeda.format(row.totalPrice()));
        configureMovimentoColumn(movimentoUsuarioColumn, 5, row -> row.createdBy());
        configureMovimentoColumn(movimentoClienteColumn, 6, row -> resolveCliente(row.customerId()));
        configureMovimentoColumn(movimentoOrdemColumn, 7, row -> blankAsDash(row.orderReference()));
        configureMovimentoColumn(movimentoDestinoColumn, 8, row -> blankAsDash(row.destinationDescription()));
        configureMovimentoColumn(movimentoObservacoesColumn, 9, row -> buildObservacao(row));
    }

    private void configureProdutoColumn(TableColumn<ProdutoRow, String> column, int fallbackIndex, java.util.function.Function<ProdutoRow, String> getter) {
        TableColumn<ProdutoRow, String> target = column != null ? column : getProdutoColumn(fallbackIndex);
        if (target != null) {
            target.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        }
    }

    private void configureMovimentoColumn(TableColumn<CasoDeUsoEstoque.InventoryMovementView, String> column, int fallbackIndex, java.util.function.Function<CasoDeUsoEstoque.InventoryMovementView, String> getter) {
        TableColumn<CasoDeUsoEstoque.InventoryMovementView, String> target = column != null ? column : getMovimentoColumn(fallbackIndex);
        if (target != null) {
            target.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        }
    }

    @SuppressWarnings("unchecked")
    private TableColumn<ProdutoRow, String> getProdutoColumn(int index) {
        if (produtosTable == null || produtosTable.getColumns().size() <= index) {
            return null;
        }
        return (TableColumn<ProdutoRow, String>) produtosTable.getColumns().get(index);
    }

    @SuppressWarnings("unchecked")
    private TableColumn<CasoDeUsoEstoque.InventoryMovementView, String> getMovimentoColumn(int index) {
        if (movimentacoesTable == null || movimentacoesTable.getColumns().size() <= index) {
            return null;
        }
        return (TableColumn<CasoDeUsoEstoque.InventoryMovementView, String>) movimentacoesTable.getColumns().get(index);
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

    private String buildObservacao(CasoDeUsoEstoque.InventoryMovementView movement) {
        if (movement.notes() != null && !movement.notes().isBlank()) {
            return movement.notes();
        }
        return apresentadorData.format(movement.occurredOn());
    }

    private String blankAsDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private record ProdutoRow(String nome, String descricao, BigDecimal custo, BigDecimal venda, int quantidade, int minimo) {
    }
}

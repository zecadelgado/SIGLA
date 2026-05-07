package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.dominio.estoque.ItemEstoque;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorData;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorMoeda;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
    private TableColumn<ProdutoRow, String> produtoStatusColumn;
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
    private boolean somenteBaixoEstoque;

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

    @FXML
    private void onEditarProduto() {
        ProdutoRow row = produtosTable == null ? null : produtosTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            mostrar("Selecione um produto.");
            return;
        }
        ItemEstoque item = casoDeUsoEstoque.listAll().stream().filter(produto -> produto.id().equals(row.id())).findFirst().orElse(null);
        if (item == null) {
            return;
        }
        Optional<CasoDeUsoEstoque.RegisterItemEstoqueCommand> command = abrirDialogoProduto(item);
        command.ifPresent(value -> {
            executar(() -> casoDeUsoEstoque.updateItem(value));
            refresh();
        });
    }

    @FXML
    private void onInativarProduto() {
        ProdutoRow row = produtosTable == null ? null : produtosTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            mostrar("Selecione um produto.");
            return;
        }
        executar(() -> casoDeUsoEstoque.inativarItem(row.id()));
        refresh();
    }

    @FXML
    private void onFiltrarBaixoEstoque() {
        somenteBaixoEstoque = !somenteBaixoEstoque;
        refresh();
    }

    private void refresh() {
        List<ItemEstoque> items = somenteBaixoEstoque ? casoDeUsoEstoque.listLowStock() : casoDeUsoEstoque.listAll();
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
                            item.id(),
                            item.name(),
                            item.description(),
                            item.sku(),
                            item.unit(),
                            item.costPrice(),
                            item.salePrice(),
                            item.quantity(),
                            item.minimumQuantity(),
                            item.ativo(),
                            item.isLowStock()
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
        configureProdutoColumn(produtoQuantidadeColumn, 4, row -> row.quantidade() + " " + row.unidade() + (row.baixoEstoque() ? " - baixo" : ""));
        configureProdutoColumn(produtoMinimoColumn, 5, row -> String.valueOf(row.minimo()));
        configureProdutoColumn(produtoStatusColumn, 6, row -> row.ativo() ? "Ativo" : "Inativo");

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

    private Optional<CasoDeUsoEstoque.RegisterItemEstoqueCommand> abrirDialogoProduto(ItemEstoque item) {
        Dialog<CasoDeUsoEstoque.RegisterItemEstoqueCommand> dialog = new Dialog<>();
        dialog.setTitle("Editar Produto");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField nome = new TextField(item.name());
        TextField descricao = new TextField(item.description());
        TextField sku = new TextField(item.sku());
        ComboBox<String> unidade = new ComboBox<>();
        unidade.getItems().setAll("un", "litro", "kg", "caixa", "pacote", "frasco");
        unidade.getSelectionModel().select(item.unit());
        TextField custo = new TextField(item.costPrice().toPlainString());
        TextField venda = new TextField(item.salePrice().toPlainString());
        TextField minimo = new TextField(String.valueOf(item.minimumQuantity()));
        CheckBox ativo = new CheckBox("Ativo");
        ativo.setSelected(item.ativo());
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.addRow(0, new Label("Nome"), nome);
        grid.addRow(1, new Label("Descricao"), descricao);
        grid.addRow(2, new Label("SKU"), sku);
        grid.addRow(3, new Label("Unidade"), unidade);
        grid.addRow(4, new Label("Custo"), custo);
        grid.addRow(5, new Label("Venda"), venda);
        grid.addRow(6, new Label("Minimo"), minimo);
        grid.add(ativo, 1, 7);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? new CasoDeUsoEstoque.RegisterItemEstoqueCommand(
                item.id(), nome.getText(), descricao.getText(), sku.getText(), new BigDecimal(custo.getText().replace(",", ".")),
                new BigDecimal(venda.getText().replace(",", ".")), item.quantity(), Integer.parseInt(minimo.getText()),
                unidade.getValue(), ativo.isSelected()) : null);
        return dialog.showAndWait();
    }

    private void executar(Runnable runnable) {
        try {
            runnable.run();
        } catch (IllegalArgumentException exception) {
            mostrar(exception.getMessage());
        }
    }

    private void mostrar(String message) {
        new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).showAndWait();
    }

    private record ProdutoRow(
            String id,
            String nome,
            String descricao,
            String sku,
            String unidade,
            BigDecimal custo,
            BigDecimal venda,
            int quantidade,
            int minimo,
            boolean ativo,
            boolean baixoEstoque
    ) {
    }
}

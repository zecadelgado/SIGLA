package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.aplicacao.usuarios.porta.entrada.CasoDeUsoUsuario;
import br.com.sigla.relatorios.etiqueta.ServicoRelatorioEtiqueta;
import br.com.sigla.dominio.estoque.ItemEstoque;
import br.com.sigla.interfacegrafica.aplicativo.SessaoLocalAplicacao;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorData;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorMoeda;
import br.com.sigla.interfacegrafica.async.ExecutorTarefasUi;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaOrdemServico;
import br.com.sigla.interfacegrafica.formatador.FormatadorMascaraMoeda;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ControladorEstoque extends ControladorComMenuPrincipal {

    private final CasoDeUsoCliente casoDeUsoCliente;
    private final CasoDeUsoEstoque casoDeUsoEstoque;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final ApresentadorMoeda apresentadorMoeda;
    private final ApresentadorData apresentadorData;
    private final FormatadorMascaraMoeda formatadorMoeda;
    private final ServicoRelatorioEtiqueta servicoRelatorioEtiqueta;
    private final ExecutorTarefasUi executorTarefasUi;
    private final SessaoLocalAplicacao sessaoLocalAplicacao;
    private final CasoDeUsoUsuario casoDeUsoUsuario;
    private final ServicoConsultaOrdemServico servicoConsultaOrdemServico;

    private Map<String, String> clienteNomes = Map.of();
    private Map<String, String> usuarioNomes = Map.of();
    private Map<String, String> ordemNumeros = Map.of();
    private int geracaoRefresh;

    @FXML
    private Label totalProdutosLabel;
    @FXML
    private Label valorTotalLabel;
    @FXML
    private Label produtosBaixaLabel;
    @FXML
    private VBox alertaBox;
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
    private TableColumn<CasoDeUsoEstoque.InventoryMovementView, String> movimentoValorTotalColumn;
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
            ApresentadorData apresentadorData,
            FormatadorMascaraMoeda formatadorMoeda,
            ServicoRelatorioEtiqueta servicoRelatorioEtiqueta,
            ExecutorTarefasUi executorTarefasUi,
            SessaoLocalAplicacao sessaoLocalAplicacao,
            CasoDeUsoUsuario casoDeUsoUsuario,
            ServicoConsultaOrdemServico servicoConsultaOrdemServico
    ) {
        super(gerenciadorNavegacao);
        this.casoDeUsoCliente = casoDeUsoCliente;
        this.casoDeUsoEstoque = casoDeUsoEstoque;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
        this.apresentadorMoeda = apresentadorMoeda;
        this.apresentadorData = apresentadorData;
        this.formatadorMoeda = formatadorMoeda;
        this.servicoRelatorioEtiqueta = servicoRelatorioEtiqueta;
        this.executorTarefasUi = executorTarefasUi;
        this.sessaoLocalAplicacao = sessaoLocalAplicacao;
        this.casoDeUsoUsuario = casoDeUsoUsuario;
        this.servicoConsultaOrdemServico = servicoConsultaOrdemServico;
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
    private void onSaida() {
        ProdutoRow row = produtosTable == null ? null : produtosTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            mostrar("Selecione um produto.");
            return;
        }
        try {
            abrirDialogoSaida(row).ifPresent(command -> {
                casoDeUsoEstoque.recordMovement(command);
                refresh();
            });
        } catch (Exception exception) {
            mostrar(br.com.sigla.interfacegrafica.util.MensagensErro.descrever(exception));
        }
    }

    private Optional<CasoDeUsoEstoque.RecordInventoryMovementCommand> abrirDialogoSaida(ProdutoRow row) {
        Dialog<CasoDeUsoEstoque.RecordInventoryMovementCommand> dialog = new Dialog<>();
        br.com.sigla.interfacegrafica.util.DialogoUi.estilizar(dialog);
        dialog.setTitle("Saída de Estoque");
        dialog.setHeaderText("Dar baixa/descartar itens de: " + row.nome());
        ButtonType confirmar = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmar, cancelar);
        TextField quantidade = new TextField();
        quantidade.setPromptText("Quantidade (disponível: "
                + br.com.sigla.interfacegrafica.util.FormatadorQuantidade.formatar(row.quantidade())
                + " " + row.unidade() + ")");
        TextField motivo = new TextField();
        motivo.setPromptText("Motivo do descarte");
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.addRow(0, new Label("Quantidade"), quantidade);
        grid.addRow(1, new Label("Motivo"), motivo);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> {
            if (button != confirmar) {
                return null;
            }
            BigDecimal qtd = br.com.sigla.interfacegrafica.util.FormatadorQuantidade.parse(quantidade.getText());
            return new CasoDeUsoEstoque.RecordInventoryMovementCommand(
                    row.id(),
                    UUID.randomUUID().toString(),
                    ItemEstoque.MovementType.SAIDA,
                    qtd,
                    LocalDate.now(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    usuarioAtual(),
                    "",
                    "",
                    "Descarte",
                    motivo.getText() == null ? "" : motivo.getText().trim());
        });
        return dialog.showAndWait();
    }

    private String usuarioAtual() {
        return sessaoLocalAplicacao == null || sessaoLocalAplicacao.usuarioAtual() == null
                ? ""
                : sessaoLocalAplicacao.usuarioAtual().id();
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
    private void onReativarProduto() {
        ProdutoRow row = produtosTable == null ? null : produtosTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            mostrar("Selecione um produto.");
            return;
        }
        executar(() -> casoDeUsoEstoque.reativarItem(row.id()));
        refresh();
    }

    @FXML
    private void onFiltrarBaixoEstoque() {
        somenteBaixoEstoque = !somenteBaixoEstoque;
        refresh();
    }

    @FXML
    private void onImprimirEtiqueta() {
        ProdutoRow row = produtosTable == null ? null : produtosTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            mostrar("Selecione um produto.");
            return;
        }
        try {
            Path arquivo = servicoRelatorioEtiqueta.imprimir(new ServicoRelatorioEtiqueta.DadosEtiqueta(
                    row.sku() == null || row.sku().isBlank() ? row.id() : row.sku(),
                    row.nome(),
                    row.unidade(),
                    apresentadorMoeda.format(row.venda())
            ));
            br.com.sigla.interfacegrafica.util.DialogoUi.informacao("Etiqueta gerada em:\n" + arquivo);
        } catch (Exception exception) {
            mostrar(br.com.sigla.interfacegrafica.util.MensagensErro.descrever("Não foi possível gerar a etiqueta:", exception));
        }
    }

    private void refresh() {
        boolean baixo = somenteBaixoEstoque;
        int geracao = ++geracaoRefresh;
        executorTarefasUi.executar(
                () -> carregar(baixo),
                dados -> {
                    if (geracao == geracaoRefresh) {
                        aplicar(dados);
                    }
                });
    }

    private EstoqueSnapshot carregar(boolean baixo) {
        List<ItemEstoque> items = baixo ? casoDeUsoEstoque.listLowStock() : casoDeUsoEstoque.listAll();
        List<CasoDeUsoEstoque.InventoryMovementView> movimentos = casoDeUsoEstoque.listMovements();
        Map<String, String> nomes = casoDeUsoCliente.listAll().stream()
                .collect(Collectors.toMap(cliente -> cliente.id(), cliente -> cliente.name(), (a, b) -> a));
        Map<String, String> usuarios = carregarMapaSeguro(() -> casoDeUsoUsuario.listAll().stream()
                .collect(Collectors.toMap(usuario -> usuario.id(), usuario -> usuario.nome(), (a, b) -> a)));
        Map<String, String> ordens = carregarMapaSeguro(() -> servicoConsultaOrdemServico.listAll().stream()
                .collect(Collectors.toMap(ordem -> ordem.id(), ordem -> ordem.numero(), (a, b) -> a)));
        return new EstoqueSnapshot(items, movimentos, nomes, usuarios, ordens);
    }

    private Map<String, String> carregarMapaSeguro(java.util.function.Supplier<Map<String, String>> fonte) {
        try {
            return fonte.get();
        } catch (Exception excecao) {
            // Resolução de nomes é apenas cosmética: se a listagem falhar (ex.: acesso
            // restrito), mostra o id cru em vez de quebrar a atualização da tela de estoque.
            return Map.of();
        }
    }

    private record EstoqueSnapshot(
            List<ItemEstoque> items,
            List<CasoDeUsoEstoque.InventoryMovementView> movimentos,
            Map<String, String> clienteNomes,
            Map<String, String> usuarioNomes,
            Map<String, String> ordemNumeros
    ) {
    }

    private void aplicar(EstoqueSnapshot dados) {
        clienteNomes = dados.clienteNomes();
        usuarioNomes = dados.usuarioNomes();
        ordemNumeros = dados.ordemNumeros();
        List<ItemEstoque> items = dados.items();
        if (totalProdutosLabel != null) {
            totalProdutosLabel.setText(String.valueOf(items.size()));
        }
        BigDecimal valorTotal = items.stream()
                .map(item -> item.salePrice().multiply(item.quantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (valorTotalLabel != null) {
            valorTotalLabel.setText(apresentadorMoeda.format(valorTotal));
        }
        long lowCount = items.stream().filter(item -> item.isLowStock()).count();
        if (produtosBaixaLabel != null) {
            produtosBaixaLabel.setText(String.valueOf(lowCount));
        }
        if (alertaBox != null) {
            boolean alertaVisivel = lowCount > 0;
            alertaBox.setVisible(alertaVisivel);
            alertaBox.setManaged(alertaVisivel);
        }
        if (alertaLabel != null) {
            alertaLabel.setText(lowCount == 0
                    ? "Nenhum produto em baixa."
                    : items.stream()
                    .filter(item -> item.isLowStock())
                    .map(item -> item.name() + " ("
                            + br.com.sigla.interfacegrafica.util.FormatadorQuantidade.formatar(item.quantity()) + "/"
                            + br.com.sigla.interfacegrafica.util.FormatadorQuantidade.formatar(item.minimumQuantity()) + ")")
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
            movimentacoesTable.getItems().setAll(dados.movimentos());
        }
    }

    private void configureTables() {
        configureProdutoColumn(produtoNomeColumn, 0, row -> row.nome());
        configureProdutoColumn(produtoDescricaoColumn, 1, row -> row.descricao());
        configureProdutoColumn(produtoCustoColumn, 2, row -> apresentadorMoeda.format(row.custo()));
        configureProdutoColumn(produtoVendaColumn, 3, row -> apresentadorMoeda.format(row.venda()));
        configureProdutoColumn(produtoQuantidadeColumn, 4, row ->
                br.com.sigla.interfacegrafica.util.FormatadorQuantidade.formatar(row.quantidade())
                        + " " + row.unidade() + (row.baixoEstoque() ? " - baixo" : ""));
        configureProdutoColumn(produtoMinimoColumn, 5, row ->
                br.com.sigla.interfacegrafica.util.FormatadorQuantidade.formatar(row.minimo()));
        configureProdutoColumn(produtoStatusColumn, 6, row -> row.ativo() ? "Ativo" : "Inativo");

        configureMovimentoColumn(movimentoProdutoColumn, 0, row -> row.itemName());
        configureMovimentoColumn(movimentoTipoColumn, 1, row -> row.type().name());
        configureMovimentoColumn(movimentoQuantidadeColumn, 2, row ->
                br.com.sigla.interfacegrafica.util.FormatadorQuantidade.formatar(row.amount()));
        configureMovimentoColumn(movimentoValorColumn, 3, row -> apresentadorMoeda.format(row.unitPrice()));
        configureMovimentoColumn(movimentoValorTotalColumn, 4, row -> apresentadorMoeda.format(row.totalPrice()));
        configureMovimentoColumn(movimentoUsuarioColumn, 5, row -> resolveUsuario(row.createdBy()));
        configureMovimentoColumn(movimentoClienteColumn, 6, row -> resolveCliente(row.customerId()));
        configureMovimentoColumn(movimentoOrdemColumn, 7, row -> resolveOrdem(row.orderReference()));
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
        return clienteNomes.getOrDefault(customerId, customerId);
    }

    private String resolveUsuario(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank()) {
            return "-";
        }
        return usuarioNomes.getOrDefault(usuarioId, usuarioId);
    }

    private String resolveOrdem(String ordemId) {
        if (ordemId == null || ordemId.isBlank()) {
            return "-";
        }
        return ordemNumeros.getOrDefault(ordemId, ordemId);
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
        br.com.sigla.interfacegrafica.util.DialogoUi.estilizar(dialog);
        dialog.setTitle("Editar Produto");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField nome = new TextField(item.name());
        TextField descricao = new TextField(item.description());
        TextField sku = new TextField(item.sku());
        ComboBox<String> unidade = new ComboBox<>();
        unidade.getItems().setAll("un", "litro", "kg", "caixa", "pacote", "frasco");
        unidade.getSelectionModel().select(item.unit());
        TextField custo = new TextField();
        formatadorMoeda.aplicar(custo);
        formatadorMoeda.definir(custo, item.costPrice());
        TextField venda = new TextField();
        formatadorMoeda.aplicar(venda);
        formatadorMoeda.definir(venda, item.salePrice());
        TextField minimo = new TextField(String.valueOf(item.minimumQuantity()));
        CheckBox ativo = new CheckBox("Ativo");
        ativo.setSelected(item.ativo());
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.addRow(0, new Label("Nome"), nome);
        grid.addRow(1, new Label("Descrição"), descricao);
        grid.addRow(2, new Label("SKU"), sku);
        grid.addRow(3, new Label("Unidade"), unidade);
        grid.addRow(4, new Label("Custo"), custo);
        grid.addRow(5, new Label("Venda"), venda);
        grid.addRow(6, new Label("Mínimo"), minimo);
        grid.add(ativo, 1, 7);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? new CasoDeUsoEstoque.RegisterItemEstoqueCommand(
                item.id(), nome.getText(), descricao.getText(), sku.getText(), formatadorMoeda.valor(custo),
                formatadorMoeda.valor(venda), item.quantity(),
                br.com.sigla.interfacegrafica.util.FormatadorQuantidade.parse(minimo.getText()),
                unidade.getValue(), ativo.isSelected()) : null);
        return dialog.showAndWait();
    }

    private void executar(Runnable runnable) {
        try {
            runnable.run();
        } catch (IllegalArgumentException exception) {
            mostrar(br.com.sigla.interfacegrafica.util.MensagensErro.descrever(exception));
        }
    }

    private void mostrar(String message) {
        br.com.sigla.interfacegrafica.util.DialogoUi.informacao(message);
    }

    private record ProdutoRow(
            String id,
            String nome,
            String descricao,
            String sku,
            String unidade,
            BigDecimal custo,
            BigDecimal venda,
            BigDecimal quantidade,
            BigDecimal minimo,
            boolean ativo,
            boolean baixoEstoque
    ) {
    }
}

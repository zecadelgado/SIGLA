package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.relatorios.formulario.FormularioOrdemServico;
import br.com.sigla.relatorios.formulario.FormularioVisita;
import br.com.sigla.relatorios.ordemservico.ServicoRelatorioOrdemServico;
import br.com.sigla.relatorios.visita.ServicoRelatorioVisita;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.dominio.servicos.OrdemServico;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorData;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorMoeda;
import br.com.sigla.interfacegrafica.consulta.ContextoDetalheOrdemServico;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaOrdemServico;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaReferencias;
import br.com.sigla.interfacegrafica.formatador.FormatadorMascaraMoeda;
import br.com.sigla.interfacegrafica.async.ExecutorTarefasUi;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.ResolvedorEntradaTexto;
import br.com.sigla.interfacegrafica.util.ValidadorEntrada;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
public class ControladorOrdemServico extends ControladorComMenuPrincipal {

    private final ServicoConsultaOrdemServico servicoConsultaOrdemServico;
    private final CasoDeUsoOrdemServico casoDeUsoOrdemServico;
    private final ServicoConsultaReferencias servicoConsultaReferencias;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final ContextoDetalheOrdemServico contextoDetalheOrdemServico;
    private final ApresentadorMoeda apresentadorMoeda;
    private final ApresentadorData apresentadorData;
    private final FormatadorMascaraMoeda formatadorMoeda;
    private final ServicoRelatorioOrdemServico servicoRelatorioOrdemServico;
    private final ServicoRelatorioVisita servicoRelatorioVisita;
    private final CasoDeUsoCliente casoDeUsoCliente;
    private final ExecutorTarefasUi executorTarefasUi;

    private int geracaoRefresh;

    @FXML
    private Label abertasLabel;
    @FXML
    private Label andamentoLabel;
    @FXML
    private Label concluidasLabel;
    @FXML
    private Label faturamentoLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private TableView<ServicoConsultaOrdemServico.OrdemServicoView> ordensTable;
    @FXML
    private TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> numeroColumn;
    @FXML
    private TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> clienteColumn;
    @FXML
    private TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> tituloColumn;
    @FXML
    private TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> tipoColumn;
    @FXML
    private TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> responsavelColumn;
    @FXML
    private TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> emissaoColumn;
    @FXML
    private TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> valorColumn;
    @FXML
    private TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> pagoColumn;
    @FXML
    private TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> statusColumn;

    public ControladorOrdemServico(
            ServicoConsultaOrdemServico servicoConsultaOrdemServico,
            CasoDeUsoOrdemServico casoDeUsoOrdemServico,
            ServicoConsultaReferencias servicoConsultaReferencias,
            GerenciadorNavegacao gerenciadorNavegacao,
            ContextoDetalheOrdemServico contextoDetalheOrdemServico,
            ApresentadorMoeda apresentadorMoeda,
            ApresentadorData apresentadorData,
            FormatadorMascaraMoeda formatadorMoeda,
            ServicoRelatorioOrdemServico servicoRelatorioOrdemServico,
            ServicoRelatorioVisita servicoRelatorioVisita,
            CasoDeUsoCliente casoDeUsoCliente,
            ExecutorTarefasUi executorTarefasUi
    ) {
        super(gerenciadorNavegacao);
        this.servicoConsultaOrdemServico = servicoConsultaOrdemServico;
        this.casoDeUsoOrdemServico = casoDeUsoOrdemServico;
        this.servicoConsultaReferencias = servicoConsultaReferencias;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
        this.contextoDetalheOrdemServico = contextoDetalheOrdemServico;
        this.apresentadorMoeda = apresentadorMoeda;
        this.apresentadorData = apresentadorData;
        this.formatadorMoeda = formatadorMoeda;
        this.servicoRelatorioOrdemServico = servicoRelatorioOrdemServico;
        this.servicoRelatorioVisita = servicoRelatorioVisita;
        this.casoDeUsoCliente = casoDeUsoCliente;
        this.executorTarefasUi = executorTarefasUi;
    }

    @FXML
    public void initialize() {
        if (statusCombo != null) {
            statusCombo.getItems().setAll("Todos", "ABERTA", "EM_ANDAMENTO", "CONCLUIDA", "CANCELADA", "ATRASADA");
            statusCombo.getSelectionModel().selectFirst();
            statusCombo.valueProperty().addListener((observable, oldValue, newValue) -> refresh());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> refresh());
        }
        configureTable();
        if (ordensTable != null) {
            ordensTable.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    onDetalharOrdem();
                }
            });
        }
        refresh();
    }

    @FXML
    private void onNovaOrdem() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.NEW_SERVICE_ORDER);
    }

    @FXML
    private void onEditarOrdem() {
        var selected = selecionada();
        if (selected == null) {
            return;
        }
        Optional<CasoDeUsoOrdemServico.UpdateOrdemServicoCommand> command = abrirDialogoEdicao(selected);
        command.ifPresent(value -> {
            executar(() -> casoDeUsoOrdemServico.update(value));
            refresh();
        });
    }

    @FXML
    private void onConcluirOrdem() {
        var selected = ordensTable == null ? null : ordensTable.getSelectionModel().getSelectedItem();
        if (selected == null || "CONCLUIDA".equals(selected.status())) {
            return;
        }
        executar(() -> casoDeUsoOrdemServico.conclude(new CasoDeUsoOrdemServico.ConcluirOrdemServicoCommand(selected.id(), "", null, selected.signed())));
        refresh();
    }

    @FXML
    private void onCancelarOrdem() {
        var selected = ordensTable == null ? null : ordensTable.getSelectionModel().getSelectedItem();
        if (selected == null || "CANCELADA".equals(selected.status())) {
            return;
        }
        executar(() -> casoDeUsoOrdemServico.cancel(new CasoDeUsoOrdemServico.CancelarOrdemServicoCommand(selected.id(), "Cancelada pela tela de OS.")));
        refresh();
    }

    @FXML
    private void onIniciarOrdem() {
        var selected = selecionada();
        if (selected != null) {
            executar(() -> casoDeUsoOrdemServico.start(selected.id()));
            refresh();
        }
    }

    @FXML
    private void onMarcarPago() {
        var selected = selecionada();
        if (selected != null) {
            executar(() -> casoDeUsoOrdemServico.marcarPago(selected.id(), true));
            refresh();
        }
    }

    @FXML
    private void onAdicionarProdutoOrdem() {
        var selected = selecionada();
        if (selected == null) {
            return;
        }
        executar(() -> abrirDialogoProduto(selected.id()).ifPresent(value -> {
            casoDeUsoOrdemServico.adicionarProduto(value);
            refresh();
        }));
    }

    @FXML
    private void onAnexarOrdem() {
        var selected = selecionada();
        if (selected == null) {
            return;
        }
        Optional<CasoDeUsoOrdemServico.AnexarOrdemServicoCommand> command = abrirDialogoAnexo(selected.id());
        command.ifPresent(value -> {
            executar(() -> casoDeUsoOrdemServico.anexar(value));
            refresh();
        });
    }

    @FXML
    private void onDetalharOrdem() {
        var selected = selecionada();
        if (selected == null) {
            return;
        }
        contextoDetalheOrdemServico.selecionarOrdem(selected.emissionDate(), selected.id());
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.SERVICE_DAY_DETAILS);
    }

    @FXML
    private void onImprimirOrdem() {
        var selected = selecionada();
        if (selected == null) {
            return;
        }
        try {
            Cliente cliente = clientePorId(selected.customerId());
            Path arquivo = servicoRelatorioOrdemServico.imprimir(new FormularioOrdemServico.Dados(
                    apresentadorData.format(selected.emissionDate()),
                    semTraco(selected.responsible()),
                    selected.customerName(),
                    documentoCliente(cliente),
                    cliente == null ? "" : cliente.email(),
                    cliente == null ? "" : cliente.phone(),
                    "",
                    "",
                    semTraco(selected.notes())
            ));
            new Alert(Alert.AlertType.INFORMATION, "OS gerada em:\n" + arquivo, ButtonType.OK).showAndWait();
        } catch (Exception exception) {
            mostrar(br.com.sigla.interfacegrafica.util.MensagensErro.descrever("Não foi possível gerar a OS:", exception));
        }
    }

    @FXML
    private void onRelatorioVisita() {
        var selected = selecionada();
        if (selected == null) {
            return;
        }
        try {
            Cliente cliente = clientePorId(selected.customerId());
            Path arquivo = servicoRelatorioVisita.imprimir(new FormularioVisita.Dados(
                    apresentadorData.format(selected.emissionDate()),
                    semTraco(selected.responsible()),
                    "",
                    "",
                    selected.customerName(),
                    documentoCliente(cliente),
                    cliente == null ? "" : cliente.location(),
                    cliente == null ? "" : cliente.phone(),
                    cliente == null ? "" : cliente.cidade(),
                    cliente == null ? "" : cliente.estado(),
                    responsavelCliente(cliente),
                    selected.customerName(),
                    semTraco(selected.responsible())
            ));
            new Alert(Alert.AlertType.INFORMATION, "Relatório de visita gerado em:\n" + arquivo, ButtonType.OK).showAndWait();
        } catch (Exception exception) {
            mostrar(br.com.sigla.interfacegrafica.util.MensagensErro.descrever("Não foi possível gerar o relatório de visita:", exception));
        }
    }

    private Cliente clientePorId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return casoDeUsoCliente.listAll().stream()
                .filter(cliente -> cliente.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    private String documentoCliente(Cliente cliente) {
        if (cliente == null) {
            return "";
        }
        return cliente.cnpj().isBlank() ? cliente.cpf() : cliente.cnpj();
    }

    private String responsavelCliente(Cliente cliente) {
        if (cliente == null) {
            return "";
        }
        return cliente.contacts().stream()
                .filter(Cliente.ContactPerson::principal)
                .map(Cliente.ContactPerson::name)
                .findFirst()
                .orElse(cliente.contacts().isEmpty() ? "" : cliente.contacts().getFirst().name());
    }

    private String semTraco(String valor) {
        return valor == null || valor.equals("-") ? "" : valor;
    }

    private void refresh() {
        int geracao = ++geracaoRefresh;
        executorTarefasUi.executar(
                () -> new OsSnapshot(servicoConsultaOrdemServico.summary(), servicoConsultaOrdemServico.listAll()),
                dados -> {
                    if (geracao == geracaoRefresh) {
                        aplicar(dados);
                    }
                });
    }

    private record OsSnapshot(
            ServicoConsultaOrdemServico.OrdemServicoResumo resumo,
            java.util.List<ServicoConsultaOrdemServico.OrdemServicoView> ordens
    ) {
    }

    private void aplicar(OsSnapshot dados) {
        var resumo = dados.resumo();
        if (abertasLabel != null) {
            abertasLabel.setText(String.valueOf(resumo.abertas()));
        }
        if (andamentoLabel != null) {
            andamentoLabel.setText(String.valueOf(resumo.emAndamento()));
        }
        if (concluidasLabel != null) {
            concluidasLabel.setText(String.valueOf(resumo.concluidas()));
        }
        if (faturamentoLabel != null) {
            faturamentoLabel.setText(apresentadorMoeda.format(resumo.faturamento()));
        }
        if (ordensTable != null) {
            String termo = searchField == null || searchField.getText() == null ? "" : searchField.getText().toLowerCase(Locale.ROOT);
            String filtroStatus = statusCombo == null || statusCombo.getValue() == null ? "Todos" : statusCombo.getValue();
            ordensTable.getItems().setAll(dados.ordens().stream()
                    .filter(order -> termo.isBlank()
                            || order.id().toLowerCase(Locale.ROOT).contains(termo)
                            || order.customerName().toLowerCase(Locale.ROOT).contains(termo)
                            || order.responsible().toLowerCase(Locale.ROOT).contains(termo)
                            || order.serviceType().toLowerCase(Locale.ROOT).contains(termo)
                            || order.title().toLowerCase(Locale.ROOT).contains(termo))
                    .filter(order -> "Todos".equals(filtroStatus) || order.status().equals(filtroStatus))
                    .toList());
        }
    }

    private void configureTable() {
        configureColumn(numeroColumn, 0, row -> row.numero());
        configureColumn(clienteColumn, 1, row -> row.customerName());
        configureColumn(tituloColumn, 2, row -> row.title());
        configureColumn(tipoColumn, 3, row -> row.serviceType());
        configureColumn(responsavelColumn, 4, row -> row.responsible());
        configureColumn(emissaoColumn, 5, row -> apresentadorData.format(row.emissionDate()));
        configureColumn(valorColumn, 6, row -> apresentadorMoeda.format(row.amount()));
        configureColumn(pagoColumn, 7, row -> row.paid() ? "Sim" : "Não");
        configureColumn(statusColumn, 8, row -> row.status());
    }

    private ServicoConsultaOrdemServico.OrdemServicoView selecionada() {
        var selected = ordensTable == null ? null : ordensTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            mostrar("Selecione uma OS.");
        }
        return selected;
    }

    private Optional<CasoDeUsoOrdemServico.AdicionarProdutoOrdemCommand> abrirDialogoProduto(String ordemId) {
        Dialog<CasoDeUsoOrdemServico.AdicionarProdutoOrdemCommand> dialog = new Dialog<>();
        dialog.setTitle("Produto da OS");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField produto = new TextField();
        TextField quantidade = new TextField("1");
        TextField valor = new TextField();
        formatadorMoeda.aplicar(valor);
        GridPane grid = grid();
        grid.addRow(0, new Label("Produto"), produto);
        grid.addRow(1, new Label("Quantidade"), quantidade);
        grid.addRow(2, new Label("Valor unitário"), valor);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            ValidadorEntrada validador = ValidadorEntrada.nova();
            var opcao = ResolvedorEntradaTexto.resolveOpcional(servicoConsultaReferencias.produtos(), produto.getText());
            validador.selecao(opcao, "um produto válido");
            int qtd = validador.inteiroPositivo(quantidade.getText(), "a quantidade");
            validador.validar();
            return new CasoDeUsoOrdemServico.AdicionarProdutoOrdemCommand(
                    ordemId,
                    UUID.randomUUID().toString(),
                    opcao.id(),
                    qtd,
                    formatadorMoeda.valor(valor)
            );
        });
        return dialog.showAndWait();
    }

    private Optional<CasoDeUsoOrdemServico.UpdateOrdemServicoCommand> abrirDialogoEdicao(ServicoConsultaOrdemServico.OrdemServicoView selected) {
        Dialog<CasoDeUsoOrdemServico.UpdateOrdemServicoCommand> dialog = new Dialog<>();
        dialog.setTitle("Editar OS");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField cliente = new TextField(selected.customerId());
        TextField titulo = new TextField(selected.title());
        TextArea descricao = new TextArea(selected.description());
        descricao.setPrefRowCount(2);
        TextField tipo = new TextField(selected.serviceType());
        TextField responsavel = new TextField(selected.responsible().equals("-") ? "" : selected.responsible());
        DatePicker dataPicker = new DatePicker(selected.emissionDate() == null ? LocalDate.now() : selected.emissionDate());
        TextField valor = new TextField();
        formatadorMoeda.aplicar(valor);
        formatadorMoeda.definir(valor, selected.amount().subtract(selected.productTotal()));
        TextArea observacoes = new TextArea(selected.notes().equals("-") ? "" : selected.notes());
        observacoes.setPrefRowCount(3);
        GridPane grid = grid();
        grid.addRow(0, new Label("Cliente"), cliente);
        grid.addRow(1, new Label("Título"), titulo);
        grid.addRow(2, new Label("Descrição"), descricao);
        grid.addRow(3, new Label("Tipo"), tipo);
        grid.addRow(4, new Label("Responsável"), responsavel);
        grid.addRow(5, new Label("Data agendada"), dataPicker);
        grid.addRow(6, new Label("Valor do serviço"), valor);
        grid.addRow(7, new Label("Observações"), observacoes);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            // Evita gravar id inexistente (violacao de FK): so usa ids validos resolvidos das referencias.
            // Cliente nao resolvido mantem o cliente atual da OS; responsavel nao resolvido fica vazio.
            var opcaoCliente = ResolvedorEntradaTexto.resolveOpcional(servicoConsultaReferencias.clientes(), cliente.getText());
            String clienteId = opcaoCliente != null ? opcaoCliente.id() : selected.customerId();
            var opcaoResponsavel = ResolvedorEntradaTexto.resolveOpcional(servicoConsultaReferencias.funcionarios(), responsavel.getText());
            String responsavelId = opcaoResponsavel != null ? opcaoResponsavel.id() : "";
            return new CasoDeUsoOrdemServico.UpdateOrdemServicoCommand(
                    selected.id(),
                    clienteId,
                        selected.contractId(),
                        titulo.getText(),
                        descricao.getText(),
                    tipo.getText(),
                    (dataPicker.getValue() == null ? LocalDate.now() : dataPicker.getValue()).atStartOfDay(),
                    responsavelId,
                    formatadorMoeda.valor(valor),
                    observacoes.getText()
            );
        });
        return dialog.showAndWait();
    }

    private Optional<CasoDeUsoOrdemServico.AnexarOrdemServicoCommand> abrirDialogoAnexo(String ordemId) {
        Dialog<CasoDeUsoOrdemServico.AnexarOrdemServicoCommand> dialog = new Dialog<>();
        dialog.setTitle("Anexo da OS");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ComboBox<OrdemServico.TipoAnexo> tipo = new ComboBox<>();
        tipo.getItems().setAll(OrdemServico.TipoAnexo.values());
        tipo.getSelectionModel().select(OrdemServico.TipoAnexo.OUTRO);
        TextField nome = new TextField();
        TextField caminho = new TextField();
        TextField mime = new TextField("application/octet-stream");
        TextArea descricao = new TextArea();
        descricao.setPrefRowCount(3);
        GridPane grid = grid();
        grid.addRow(0, new Label("Tipo"), tipo);
        grid.addRow(1, new Label("Nome do arquivo"), nome);
        grid.addRow(2, new Label("Caminho"), caminho);
        grid.addRow(3, new Label("MIME"), mime);
        grid.addRow(4, new Label("Descrição"), descricao);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? new CasoDeUsoOrdemServico.AnexarOrdemServicoCommand(
                ordemId,
                UUID.randomUUID().toString(),
                tipo.getValue(),
                nome.getText(),
                caminho.getText(),
                mime.getText(),
                0,
                descricao.getText(),
                ""
        ) : null);
        return dialog.showAndWait();
    }

    private GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        return grid;
    }

    private void executar(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception exception) {
            mostrar(br.com.sigla.interfacegrafica.util.MensagensErro.descrever(exception));
        }
    }

    private void mostrar(String message) {
        new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).showAndWait();
    }

    private void configureColumn(TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> column, int fallbackIndex, java.util.function.Function<ServicoConsultaOrdemServico.OrdemServicoView, String> getter) {
        TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> target = column != null ? column : getColumn(fallbackIndex);
        if (target != null) {
            target.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        }
    }

    @SuppressWarnings("unchecked")
    private TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> getColumn(int index) {
        if (ordensTable == null || ordensTable.getColumns().size() <= index) {
            return null;
        }
        return (TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String>) ordensTable.getColumns().get(index);
    }
}

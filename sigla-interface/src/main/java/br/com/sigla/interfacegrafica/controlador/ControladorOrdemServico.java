package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.aplicacao.servicos.porta.saida.PortaArmazenamentoAnexo;
import br.com.sigla.relatorios.formulario.FormularioOrdemServico;
import br.com.sigla.relatorios.formulario.FormularioVisita;
import br.com.sigla.relatorios.ordemservico.ServicoRelatorioOrdemServico;
import br.com.sigla.relatorios.visita.ServicoRelatorioVisita;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.dominio.servicos.DadosFormularioServico;
import br.com.sigla.dominio.servicos.OrdemServico;
import br.com.sigla.interfacegrafica.dialogo.DialogoDadosVisita;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorData;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorMoeda;
import br.com.sigla.interfacegrafica.consulta.ContextoDetalheOrdemServico;
import br.com.sigla.interfacegrafica.consulta.ContextoEdicaoOrdemServico;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaOrdemServico;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaReferencias;
import br.com.sigla.interfacegrafica.formatador.FormatadorMascaraMoeda;
import br.com.sigla.interfacegrafica.async.ExecutorTarefasUi;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.modelo.OpcaoId;
import br.com.sigla.interfacegrafica.util.UtilComboBox;
import br.com.sigla.interfacegrafica.util.ValidadorEntrada;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Path;
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
    private final ContextoEdicaoOrdemServico contextoEdicaoOrdemServico;
    private final ApresentadorMoeda apresentadorMoeda;
    private final ApresentadorData apresentadorData;
    private final FormatadorMascaraMoeda formatadorMoeda;
    private final ServicoRelatorioOrdemServico servicoRelatorioOrdemServico;
    private final ServicoRelatorioVisita servicoRelatorioVisita;
    private final CasoDeUsoCliente casoDeUsoCliente;
    private final ExecutorTarefasUi executorTarefasUi;
    private final PortaArmazenamentoAnexo portaArmazenamentoAnexo;

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
    @FXML
    private javafx.scene.layout.HBox acoesOrdemBox;

    public ControladorOrdemServico(
            ServicoConsultaOrdemServico servicoConsultaOrdemServico,
            CasoDeUsoOrdemServico casoDeUsoOrdemServico,
            ServicoConsultaReferencias servicoConsultaReferencias,
            GerenciadorNavegacao gerenciadorNavegacao,
            ContextoDetalheOrdemServico contextoDetalheOrdemServico,
            ContextoEdicaoOrdemServico contextoEdicaoOrdemServico,
            ApresentadorMoeda apresentadorMoeda,
            ApresentadorData apresentadorData,
            FormatadorMascaraMoeda formatadorMoeda,
            ServicoRelatorioOrdemServico servicoRelatorioOrdemServico,
            ServicoRelatorioVisita servicoRelatorioVisita,
            CasoDeUsoCliente casoDeUsoCliente,
            ExecutorTarefasUi executorTarefasUi,
            PortaArmazenamentoAnexo portaArmazenamentoAnexo
    ) {
        super(gerenciadorNavegacao);
        this.servicoConsultaOrdemServico = servicoConsultaOrdemServico;
        this.casoDeUsoOrdemServico = casoDeUsoOrdemServico;
        this.servicoConsultaReferencias = servicoConsultaReferencias;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
        this.contextoDetalheOrdemServico = contextoDetalheOrdemServico;
        this.contextoEdicaoOrdemServico = contextoEdicaoOrdemServico;
        this.apresentadorMoeda = apresentadorMoeda;
        this.apresentadorData = apresentadorData;
        this.formatadorMoeda = formatadorMoeda;
        this.servicoRelatorioOrdemServico = servicoRelatorioOrdemServico;
        this.servicoRelatorioVisita = servicoRelatorioVisita;
        this.casoDeUsoCliente = casoDeUsoCliente;
        this.executorTarefasUi = executorTarefasUi;
        this.portaArmazenamentoAnexo = portaArmazenamentoAnexo;
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
        if (acoesOrdemBox != null && ordensTable != null) {
            // Botões de ação só ficam habilitados quando há uma OS selecionada, tornando o
            // estado da seleção inequívoco (evita "Selecione uma OS." após um clique que
            // aparentava ter selecionado a linha).
            acoesOrdemBox.disableProperty().bind(ordensTable.getSelectionModel().selectedItemProperty().isNull());
        }
        refresh();
    }

    @FXML
    private void onNovaOrdem() {
        contextoEdicaoOrdemServico.limpar();
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.NEW_SERVICE_ORDER);
    }

    @FXML
    private void onEditarOrdem() {
        var selected = selecionada();
        if (selected == null) {
            return;
        }
        // Reusa a tela de criacao em modo edicao: assim a edicao oferece exatamente
        // os mesmos campos da criacao (inclusive os que alimentam o PDF).
        contextoEdicaoOrdemServico.editar(selected.id());
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.NEW_SERVICE_ORDER);
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
            mostrar("O pagamento deve ser registrado na tela Financeiro. A OS sera atualizada automaticamente.");
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
    private void onVerAnexos() {
        var selected = selecionada();
        if (selected == null) {
            return;
        }
        OrdemServico ordem = casoDeUsoOrdemServico.listAll().stream()
                .filter(os -> os.id().equals(selected.id()))
                .findFirst()
                .orElse(null);
        if (ordem == null || ordem.anexos().isEmpty()) {
            mostrar("Esta OS não possui anexos.");
            return;
        }
        Dialog<Void> dialog = new Dialog<>();
        br.com.sigla.interfacegrafica.util.DialogoUi.estilizar(dialog);
        dialog.setTitle("Anexos da OS");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        javafx.scene.control.ListView<OrdemServico.Anexo> lista = new javafx.scene.control.ListView<>();
        lista.getItems().setAll(ordem.anexos());
        lista.setCellFactory(view -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(OrdemServico.Anexo anexo, boolean vazio) {
                super.updateItem(anexo, vazio);
                setText(vazio || anexo == null ? null
                        : anexo.nomeArquivo() + "  [" + anexo.tipo() + "]"
                        + (anexo.descricao() == null || anexo.descricao().isBlank() ? "" : " - " + anexo.descricao()));
            }
        });
        lista.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && lista.getSelectionModel().getSelectedItem() != null) {
                abrirArquivo(lista.getSelectionModel().getSelectedItem().caminhoStorage());
            }
        });
        javafx.scene.control.Button abrir = new javafx.scene.control.Button("Abrir selecionado");
        abrir.setMaxWidth(Double.MAX_VALUE);
        abrir.setOnAction(event -> {
            OrdemServico.Anexo anexo = lista.getSelectionModel().getSelectedItem();
            if (anexo == null) {
                mostrar("Selecione um anexo na lista.");
                return;
            }
            abrirArquivo(anexo.caminhoStorage());
        });
        javafx.scene.layout.VBox caixa = new javafx.scene.layout.VBox(8, lista, abrir);
        caixa.setPrefSize(480, 360);
        dialog.getDialogPane().setContent(caixa);
        dialog.showAndWait();
    }

    private void abrirArquivo(String caminho) {
        if (caminho == null || caminho.isBlank()) {
            mostrar("Anexo sem arquivo associado.");
            return;
        }
        File arquivo = new File(caminho);
        if (!arquivo.exists()) {
            mostrar("Arquivo não encontrado em: " + caminho);
            return;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(arquivo);
            } else {
                mostrar("Não foi possível abrir automaticamente. Arquivo em: " + caminho);
            }
        } catch (Exception excecao) {
            mostrar(br.com.sigla.interfacegrafica.util.MensagensErro.descrever("Não foi possível abrir o anexo:", excecao));
        }
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
            OrdemServico ordem = ordemPorId(selected.id());
            DadosFormularioServico.Os os = ordem == null
                    ? DadosFormularioServico.Os.vazio()
                    : ordem.dadosFormulario().os();
            Cliente cliente = clientePorId(selected.customerId());
            Path arquivo = servicoRelatorioOrdemServico.imprimir(new FormularioOrdemServico.Dados(
                    apresentadorData.format(selected.emissionDate()),
                    semTraco(selected.responsible()),
                    selected.customerName(),
                    documentoCliente(cliente),
                    cliente == null ? "" : cliente.email(),
                    cliente == null ? "" : cliente.phone(),
                    os.horaInicio(),
                    os.horaTermino(),
                    semTraco(selected.notes()),
                    os
            ));
            br.com.sigla.interfacegrafica.util.DialogoUi.informacao("OS gerada em:\n" + arquivo);
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
            OrdemServico ordem = ordemPorId(selected.id());
            DadosFormularioServico.Visita atual = ordem == null
                    ? DadosFormularioServico.Visita.vazio()
                    : ordem.dadosFormulario().visita();
            Optional<DadosFormularioServico.Visita> editado = DialogoDadosVisita.abrir(atual);
            if (editado.isEmpty()) {
                return;
            }
            DadosFormularioServico.Visita visita = editado.get();
            if (ordem != null) {
                executar(() -> casoDeUsoOrdemServico.atualizarDadosFormulario(
                        ordem.id(),
                        new DadosFormularioServico(ordem.dadosFormulario().os(), visita)));
            }
            Cliente cliente = clientePorId(selected.customerId());
            Path arquivo = servicoRelatorioVisita.imprimir(new FormularioVisita.Dados(
                    apresentadorData.format(selected.emissionDate()),
                    semTraco(selected.responsible()),
                    visita.horaInicio(),
                    visita.horaTermino(),
                    visita.horarioMarcado(),
                    selected.customerName(),
                    documentoCliente(cliente),
                    cliente == null ? "" : cliente.location(),
                    cliente == null ? "" : cliente.phone(),
                    cliente == null ? "" : cliente.cidade(),
                    cliente == null ? "" : cliente.estado(),
                    responsavelCliente(cliente),
                    selected.customerName(),
                    semTraco(selected.responsible()),
                    visita
            ));
            br.com.sigla.interfacegrafica.util.DialogoUi.informacao("Relatório de visita gerado em:\n" + arquivo);
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

    private OrdemServico ordemPorId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return casoDeUsoOrdemServico.listAll().stream()
                .filter(ordem -> ordem.id().equals(id))
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
            // Preserva a OS selecionada pelo id: setAll() limpa a seleção, mas a linha
            // permanece visível, dando a impressão de que continua selecionada. Sem isso,
            // ações como "Adicionar produto" reclamavam "Selecione uma OS." após um refresh.
            var anterior = ordensTable.getSelectionModel().getSelectedItem();
            String idSelecionado = anterior == null ? null : anterior.id();
            ordensTable.getItems().setAll(dados.ordens().stream()
                    .filter(order -> termo.isBlank()
                            || order.id().toLowerCase(Locale.ROOT).contains(termo)
                            || order.customerName().toLowerCase(Locale.ROOT).contains(termo)
                            || order.responsible().toLowerCase(Locale.ROOT).contains(termo)
                            || order.serviceType().toLowerCase(Locale.ROOT).contains(termo)
                            || order.title().toLowerCase(Locale.ROOT).contains(termo))
                    .filter(order -> "Todos".equals(filtroStatus) || order.status().equals(filtroStatus))
                    .toList());
            if (idSelecionado != null) {
                ordensTable.getItems().stream()
                        .filter(order -> idSelecionado.equals(order.id()))
                        .findFirst()
                        .ifPresent(order -> ordensTable.getSelectionModel().select(order));
            }
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
        configureColumn(pagoColumn, 7, ServicoConsultaOrdemServico.OrdemServicoView::financialStatus);
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
        br.com.sigla.interfacegrafica.util.DialogoUi.estilizar(dialog);
        dialog.setTitle("Produto da OS");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ComboBox<OpcaoId> produto = new ComboBox<>();
        UtilComboBox.preencher(produto, servicoConsultaReferencias.produtos(), true);
        produto.setMaxWidth(Double.MAX_VALUE);
        TextField quantidade = new TextField("1");
        TextField valor = new TextField();
        formatadorMoeda.aplicar(valor);
        GridPane grid = grid();
        grid.addRow(0, new Label("Produto"), produto);
        grid.addRow(1, new Label("Quantidade"), quantidade);
        grid.addRow(2, new Label("Valor unitário"), valor);
        dialog.getDialogPane().setContent(grid);
        // Valida ao clicar em OK e ANTES de o diálogo fechar: havendo erro, consome o evento
        // (mantém o diálogo aberto) e exibe a mensagem amigável. Assim nunca chega um produto
        // em branco ao backend nem se depende da propagação de exceção do resultConverter,
        // que algumas versões do JavaFX engolem silenciosamente.
        final Button botaoOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        botaoOk.addEventFilter(javafx.event.ActionEvent.ACTION, evento -> {
            try {
                validarProduto(produto, quantidade);
            } catch (IllegalArgumentException erro) {
                evento.consume();
                mostrar(erro.getMessage());
            }
        });
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            OpcaoId opcao = UtilComboBox.selecionado(produto);
            if (opcao == null) {
                return null; // proteção extra: o filtro já bloqueia, mas evita NPE
            }
            return new CasoDeUsoOrdemServico.AdicionarProdutoOrdemCommand(
                    ordemId,
                    UUID.randomUUID().toString(),
                    opcao.id(),
                    br.com.sigla.interfacegrafica.util.FormatadorQuantidade.parse(quantidade.getText()),
                    formatadorMoeda.valor(valor)
            );
        });
        return dialog.showAndWait();
    }

    /** Valida os campos obrigatórios do produto, lançando mensagem amigável agregada. */
    private void validarProduto(ComboBox<OpcaoId> produto, TextField quantidade) {
        ValidadorEntrada validador = ValidadorEntrada.nova();
        validador.selecao(UtilComboBox.selecionado(produto), "um produto");
        validador.quantidadePositiva(quantidade.getText(), "a quantidade");
        validador.validar();
    }

    private Optional<CasoDeUsoOrdemServico.AnexarOrdemServicoCommand> abrirDialogoAnexo(String ordemId) {
        Dialog<CasoDeUsoOrdemServico.AnexarOrdemServicoCommand> dialog = new Dialog<>();
        br.com.sigla.interfacegrafica.util.DialogoUi.estilizar(dialog);
        dialog.setTitle("Anexo da OS");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ComboBox<OrdemServico.TipoAnexo> tipo = new ComboBox<>();
        tipo.getItems().setAll(OrdemServico.TipoAnexo.values());
        tipo.getSelectionModel().select(OrdemServico.TipoAnexo.OUTRO);
        Label arquivoLabel = new Label("Nenhum arquivo selecionado");
        TextArea descricao = new TextArea();
        descricao.setPrefRowCount(3);

        String[] caminhoStore = {""};
        String[] nomeArquivo = {""};
        String[] mime = {"application/octet-stream"};
        long[] tamanho = {0L};

        Button escolher = new Button("Escolher arquivo...");
        escolher.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Selecionar arquivo para anexar");
            File arquivo = chooser.showOpenDialog(escolher.getScene() == null ? null : escolher.getScene().getWindow());
            if (arquivo == null) {
                return;
            }
            try {
                byte[] conteudo = java.nio.file.Files.readAllBytes(arquivo.toPath());
                String tipoDetectado = java.nio.file.Files.probeContentType(arquivo.toPath());
                // Copia o arquivo para o armazenamento da aplicacao (var/attachments) e guarda o caminho.
                caminhoStore[0] = portaArmazenamentoAnexo.store("os-" + ordemId, arquivo.getName(), conteudo);
                nomeArquivo[0] = arquivo.getName();
                mime[0] = tipoDetectado == null || tipoDetectado.isBlank() ? "application/octet-stream" : tipoDetectado;
                tamanho[0] = conteudo.length;
                arquivoLabel.setText(arquivo.getName() + "  (" + Math.max(1, conteudo.length / 1024) + " KB)");
            } catch (Exception excecao) {
                mostrar(br.com.sigla.interfacegrafica.util.MensagensErro.descrever("Falha ao anexar o arquivo:", excecao));
            }
        });

        GridPane grid = grid();
        grid.addRow(0, new Label("Tipo"), tipo);
        grid.addRow(1, new Label("Arquivo"), escolher);
        grid.addRow(2, new Label(""), arquivoLabel);
        grid.addRow(3, new Label("Descrição"), descricao);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK || caminhoStore[0].isBlank()) {
                return null;
            }
            return new CasoDeUsoOrdemServico.AnexarOrdemServicoCommand(
                    ordemId,
                    UUID.randomUUID().toString(),
                    tipo.getValue(),
                    nomeArquivo[0],
                    caminhoStore[0],
                    mime[0],
                    tamanho[0],
                    descricao.getText(),
                    ""
            );
        });
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
        br.com.sigla.interfacegrafica.util.DialogoUi.informacao(message);
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

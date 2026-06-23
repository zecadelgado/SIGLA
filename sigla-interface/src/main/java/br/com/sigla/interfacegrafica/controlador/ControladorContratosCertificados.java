package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.certificados.porta.entrada.CasoDeUsoCertificado;
import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.dominio.certificados.Certificado;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorData;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorMoeda;
import br.com.sigla.interfacegrafica.async.ExecutorTarefasUi;
import br.com.sigla.interfacegrafica.formatador.FormatadorMascaraMoeda;
import br.com.sigla.interfacegrafica.util.TradutorInterface;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
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
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ControladorContratosCertificados {

    private final CasoDeUsoContrato casoDeUsoContrato;
    private final CasoDeUsoCertificado casoDeUsoCertificado;
    private final CasoDeUsoCliente casoDeUsoCliente;
    private final ApresentadorData apresentadorData;
    private final ApresentadorMoeda apresentadorMoeda;
    private final FormatadorMascaraMoeda formatadorMoeda;
    private final ExecutorTarefasUi executorTarefasUi;

    private int geracaoRefresh;

    @FXML
    private Label totalAtivosLabel;
    @FXML
    private Label proximosLabel;
    @FXML
    private Label vencidosLabel;
    @FXML
    private ComboBox<String> filtroTipoCombo;
    @FXML
    private ComboBox<String> filtroSituacaoCombo;
    @FXML
    private TableView<ItemVencimentoRow> itensTable;
    @FXML
    private TableColumn<ItemVencimentoRow, String> tipoColumn;
    @FXML
    private TableColumn<ItemVencimentoRow, String> clienteColumn;
    @FXML
    private TableColumn<ItemVencimentoRow, String> descricaoColumn;
    @FXML
    private TableColumn<ItemVencimentoRow, String> inicioColumn;
    @FXML
    private TableColumn<ItemVencimentoRow, String> vencimentoColumn;
    @FXML
    private TableColumn<ItemVencimentoRow, String> statusColumn;
    @FXML
    private TableColumn<ItemVencimentoRow, String> alertaColumn;
    @FXML
    private TableColumn<ItemVencimentoRow, String> detalheColumn;
    @FXML
    private TableColumn<ItemVencimentoRow, String> situacaoColumn;

    public ControladorContratosCertificados(
            CasoDeUsoContrato casoDeUsoContrato,
            CasoDeUsoCertificado casoDeUsoCertificado,
            CasoDeUsoCliente casoDeUsoCliente,
            ApresentadorData apresentadorData,
            ApresentadorMoeda apresentadorMoeda,
            FormatadorMascaraMoeda formatadorMoeda,
            ExecutorTarefasUi executorTarefasUi
    ) {
        this.casoDeUsoContrato = casoDeUsoContrato;
        this.casoDeUsoCertificado = casoDeUsoCertificado;
        this.casoDeUsoCliente = casoDeUsoCliente;
        this.apresentadorData = apresentadorData;
        this.apresentadorMoeda = apresentadorMoeda;
        this.formatadorMoeda = formatadorMoeda;
        this.executorTarefasUi = executorTarefasUi;
    }

    @FXML
    public void initialize() {
        configurarFiltros();
        configurarTabela();
        refresh();
    }

    @FXML
    private void onNovoContrato() {
        abrirDialogoContrato(null);
    }

    @FXML
    private void onNovoCertificado() {
        abrirDialogoCertificado(null);
    }

    @FXML
    private void onAtualizar() {
        refresh();
    }

    @FXML
    private void onEditarItem() {
        ItemVencimentoRow row = selecionado();
        if (row == null) {
            return;
        }
        if ("Contrato".equals(row.tipo())) {
            contratoPorId(row.id()).ifPresent(this::abrirDialogoContrato);
        } else {
            certificadoPorId(row.id()).ifPresent(this::abrirDialogoCertificado);
        }
    }

    @FXML
    private void onEncerrarItem() {
        ItemVencimentoRow row = selecionado();
        if (row == null) {
            return;
        }
        if (!"Contrato".equals(row.tipo())) {
            alerta("Encerrar disponível apenas para contratos. Para certificados, use Renovar.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Encerrar contrato");
        dialog.setHeaderText(null);
        dialog.setContentText("Motivo do encerramento:");
        dialog.showAndWait().ifPresent(motivo -> {
            if (motivo.isBlank()) {
                alerta("Informe o motivo do encerramento.");
                return;
            }
            executar(() -> {
                casoDeUsoContrato.encerrar(new CasoDeUsoContrato.EncerrarContratoCommand(row.id(), motivo));
                refresh();
            });
        });
    }

    @FXML
    private void onRenovarItem() {
        ItemVencimentoRow row = selecionado();
        if (row == null) {
            return;
        }
        if ("Contrato".equals(row.tipo())) {
            Dialog<LocalDate> dialog = new Dialog<>();
            br.com.sigla.interfacegrafica.util.DialogoUi.estilizar(dialog);
            dialog.setTitle("Renovar contrato");
            dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
            DatePicker picker = new DatePicker();
            contratoPorId(row.id()).ifPresent(contrato -> picker.setValue(contrato.endDate().plusMonths(contrato.periodoMeses())));
            dialog.getDialogPane().setContent(grid("Nova data fim", picker));
            dialog.setResultConverter(button -> button == ButtonType.OK ? picker.getValue() : null);
            dialog.showAndWait().ifPresent(novaData -> executar(() -> {
                casoDeUsoContrato.renovar(new CasoDeUsoContrato.RenovarContratoCommand(row.id(), novaData));
                refresh();
            }));
        } else {
            executar(() -> {
                casoDeUsoCertificado.renovar(new CasoDeUsoCertificado.RenovarCertificadoCommand(row.id(), LocalDate.now(), 0));
                refresh();
            });
        }
    }

    private ItemVencimentoRow selecionado() {
        ItemVencimentoRow row = itensTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            alerta("Selecione um contrato ou certificado na tabela.");
        }
        return row;
    }

    private java.util.Optional<Contrato> contratoPorId(String id) {
        return casoDeUsoContrato.listAll().stream().filter(contrato -> contrato.id().equals(id)).findFirst();
    }

    private java.util.Optional<Certificado> certificadoPorId(String id) {
        return casoDeUsoCertificado.listAll().stream().filter(certificado -> certificado.id().equals(id)).findFirst();
    }

    private void executar(Runnable acao) {
        try {
            acao.run();
        } catch (RuntimeException exception) {
            alerta(br.com.sigla.interfacegrafica.util.MensagensErro.descrever(exception));
        }
    }

    private void alerta(String mensagem) {
        br.com.sigla.interfacegrafica.util.DialogoUi.informacao(mensagem);
    }

    private void configurarFiltros() {
        filtroTipoCombo.getItems().setAll("Todos", "Contratos", "Certificados");
        filtroTipoCombo.getSelectionModel().select("Todos");
        filtroSituacaoCombo.getItems().setAll("Todos", "Ativos", "Próximos do vencimento", "Vencidos");
        filtroSituacaoCombo.getSelectionModel().select("Todos");
        filtroTipoCombo.setOnAction(event -> refresh());
        filtroSituacaoCombo.setOnAction(event -> refresh());
    }

    private void configurarTabela() {
        tipoColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().tipo()));
        clienteColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().cliente()));
        descricaoColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().descricao()));
        inicioColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().inicio()));
        vencimentoColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().vencimento()));
        statusColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().status()));
        alertaColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().alerta()));
        detalheColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().detalhe()));
        situacaoColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().situacao()));
    }

    private void refresh() {
        int geracao = ++geracaoRefresh;
        executorTarefasUi.executar(
                () -> new VencimentoSnapshot(casoDeUsoCliente.listAll(), casoDeUsoContrato.listAll(), casoDeUsoCertificado.listAll()),
                dados -> {
                    if (geracao == geracaoRefresh) {
                        aplicar(dados);
                    }
                });
    }

    private record VencimentoSnapshot(List<Cliente> clientes, List<Contrato> contratos, List<Certificado> certificados) {
    }

    private void aplicar(VencimentoSnapshot dados) {
        LocalDate hoje = LocalDate.now();
        Map<String, Cliente> clientes = dados.clientes().stream()
                .collect(Collectors.toMap(Cliente::id, Function.identity(), (left, right) -> left));
        List<ItemVencimentoRow> rows = new ArrayList<>();

        for (Contrato contrato : dados.contratos()) {
            String cliente = nomeCliente(clientes, contrato.customerId());
            String situacao = situacao(contrato.status() == Contrato.ContratoStatus.CANCELLED, contrato.endDate(), contrato.alertDaysBeforeEnd(), hoje);
            rows.add(new ItemVencimentoRow(
                    contrato.id(),
                    "Contrato",
                    cliente,
                    contrato.description().isBlank() ? TradutorInterface.texto(contrato.type()) : contrato.description(),
                    apresentadorData.format(contrato.startDate()),
                    apresentadorData.format(contrato.endDate()),
                    TradutorInterface.texto(contrato.status()),
                    contrato.alertDaysBeforeEnd() + " dias",
                    apresentadorMoeda.format(contrato.monthlyValue()),
                    situacao
            ));
        }

        for (Certificado certificado : dados.certificados()) {
            String cliente = nomeCliente(clientes, certificado.customerId());
            String situacao = situacao(certificado.status() == Certificado.CertificadoStatus.REPLACED, certificado.validUntil(), certificado.renewalAlertDays(), hoje);
            rows.add(new ItemVencimentoRow(
                    certificado.id(),
                    "Certificado",
                    cliente,
                    certificado.description().isBlank() ? "Certificado de higiene" : certificado.description(),
                    apresentadorData.format(certificado.issuedOn()),
                    apresentadorData.format(certificado.validUntil()),
                    TradutorInterface.texto(certificado.status()),
                    certificado.renewalAlertDays() + " dias",
                    certificado.intervalMonths() + " meses",
                    situacao
            ));
        }

        rows = aplicarFiltros(rows).stream()
                .sorted(Comparator.comparing(ItemVencimentoRow::vencimento).thenComparing(ItemVencimentoRow::cliente))
                .toList();
        itensTable.getItems().setAll(rows);

        totalAtivosLabel.setText(String.valueOf(rows.stream().filter(row -> row.situacao().equals("Ativo")).count()));
        proximosLabel.setText(String.valueOf(rows.stream().filter(row -> row.situacao().equals("Proximo")).count()));
        vencidosLabel.setText(String.valueOf(rows.stream().filter(row -> row.situacao().equals("Vencido")).count()));
    }

    private List<ItemVencimentoRow> aplicarFiltros(List<ItemVencimentoRow> rows) {
        String tipo = filtroTipoCombo.getValue();
        String situacao = filtroSituacaoCombo.getValue();
        return rows.stream()
                .filter(row -> "Todos".equals(tipo)
                        || ("Contratos".equals(tipo) && row.tipo().equals("Contrato"))
                        || ("Certificados".equals(tipo) && row.tipo().equals("Certificado")))
                .filter(row -> "Todos".equals(situacao)
                        || ("Ativos".equals(situacao) && row.situacao().equals("Ativo"))
                        || ("Próximos do vencimento".equals(situacao) && row.situacao().equals("Proximo"))
                        || ("Vencidos".equals(situacao) && row.situacao().equals("Vencido")))
                .toList();
    }

    private String situacao(boolean encerrado, LocalDate vencimento, int diasAlerta, LocalDate hoje) {
        if (vencimento != null && vencimento.isBefore(hoje)) {
            return "Vencido";
        }
        if (!encerrado && vencimento != null && !hoje.isBefore(vencimento.minusDays(diasAlerta))) {
            return "Proximo";
        }
        return encerrado ? "Encerrado" : "Ativo";
    }

    private String nomeCliente(Map<String, Cliente> clientes, String clienteId) {
        Cliente cliente = clientes.get(clienteId);
        return cliente == null ? clienteId : cliente.name();
    }

    private void abrirDialogoContrato(Contrato existente) {
        Dialog<Boolean> dialog = new Dialog<>();
        br.com.sigla.interfacegrafica.util.DialogoUi.estilizar(dialog);
        dialog.setTitle(existente == null ? "Novo contrato" : "Editar contrato");
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<ClienteOption> clienteCombo = clientesCombo();
        TextField descricaoField = new TextField();
        DatePicker inicioPicker = new DatePicker(LocalDate.now());
        DatePicker fimPicker = new DatePicker(LocalDate.now().plusMonths(12));
        ComboBox<Contrato.ContratoType> tipoCombo = new ComboBox<>();
        TradutorInterface.aplicar(tipoCombo);
        tipoCombo.getItems().setAll(
            java.util.Arrays.stream(Contrato.ContratoType.values())
                .filter(t -> t != Contrato.ContratoType.CORPORATE)
                .toList()
        );
        tipoCombo.getSelectionModel().select(Contrato.ContratoType.MONTHLY);
        ComboBox<Contrato.ServiceFrequency> frequenciaCombo = new ComboBox<>();
        TradutorInterface.aplicar(frequenciaCombo);
        frequenciaCombo.getItems().setAll(Contrato.ServiceFrequency.values());
        frequenciaCombo.getSelectionModel().select(Contrato.ServiceFrequency.MONTHLY);
        TextField valorMensalField = new TextField();
        formatadorMoeda.aplicar(valorMensalField);
        formatadorMoeda.definir(valorMensalField, BigDecimal.ZERO);
        TextField diasAlertaField = new TextField("15");
        CheckBox alertaAtivoCheck = new CheckBox("Alerta ativo");
        alertaAtivoCheck.setSelected(true);
        TextArea observacoesArea = new TextArea();
        observacoesArea.setPrefRowCount(3);

        if (existente != null) {
            selecionarCliente(clienteCombo, existente.customerId());
            descricaoField.setText(existente.description());
            inicioPicker.setValue(existente.startDate());
            fimPicker.setValue(existente.endDate());
            tipoCombo.getSelectionModel().select(existente.type());
            frequenciaCombo.getSelectionModel().select(existente.serviceFrequency());
            formatadorMoeda.definir(valorMensalField, existente.monthlyValue());
            diasAlertaField.setText(String.valueOf(existente.alertDaysBeforeEnd()));
            alertaAtivoCheck.setSelected(existente.alertActive());
            observacoesArea.setText(existente.notes());
        }

        dialog.getDialogPane().setContent(grid(
                "Cliente", clienteCombo,
                "Descrição", descricaoField,
                "Início", inicioPicker,
                "Fim", fimPicker,
                "Tipo", tipoCombo,
                "Frequência", frequenciaCombo,
                "Valor mensal", valorMensalField,
                "Dias de alerta", diasAlertaField,
                "Alerta", alertaAtivoCheck,
                "Observações", observacoesArea
        ));
        dialog.setResultConverter(button -> button == ButtonType.OK);
        dialog.showAndWait().ifPresent(confirmado -> {
            if (!Boolean.TRUE.equals(confirmado)) {
                return;
            }
            ClienteOption cliente = clienteCombo.getValue();
            executar(() -> {
                if (existente == null) {
                    casoDeUsoContrato.create(new CasoDeUsoContrato.CreateContratoCommand(
                            UUID.randomUUID().toString(),
                            cliente == null ? "" : cliente.id(),
                            descricaoField.getText(),
                            inicioPicker.getValue(),
                            fimPicker.getValue(),
                            tipoCombo.getValue(),
                            frequenciaCombo.getValue(),
                            Contrato.ContratoStatus.ACTIVE,
                            Contrato.RenewalRule.MANUAL,
                            formatadorMoeda.valor(valorMensalField),
                            alertaAtivoCheck.isSelected(),
                            parseInt(diasAlertaField.getText(), 15),
                            observacoesArea.getText()
                    ));
                } else {
                    casoDeUsoContrato.update(new CasoDeUsoContrato.UpdateContratoCommand(
                            existente.id(),
                            cliente == null ? existente.customerId() : cliente.id(),
                            descricaoField.getText(),
                            inicioPicker.getValue(),
                            fimPicker.getValue(),
                            tipoCombo.getValue(),
                            frequenciaCombo.getValue(),
                            Contrato.RenewalRule.MANUAL,
                            formatadorMoeda.valor(valorMensalField),
                            alertaAtivoCheck.isSelected(),
                            parseInt(diasAlertaField.getText(), 15),
                            observacoesArea.getText()
                    ));
                }
                refresh();
            });
        });
    }

    private void selecionarCliente(ComboBox<ClienteOption> combo, String clienteId) {
        combo.getItems().stream()
                .filter(option -> option.id().equals(clienteId))
                .findFirst()
                .ifPresent(option -> combo.getSelectionModel().select(option));
    }

    private void abrirDialogoCertificado(Certificado existente) {
        Dialog<Boolean> dialog = new Dialog<>();
        br.com.sigla.interfacegrafica.util.DialogoUi.estilizar(dialog);
        dialog.setTitle(existente == null ? "Novo certificado" : "Editar certificado");
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<ClienteOption> clienteCombo = clientesCombo();
        TextField descricaoField = new TextField("Certificado de higiene");
        DatePicker emissaoPicker = new DatePicker(LocalDate.now());
        DatePicker validadePicker = new DatePicker();
        TextField intervaloField = new TextField("6");
        TextField diasAlertaField = new TextField("15");
        CheckBox alertaAtivoCheck = new CheckBox("Alerta ativo");
        alertaAtivoCheck.setSelected(true);
        TextArea observacoesArea = new TextArea();
        observacoesArea.setPrefRowCount(3);

        if (existente != null) {
            selecionarCliente(clienteCombo, existente.customerId());
            descricaoField.setText(existente.description());
            emissaoPicker.setValue(existente.issuedOn());
            validadePicker.setValue(existente.validUntil());
            intervaloField.setText(String.valueOf(existente.intervalMonths()));
            diasAlertaField.setText(String.valueOf(existente.renewalAlertDays()));
            alertaAtivoCheck.setSelected(existente.alertActive());
            observacoesArea.setText(existente.notes());
        }

        dialog.getDialogPane().setContent(grid(
                "Cliente", clienteCombo,
                "Descrição", descricaoField,
                "Emissão", emissaoPicker,
                "Validade", validadePicker,
                "Intervalo em meses", intervaloField,
                "Dias de alerta", diasAlertaField,
                "Alerta", alertaAtivoCheck,
                "Observações", observacoesArea
        ));
        dialog.setResultConverter(button -> button == ButtonType.OK);
        dialog.showAndWait().ifPresent(confirmado -> {
            if (!Boolean.TRUE.equals(confirmado)) {
                return;
            }
            ClienteOption cliente = clienteCombo.getValue();
            executar(() -> {
                if (existente == null) {
                    casoDeUsoCertificado.issue(new CasoDeUsoCertificado.IssueCertificadoCommand(
                            UUID.randomUUID().toString(),
                            cliente == null ? "" : cliente.id(),
                            "",
                            "",
                            descricaoField.getText(),
                            emissaoPicker.getValue(),
                            validadePicker.getValue(),
                            parseInt(intervaloField.getText(), 6),
                            alertaAtivoCheck.isSelected(),
                            Certificado.CertificadoStatus.ACTIVE,
                            parseInt(diasAlertaField.getText(), 15),
                            observacoesArea.getText()
                    ));
                } else {
                    casoDeUsoCertificado.update(new CasoDeUsoCertificado.UpdateCertificadoCommand(
                            existente.id(),
                            cliente == null ? existente.customerId() : cliente.id(),
                            descricaoField.getText(),
                            emissaoPicker.getValue(),
                            validadePicker.getValue(),
                            parseInt(intervaloField.getText(), 6),
                            alertaAtivoCheck.isSelected(),
                            parseInt(diasAlertaField.getText(), 15),
                            observacoesArea.getText()
                    ));
                }
                refresh();
            });
        });
    }

    private ComboBox<ClienteOption> clientesCombo() {
        ComboBox<ClienteOption> combo = new ComboBox<>();
        combo.getItems().setAll(casoDeUsoCliente.listAll().stream()
                .filter(Cliente::ativo)
                .map(cliente -> new ClienteOption(cliente.id(), cliente.name()))
                .sorted(Comparator.comparing(ClienteOption::name))
                .toList());
        if (!combo.getItems().isEmpty()) {
            combo.getSelectionModel().selectFirst();
        }
        return combo;
    }

    private GridPane grid(Object... labelAndControlPairs) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        for (int index = 0; index < labelAndControlPairs.length; index += 2) {
            grid.add(new Label(String.valueOf(labelAndControlPairs[index])), 0, index / 2);
            grid.add((javafx.scene.Node) labelAndControlPairs[index + 1], 1, index / 2);
        }
        return grid;
    }

    private BigDecimal parseMoney(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.replace(".", "").replace(",", "."));
    }

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }

    private record ClienteOption(String id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    public record ItemVencimentoRow(
            String id,
            String tipo,
            String cliente,
            String descricao,
            String inicio,
            String vencimento,
            String status,
            String alerta,
            String detalhe,
            String situacao
    ) {
    }
}

package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.certificados.porta.entrada.CasoDeUsoCertificado;
import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.dominio.certificados.Certificado;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorData;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorMoeda;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
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
            ApresentadorMoeda apresentadorMoeda
    ) {
        this.casoDeUsoContrato = casoDeUsoContrato;
        this.casoDeUsoCertificado = casoDeUsoCertificado;
        this.casoDeUsoCliente = casoDeUsoCliente;
        this.apresentadorData = apresentadorData;
        this.apresentadorMoeda = apresentadorMoeda;
    }

    @FXML
    public void initialize() {
        configurarFiltros();
        configurarTabela();
        refresh();
    }

    @FXML
    private void onNovoContrato() {
        abrirDialogoContrato();
    }

    @FXML
    private void onNovoCertificado() {
        abrirDialogoCertificado();
    }

    @FXML
    private void onAtualizar() {
        refresh();
    }

    private void configurarFiltros() {
        filtroTipoCombo.getItems().setAll("Todos", "Contratos", "Certificados");
        filtroTipoCombo.getSelectionModel().select("Todos");
        filtroSituacaoCombo.getItems().setAll("Todos", "Ativos", "Proximos do vencimento", "Vencidos");
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
        LocalDate hoje = LocalDate.now();
        Map<String, Cliente> clientes = casoDeUsoCliente.listAll().stream()
                .collect(Collectors.toMap(Cliente::id, Function.identity(), (left, right) -> left));
        List<ItemVencimentoRow> rows = new ArrayList<>();

        for (Contrato contrato : casoDeUsoContrato.listAll()) {
            String cliente = nomeCliente(clientes, contrato.customerId());
            String situacao = situacao(contrato.status() == Contrato.ContratoStatus.CANCELLED, contrato.endDate(), contrato.alertDaysBeforeEnd(), hoje);
            rows.add(new ItemVencimentoRow(
                    "Contrato",
                    cliente,
                    contrato.description().isBlank() ? contrato.type().name() : contrato.description(),
                    apresentadorData.format(contrato.startDate()),
                    apresentadorData.format(contrato.endDate()),
                    contrato.status().name(),
                    contrato.alertDaysBeforeEnd() + " dias",
                    apresentadorMoeda.format(contrato.monthlyValue()),
                    situacao
            ));
        }

        for (Certificado certificado : casoDeUsoCertificado.listAll()) {
            String cliente = nomeCliente(clientes, certificado.customerId());
            String situacao = situacao(certificado.status() == Certificado.CertificadoStatus.REPLACED, certificado.validUntil(), certificado.renewalAlertDays(), hoje);
            rows.add(new ItemVencimentoRow(
                    "Certificado",
                    cliente,
                    certificado.description().isBlank() ? "Certificado de higiene" : certificado.description(),
                    apresentadorData.format(certificado.issuedOn()),
                    apresentadorData.format(certificado.validUntil()),
                    certificado.status().name(),
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
                        || ("Proximos do vencimento".equals(situacao) && row.situacao().equals("Proximo"))
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

    private void abrirDialogoContrato() {
        Dialog<CasoDeUsoContrato.CreateContratoCommand> dialog = new Dialog<>();
        dialog.setTitle("Novo contrato");
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<ClienteOption> clienteCombo = clientesCombo();
        TextField descricaoField = new TextField();
        DatePicker inicioPicker = new DatePicker(LocalDate.now());
        DatePicker fimPicker = new DatePicker(LocalDate.now().plusMonths(12));
        ComboBox<Contrato.ContratoType> tipoCombo = new ComboBox<>();
        tipoCombo.getItems().setAll(Contrato.ContratoType.values());
        tipoCombo.getSelectionModel().select(Contrato.ContratoType.MONTHLY);
        ComboBox<Contrato.ServiceFrequency> frequenciaCombo = new ComboBox<>();
        frequenciaCombo.getItems().setAll(Contrato.ServiceFrequency.values());
        frequenciaCombo.getSelectionModel().select(Contrato.ServiceFrequency.MONTHLY);
        TextField valorMensalField = new TextField("0");
        TextField diasAlertaField = new TextField("15");
        CheckBox alertaAtivoCheck = new CheckBox("Alerta ativo");
        alertaAtivoCheck.setSelected(true);
        TextArea observacoesArea = new TextArea();
        observacoesArea.setPrefRowCount(3);

        dialog.getDialogPane().setContent(grid(
                "Cliente", clienteCombo,
                "Descricao", descricaoField,
                "Inicio", inicioPicker,
                "Fim", fimPicker,
                "Tipo", tipoCombo,
                "Frequencia", frequenciaCombo,
                "Valor mensal", valorMensalField,
                "Dias de alerta", diasAlertaField,
                "Alerta", alertaAtivoCheck,
                "Observacoes", observacoesArea
        ));
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            ClienteOption cliente = clienteCombo.getValue();
            return new CasoDeUsoContrato.CreateContratoCommand(
                    UUID.randomUUID().toString(),
                    cliente == null ? "" : cliente.id(),
                    descricaoField.getText(),
                    inicioPicker.getValue(),
                    fimPicker.getValue(),
                    tipoCombo.getValue(),
                    frequenciaCombo.getValue(),
                    Contrato.ContratoStatus.ACTIVE,
                    Contrato.RenewalRule.MANUAL,
                    parseMoney(valorMensalField.getText()),
                    alertaAtivoCheck.isSelected(),
                    parseInt(diasAlertaField.getText(), 15),
                    observacoesArea.getText()
            );
        });
        dialog.showAndWait().ifPresent(command -> {
            casoDeUsoContrato.create(command);
            refresh();
        });
    }

    private void abrirDialogoCertificado() {
        Dialog<CasoDeUsoCertificado.IssueCertificadoCommand> dialog = new Dialog<>();
        dialog.setTitle("Novo certificado");
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

        dialog.getDialogPane().setContent(grid(
                "Cliente", clienteCombo,
                "Descricao", descricaoField,
                "Emissao", emissaoPicker,
                "Validade", validadePicker,
                "Intervalo em meses", intervaloField,
                "Dias de alerta", diasAlertaField,
                "Alerta", alertaAtivoCheck,
                "Observacoes", observacoesArea
        ));
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            ClienteOption cliente = clienteCombo.getValue();
            return new CasoDeUsoCertificado.IssueCertificadoCommand(
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
            );
        });
        dialog.showAndWait().ifPresent(command -> {
            casoDeUsoCertificado.issue(command);
            refresh();
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

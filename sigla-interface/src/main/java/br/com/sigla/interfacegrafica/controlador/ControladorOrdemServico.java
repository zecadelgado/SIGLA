package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.interfacegrafica.apresentacao.ApresentadorData;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorMoeda;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaOrdemServico;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ControladorOrdemServico extends ControladorComMenuPrincipal {

    private final ServicoConsultaOrdemServico servicoConsultaOrdemServico;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final ApresentadorMoeda apresentadorMoeda;
    private final ApresentadorData apresentadorData;

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
    private TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> responsavelColumn;
    @FXML
    private TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> emissaoColumn;
    @FXML
    private TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> valorColumn;
    @FXML
    private TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> statusColumn;

    public ControladorOrdemServico(
            ServicoConsultaOrdemServico servicoConsultaOrdemServico,
            GerenciadorNavegacao gerenciadorNavegacao,
            ApresentadorMoeda apresentadorMoeda,
            ApresentadorData apresentadorData
    ) {
        super(gerenciadorNavegacao);
        this.servicoConsultaOrdemServico = servicoConsultaOrdemServico;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
        this.apresentadorMoeda = apresentadorMoeda;
        this.apresentadorData = apresentadorData;
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
        refresh();
    }

    @FXML
    private void onNovaOrdem() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.NEW_SERVICE_ORDER);
    }

    private void refresh() {
        var resumo = servicoConsultaOrdemServico.summary();
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
            ordensTable.getItems().setAll(servicoConsultaOrdemServico.listAll().stream()
                    .filter(order -> termo.isBlank()
                            || order.id().toLowerCase(Locale.ROOT).contains(termo)
                            || order.customerName().toLowerCase(Locale.ROOT).contains(termo)
                            || order.responsible().toLowerCase(Locale.ROOT).contains(termo)
                            || order.title().toLowerCase(Locale.ROOT).contains(termo))
                    .filter(order -> "Todos".equals(filtroStatus) || order.status().equals(filtroStatus))
                    .toList());
        }
    }

    private void configureTable() {
        configureColumn(numeroColumn, 0, row -> row.numero());
        configureColumn(clienteColumn, 1, row -> row.customerName());
        configureColumn(responsavelColumn, 2, row -> row.responsible());
        configureColumn(emissaoColumn, 3, row -> apresentadorData.format(row.emissionDate()));
        configureColumn(valorColumn, 4, row -> apresentadorMoeda.format(row.amount()));
        configureColumn(statusColumn, 5, row -> row.status());
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

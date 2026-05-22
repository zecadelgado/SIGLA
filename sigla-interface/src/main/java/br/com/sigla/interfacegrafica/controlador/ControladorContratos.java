package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorMoeda;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaReferencias;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Component;

@Component
public class ControladorContratos extends ControladorComMenuPrincipal {

    private final CasoDeUsoContrato contractUseCase;
    private final ServicoConsultaReferencias servicoConsultaReferencias;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final ApresentadorMoeda apresentadorMoeda;

    @FXML
    private TableView<ContratoRow> contratosTable;
    @FXML
    private TableColumn<ContratoRow, String> clienteColumn;
    @FXML
    private TableColumn<ContratoRow, String> descricaoColumn;
    @FXML
    private TableColumn<ContratoRow, String> tipoColumn;
    @FXML
    private TableColumn<ContratoRow, String> inicioColumn;
    @FXML
    private TableColumn<ContratoRow, String> fimColumn;
    @FXML
    private TableColumn<ContratoRow, String> valorColumn;
    @FXML
    private TableColumn<ContratoRow, String> statusColumn;

    public ControladorContratos(
            CasoDeUsoContrato contractUseCase,
            ServicoConsultaReferencias servicoConsultaReferencias,
            GerenciadorNavegacao gerenciadorNavegacao,
            ApresentadorMoeda apresentadorMoeda
    ) {
        super(gerenciadorNavegacao);
        this.contractUseCase = contractUseCase;
        this.servicoConsultaReferencias = servicoConsultaReferencias;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
        this.apresentadorMoeda = apresentadorMoeda;
    }

    @FXML
    public void initialize() {
        configureTable();
        refresh();
    }

    @FXML
    private void onNovoContrato() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.NEW_CONTRACT);
    }

    private void refresh() {
        if (contratosTable == null) {
            return;
        }
        contratosTable.getItems().setAll(contractUseCase.listAll().stream()
                .map(contract -> new ContratoRow(
                        clienteNome(contract.customerId()),
                        contract.descricao(),
                        contract.type().name() + "/" + contract.serviceFrequency().name(),
                        contract.startDate().toString(),
                        contract.endDate().toString(),
                        apresentadorMoeda.format(contract.valorMensal()),
                        contract.status().name()
                ))
                .toList());
    }

    private void configureTable() {
        configureColumn(clienteColumn, 0, ContratoRow::cliente);
        configureColumn(descricaoColumn, 1, ContratoRow::descricao);
        configureColumn(tipoColumn, 2, ContratoRow::tipo);
        configureColumn(inicioColumn, 3, ContratoRow::inicio);
        configureColumn(fimColumn, 4, ContratoRow::fim);
        configureColumn(valorColumn, 5, ContratoRow::valor);
        configureColumn(statusColumn, 6, ContratoRow::status);
    }

    private void configureColumn(TableColumn<ContratoRow, String> column, int fallbackIndex, java.util.function.Function<ContratoRow, String> getter) {
        TableColumn<ContratoRow, String> target = column != null ? column : getColumn(fallbackIndex);
        if (target != null) {
            target.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        }
    }

    @SuppressWarnings("unchecked")
    private TableColumn<ContratoRow, String> getColumn(int index) {
        if (contratosTable == null || contratosTable.getColumns().size() <= index) {
            return null;
        }
        return (TableColumn<ContratoRow, String>) contratosTable.getColumns().get(index);
    }

    private String clienteNome(String clienteId) {
        return servicoConsultaReferencias.clientes().stream()
                .filter(option -> option.id().equals(clienteId))
                .map(option -> option.label())
                .findFirst()
                .orElse(clienteId == null || clienteId.isBlank() ? "-" : clienteId);
    }

    private record ContratoRow(String cliente, String descricao, String tipo, String inicio, String fim, String valor, String status) {
    }
}


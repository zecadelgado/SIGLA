package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.agenda.porta.entrada.CasoDeUsoAgenda;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorData;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaReferencias;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Component;

@Component
public class ControladorAgenda extends ControladorComMenuPrincipal {

    private final CasoDeUsoAgenda agendaUseCase;
    private final ServicoConsultaReferencias servicoConsultaReferencias;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final ApresentadorData apresentadorData;

    @FXML
    private TableView<AgendaRow> agendaTable;
    @FXML
    private TableColumn<AgendaRow, String> clienteColumn;
    @FXML
    private TableColumn<AgendaRow, String> tituloColumn;
    @FXML
    private TableColumn<AgendaRow, String> tipoColumn;
    @FXML
    private TableColumn<AgendaRow, String> dataColumn;
    @FXML
    private TableColumn<AgendaRow, String> statusColumn;
    @FXML
    private TableColumn<AgendaRow, String> prioridadeColumn;

    public ControladorAgenda(
            CasoDeUsoAgenda agendaUseCase,
            ServicoConsultaReferencias servicoConsultaReferencias,
            GerenciadorNavegacao gerenciadorNavegacao,
            ApresentadorData apresentadorData
    ) {
        super(gerenciadorNavegacao);
        this.agendaUseCase = agendaUseCase;
        this.servicoConsultaReferencias = servicoConsultaReferencias;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
        this.apresentadorData = apresentadorData;
    }

    @FXML
    public void initialize() {
        configureTable();
        refresh();
    }

    @FXML
    private void onNovoEvento() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.NEW_EVENT);
    }

    private void refresh() {
        if (agendaTable == null) {
            return;
        }
        agendaTable.getItems().setAll(agendaUseCase.listAll().stream()
                .map(schedule -> new AgendaRow(
                        clienteNome(schedule.customerId()),
                        blankAsDash(schedule.title()),
                        schedule.type().name() + "/" + schedule.recurrence().name(),
                        apresentadorData.format(schedule.scheduledDate()),
                        schedule.status().name(),
                        schedule.priority().name()
                ))
                .toList());
    }

    private void configureTable() {
        configureColumn(clienteColumn, 0, AgendaRow::cliente);
        configureColumn(tituloColumn, 1, AgendaRow::titulo);
        configureColumn(tipoColumn, 2, AgendaRow::tipo);
        configureColumn(dataColumn, 3, AgendaRow::data);
        configureColumn(statusColumn, 4, AgendaRow::status);
        configureColumn(prioridadeColumn, 5, AgendaRow::prioridade);
    }

    private void configureColumn(TableColumn<AgendaRow, String> column, int fallbackIndex, java.util.function.Function<AgendaRow, String> getter) {
        TableColumn<AgendaRow, String> target = column != null ? column : getColumn(fallbackIndex);
        if (target != null) {
            target.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        }
    }

    @SuppressWarnings("unchecked")
    private TableColumn<AgendaRow, String> getColumn(int index) {
        if (agendaTable == null || agendaTable.getColumns().size() <= index) {
            return null;
        }
        return (TableColumn<AgendaRow, String>) agendaTable.getColumns().get(index);
    }

    private String clienteNome(String clienteId) {
        return servicoConsultaReferencias.clientes().stream()
                .filter(option -> option.id().equals(clienteId))
                .map(option -> option.label())
                .findFirst()
                .orElse(clienteId == null || clienteId.isBlank() ? "-" : clienteId);
    }

    private String blankAsDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private record AgendaRow(String cliente, String titulo, String tipo, String data, String status, String prioridade) {
    }
}


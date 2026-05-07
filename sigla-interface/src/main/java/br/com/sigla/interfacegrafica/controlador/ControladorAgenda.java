package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.agenda.porta.entrada.CasoDeUsoAgenda;
import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.usuarios.porta.entrada.CasoDeUsoUsuario;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.dominio.usuarios.Usuario;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorData;
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
import javafx.scene.layout.GridPane;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class ControladorAgenda {

    private final CasoDeUsoAgenda agendaUseCase;
    private final CasoDeUsoCliente clienteUseCase;
    private final CasoDeUsoUsuario usuarioUseCase;
    private final ApresentadorData apresentadorData;

    @FXML
    private Label periodoLabel;
    @FXML
    private ComboBox<String> periodoCombo;
    @FXML
    private DatePicker dataReferenciaPicker;
    @FXML
    private TableView<AgendaRow> eventosTable;
    @FXML
    private TableColumn<AgendaRow, String> dataColumn;
    @FXML
    private TableColumn<AgendaRow, String> tituloColumn;
    @FXML
    private TableColumn<AgendaRow, String> clienteColumn;
    @FXML
    private TableColumn<AgendaRow, String> tipoColumn;
    @FXML
    private TableColumn<AgendaRow, String> recorrenciaColumn;
    @FXML
    private TableColumn<AgendaRow, String> statusColumn;
    @FXML
    private TableColumn<AgendaRow, String> prioridadeColumn;
    @FXML
    private TableColumn<AgendaRow, String> responsavelColumn;
    @FXML
    private TableColumn<AgendaRow, String> lembreteColumn;
    @FXML
    private TableColumn<AgendaRow, String> vinculoColumn;

    public ControladorAgenda(
            CasoDeUsoAgenda agendaUseCase,
            CasoDeUsoCliente clienteUseCase,
            CasoDeUsoUsuario usuarioUseCase,
            ApresentadorData apresentadorData
    ) {
        this.agendaUseCase = agendaUseCase;
        this.clienteUseCase = clienteUseCase;
        this.usuarioUseCase = usuarioUseCase;
        this.apresentadorData = apresentadorData;
    }

    @FXML
    public void initialize() {
        periodoCombo.getItems().setAll("Mes", "Semana", "Dia");
        periodoCombo.getSelectionModel().select("Mes");
        dataReferenciaPicker.setValue(LocalDate.now());
        periodoCombo.setOnAction(event -> refresh());
        dataReferenciaPicker.setOnAction(event -> refresh());
        configurarTabela();
        refresh();
    }

    @FXML
    private void onNovoEvento() {
        abrirDialogo(null);
    }

    @FXML
    private void onEditarEvento() {
        AgendaRow row = selecionado();
        if (row == null) {
            return;
        }
        agendaUseCase.listAll().stream()
                .filter(evento -> evento.id().equals(row.id()))
                .findFirst()
                .ifPresent(this::abrirDialogo);
    }

    @FXML
    private void onReagendar() {
        AgendaRow row = selecionado();
        if (row == null) {
            return;
        }
        Dialog<CasoDeUsoAgenda.RescheduleVisitCommand> dialog = new Dialog<>();
        dialog.setTitle("Reagendar evento");
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        DatePicker dataPicker = new DatePicker(row.dataBase());
        TextField inicioField = new TextField(row.inicioHora());
        TextField fimField = new TextField(row.fimHora());
        dialog.getDialogPane().setContent(grid("Data", dataPicker, "Inicio", inicioField, "Fim", fimField));
        dialog.setResultConverter(button -> button == ButtonType.OK
                ? new CasoDeUsoAgenda.RescheduleVisitCommand(row.id(), at(dataPicker.getValue(), inicioField.getText()), at(dataPicker.getValue(), fimField.getText()))
                : null);
        dialog.showAndWait().ifPresent(command -> executar(() -> agendaUseCase.reschedule(command)));
    }

    @FXML
    private void onCancelar() {
        AgendaRow row = selecionado();
        if (row == null) {
            return;
        }
        executar(() -> agendaUseCase.cancel(new CasoDeUsoAgenda.ChangeVisitStatusCommand(row.id(), "Cancelado pela agenda.")));
    }

    @FXML
    private void onConcluir() {
        AgendaRow row = selecionado();
        if (row == null) {
            return;
        }
        executar(() -> agendaUseCase.complete(new CasoDeUsoAgenda.ChangeVisitStatusCommand(row.id(), "Concluido pela agenda.")));
    }

    @FXML
    private void onAtualizar() {
        refresh();
    }

    private void configurarTabela() {
        dataColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().data()));
        tituloColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().titulo()));
        clienteColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().cliente()));
        tipoColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().tipo()));
        recorrenciaColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().recorrencia()));
        statusColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().status()));
        prioridadeColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().prioridade()));
        responsavelColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().responsavel()));
        lembreteColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().lembrete()));
        vinculoColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().vinculo()));
    }

    private void refresh() {
        LocalDate reference = dataReferenciaPicker.getValue() == null ? LocalDate.now() : dataReferenciaPicker.getValue();
        LocalDate start;
        LocalDate end;
        switch (periodoCombo.getValue()) {
            case "Dia" -> {
                start = reference;
                end = reference;
            }
            case "Semana" -> {
                start = reference.minusDays(reference.getDayOfWeek().getValue() - 1L);
                end = start.plusDays(6);
            }
            default -> {
                YearMonth month = YearMonth.from(reference);
                start = month.atDay(1);
                end = month.atEndOfMonth();
            }
        }
        periodoLabel.setText(apresentadorData.format(start) + " a " + apresentadorData.format(end));
        List<Cliente> clientes = clienteUseCase.listAll();
        List<Usuario> usuarios = usuarioUseCase.listAll();
        eventosTable.getItems().setAll(agendaUseCase.listBetween(start, end).stream()
                .map(evento -> toRow(evento, clientes, usuarios))
                .sorted(Comparator.comparing(AgendaRow::dataBase).thenComparing(AgendaRow::inicioHora).thenComparing(AgendaRow::titulo))
                .toList());
    }

    private void abrirDialogo(VisitaAgendada atual) {
        Dialog<CasoDeUsoAgenda.ScheduleVisitCommand> dialog = new Dialog<>();
        dialog.setTitle(atual == null ? "Novo evento" : "Editar evento");
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<ClienteOption> clienteCombo = clientesCombo();
        select(clienteCombo, atual == null ? "" : atual.customerId());
        ComboBox<UsuarioOption> responsavelCombo = usuariosCombo();
        select(responsavelCombo, atual == null ? "" : atual.responsibleId());
        TextField tituloField = new TextField(atual == null ? "" : atual.title());
        TextArea descricaoArea = new TextArea(atual == null ? "" : atual.notes());
        descricaoArea.setPrefRowCount(3);
        ComboBox<VisitaAgendada.VisitType> tipoCombo = new ComboBox<>();
        tipoCombo.getItems().setAll(VisitaAgendada.VisitType.values());
        tipoCombo.getSelectionModel().select(atual == null ? VisitaAgendada.VisitType.ONE_OFF : atual.type());
        ComboBox<VisitaAgendada.Recurrence> recorrenciaCombo = new ComboBox<>();
        recorrenciaCombo.getItems().setAll(VisitaAgendada.Recurrence.values());
        recorrenciaCombo.getSelectionModel().select(atual == null ? VisitaAgendada.Recurrence.NONE : atual.recurrence());
        ComboBox<VisitaAgendada.VisitStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().setAll(VisitaAgendada.VisitStatus.values());
        statusCombo.getSelectionModel().select(atual == null ? VisitaAgendada.VisitStatus.SCHEDULED : atual.status());
        ComboBox<VisitaAgendada.VisitPriority> prioridadeCombo = new ComboBox<>();
        prioridadeCombo.getItems().setAll(VisitaAgendada.VisitPriority.values());
        prioridadeCombo.getSelectionModel().select(atual == null ? VisitaAgendada.VisitPriority.NORMAL : atual.priority());
        DatePicker dataPicker = new DatePicker(atual == null ? LocalDate.now() : atual.scheduledDate());
        TextField inicioField = new TextField(atual == null || atual.startAt() == null ? "08:00" : atual.startAt().toLocalTime().toString());
        TextField fimField = new TextField(atual == null || atual.endAt() == null ? "09:00" : atual.endAt().toLocalTime().toString());
        CheckBox diaInteiroCheck = new CheckBox("Dia inteiro");
        diaInteiroCheck.setSelected(atual != null && atual.allDay());
        CheckBox lembreteCheck = new CheckBox("Lembrete ativo");
        lembreteCheck.setSelected(atual != null && atual.reminderActive());
        TextField diasLembreteField = new TextField(atual == null ? "1" : String.valueOf(atual.reminderDaysBefore()));

        dialog.getDialogPane().setContent(grid(
                "Cliente", clienteCombo,
                "Responsavel", responsavelCombo,
                "Titulo", tituloField,
                "Descricao", descricaoArea,
                "Tipo", tipoCombo,
                "Recorrencia", recorrenciaCombo,
                "Status", statusCombo,
                "Prioridade", prioridadeCombo,
                "Data", dataPicker,
                "Inicio", inicioField,
                "Fim", fimField,
                "Dia inteiro", diaInteiroCheck,
                "Lembrete", lembreteCheck,
                "Dias lembrete", diasLembreteField
        ));
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            ClienteOption cliente = clienteCombo.getValue();
            UsuarioOption responsavel = responsavelCombo.getValue();
            LocalDate date = dataPicker.getValue();
            boolean allDay = diaInteiroCheck.isSelected();
            return new CasoDeUsoAgenda.ScheduleVisitCommand(
                    atual == null ? UUID.randomUUID().toString() : atual.id(),
                    cliente == null ? "" : cliente.id(),
                    atual == null ? "" : atual.orderId(),
                    atual == null ? "" : atual.contractId(),
                    atual == null ? "" : atual.certificateId(),
                    tipoCombo.getValue(),
                    recorrenciaCombo.getValue(),
                    date,
                    tituloField.getText(),
                    serviceType(tipoCombo.getValue()),
                    responsavel == null ? "" : responsavel.name(),
                    allDay ? date.atStartOfDay() : at(date, inicioField.getText()),
                    allDay ? date.atStartOfDay() : at(date, fimField.getText()),
                    allDay,
                    statusCombo.getValue(),
                    prioridadeCombo.getValue(),
                    responsavel == null ? "" : responsavel.id(),
                    lembreteCheck.isSelected(),
                    parseInt(diasLembreteField.getText(), 1),
                    descricaoArea.getText()
            );
        });
        dialog.showAndWait().ifPresent(command -> executar(() -> {
            if (atual == null) {
                agendaUseCase.schedule(command);
            } else {
                agendaUseCase.update(command);
            }
        }));
    }

    private AgendaRow toRow(VisitaAgendada evento, List<Cliente> clientes, List<Usuario> usuarios) {
        String baseId = evento.id().contains("#") ? evento.id().substring(0, evento.id().indexOf('#')) : evento.id();
        String vinculo = !evento.orderId().isBlank() ? "OS"
                : !evento.contractId().isBlank() ? "Contrato"
                : !evento.certificateId().isBlank() ? "Certificado"
                : "-";
        return new AgendaRow(
                baseId,
                evento.scheduledDate(),
                apresentadorData.format(evento.startAt()),
                evento.startAt() == null ? "" : evento.startAt().toLocalTime().toString(),
                evento.endAt() == null ? "" : evento.endAt().toLocalTime().toString(),
                evento.title(),
                nomeCliente(clientes, evento.customerId()),
                evento.serviceType().isBlank() ? evento.type().name() : evento.serviceType(),
                evento.recurrence().name(),
                status(evento),
                evento.priority().name(),
                nomeUsuario(usuarios, evento.responsibleId(), evento.internalResponsible()),
                evento.reminderActive() ? evento.reminderDaysBefore() + " dia(s)" : "Nao",
                vinculo
        );
    }

    private String status(VisitaAgendada evento) {
        if (evento.status() == VisitaAgendada.VisitStatus.SCHEDULED && evento.scheduledDate().isBefore(LocalDate.now())) {
            return "VENCIDO";
        }
        return evento.status().name();
    }

    private String serviceType(VisitaAgendada.VisitType type) {
        return switch (type == null ? VisitaAgendada.VisitType.ONE_OFF : type) {
            case MONTHLY -> "visita_mensal";
            case BIWEEKLY -> "visita_quinzenal";
            case ONE_OFF -> "servico_avulso";
        };
    }

    private ComboBox<ClienteOption> clientesCombo() {
        ComboBox<ClienteOption> combo = new ComboBox<>();
        combo.getItems().setAll(clienteUseCase.listAll().stream()
                .filter(Cliente::ativo)
                .map(cliente -> new ClienteOption(cliente.id(), nomeCliente(List.of(cliente), cliente.id())))
                .sorted(Comparator.comparing(ClienteOption::name))
                .toList());
        return combo;
    }

    private ComboBox<UsuarioOption> usuariosCombo() {
        ComboBox<UsuarioOption> combo = new ComboBox<>();
        combo.getItems().setAll(usuarioUseCase.listAll().stream()
                .filter(Usuario::ativo)
                .map(usuario -> new UsuarioOption(usuario.id(), usuario.nome()))
                .sorted(Comparator.comparing(UsuarioOption::name))
                .toList());
        return combo;
    }

    private <T extends Option> void select(ComboBox<T> combo, String id) {
        combo.getItems().stream().filter(option -> option.id().equals(id)).findFirst().ifPresent(combo.getSelectionModel()::select);
        if (combo.getSelectionModel().isEmpty() && !combo.getItems().isEmpty()) {
            combo.getSelectionModel().selectFirst();
        }
    }

    private AgendaRow selecionado() {
        AgendaRow row = eventosTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            mostrar("Selecione um evento.");
        }
        return row;
    }

    private void executar(Runnable action) {
        try {
            action.run();
            refresh();
        } catch (RuntimeException exception) {
            mostrar(exception.getMessage());
        }
    }

    private void mostrar(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.showAndWait();
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

    private LocalDateTime at(LocalDate date, String time) {
        return LocalDateTime.of(date, LocalTime.parse(time == null || time.isBlank() ? "00:00" : time.trim()));
    }

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }

    private String nomeCliente(List<Cliente> clientes, String clienteId) {
        return clientes.stream()
                .filter(cliente -> cliente.id().equals(clienteId))
                .findFirst()
                .map(cliente -> cliente.tipo() == Cliente.TipoCliente.PESSOA_JURIDICA
                        ? firstNonBlank(cliente.nomeFantasia(), cliente.razaoSocial(), cliente.name())
                        : cliente.name())
                .orElse(clienteId == null || clienteId.isBlank() ? "-" : clienteId);
    }

    private String nomeUsuario(List<Usuario> usuarios, String usuarioId, String fallback) {
        return usuarios.stream()
                .filter(usuario -> usuario.id().equals(usuarioId))
                .findFirst()
                .map(Usuario::nome)
                .orElse(fallback == null || fallback.isBlank() ? "-" : fallback);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "-";
    }

    private interface Option {
        String id();
    }

    private record ClienteOption(String id, String name) implements Option {
        @Override
        public String toString() {
            return name;
        }
    }

    private record UsuarioOption(String id, String name) implements Option {
        @Override
        public String toString() {
            return name;
        }
    }

    public record AgendaRow(
            String id,
            LocalDate dataBase,
            String data,
            String inicioHora,
            String fimHora,
            String titulo,
            String cliente,
            String tipo,
            String recorrencia,
            String status,
            String prioridade,
            String responsavel,
            String lembrete,
            String vinculo
    ) {
    }
}

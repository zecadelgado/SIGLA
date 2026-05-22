package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.agenda.porta.entrada.CasoDeUsoAgenda;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaReferencias;
import br.com.sigla.interfacegrafica.modelo.OpcaoId;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.UtilComboBox;
import br.com.sigla.interfacegrafica.util.UtilJanela;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Component
public class ControladorNovoEvento {

    private final CasoDeUsoAgenda casoDeUsoAgenda;
    private final ServicoConsultaReferencias servicoConsultaReferencias;
    private final GerenciadorNavegacao gerenciadorNavegacao;

    @FXML
    private ComboBox<OpcaoId> clienteCombo;
    @FXML
    private ComboBox<OpcaoId> contratoCombo;
    @FXML
    private ComboBox<OpcaoId> ordemCombo;
    @FXML
    private ComboBox<OpcaoId> responsavelCombo;
    @FXML
    private TextField tituloField;
    @FXML
    private TextArea descricaoArea;
    @FXML
    private ComboBox<VisitaAgendada.VisitType> tipoCombo;
    @FXML
    private ComboBox<VisitaAgendada.Recurrence> recorrenciaCombo;
    @FXML
    private ComboBox<VisitaAgendada.VisitStatus> statusCombo;
    @FXML
    private ComboBox<VisitaAgendada.VisitPriority> prioridadeCombo;
    @FXML
    private DatePicker dataPicker;
    @FXML
    private TextField inicioField;
    @FXML
    private TextField fimField;
    @FXML
    private CheckBox diaInteiroCheck;
    @FXML
    private CheckBox lembreteCheck;
    @FXML
    private TextField diasLembreteField;
    @FXML
    private Label feedbackLabel;

    public ControladorNovoEvento(
            CasoDeUsoAgenda casoDeUsoAgenda,
            ServicoConsultaReferencias servicoConsultaReferencias,
            GerenciadorNavegacao gerenciadorNavegacao
    ) {
        this.casoDeUsoAgenda = casoDeUsoAgenda;
        this.servicoConsultaReferencias = servicoConsultaReferencias;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
    }

    @FXML
    public void initialize() {
        UtilComboBox.preencher(clienteCombo, servicoConsultaReferencias.clientes(), true);
        UtilComboBox.preencher(contratoCombo, servicoConsultaReferencias.contratos(), true);
        UtilComboBox.preencher(ordemCombo, servicoConsultaReferencias.ordensServico(), true);
        UtilComboBox.preencher(responsavelCombo, servicoConsultaReferencias.funcionarios(), true);
        if (clienteCombo != null) {
            clienteCombo.valueProperty().addListener((observable, oldValue, newValue) -> atualizarRelacionadosPorCliente());
        }
        if (contratoCombo != null) {
            contratoCombo.valueProperty().addListener((observable, oldValue, newValue) -> sincronizarClientePorContrato());
        }
        if (ordemCombo != null) {
            ordemCombo.valueProperty().addListener((observable, oldValue, newValue) -> sincronizarPorOrdem());
        }
        if (tipoCombo != null) {
            tipoCombo.getItems().setAll(VisitaAgendada.VisitType.values());
            tipoCombo.getSelectionModel().select(VisitaAgendada.VisitType.ONE_OFF);
        }
        if (recorrenciaCombo != null) {
            recorrenciaCombo.getItems().setAll(VisitaAgendada.Recurrence.values());
            recorrenciaCombo.getSelectionModel().select(VisitaAgendada.Recurrence.NONE);
        }
        if (statusCombo != null) {
            statusCombo.getItems().setAll(VisitaAgendada.VisitStatus.values());
            statusCombo.getSelectionModel().select(VisitaAgendada.VisitStatus.SCHEDULED);
        }
        if (prioridadeCombo != null) {
            prioridadeCombo.getItems().setAll(VisitaAgendada.VisitPriority.values());
            prioridadeCombo.getSelectionModel().select(VisitaAgendada.VisitPriority.NORMAL);
        }
        if (dataPicker != null) {
            dataPicker.setValue(LocalDate.now());
        }
        if (inicioField != null) {
            inicioField.setText("08:00");
        }
        if (fimField != null) {
            fimField.setText("09:00");
        }
        if (diasLembreteField != null) {
            diasLembreteField.setText("1");
        }
        setFeedback("");
    }

    @FXML
    private void onConfirmar() {
        try {
            LocalDate data = dataPicker == null || dataPicker.getValue() == null ? LocalDate.now() : dataPicker.getValue();
            LocalTime inicio = parseHora(inicioField == null ? "" : inicioField.getText(), LocalTime.of(8, 0));
            LocalTime fim = parseHora(fimField == null ? "" : fimField.getText(), LocalTime.of(9, 0));
            casoDeUsoAgenda.schedule(new CasoDeUsoAgenda.ScheduleVisitCommand(
                    UUID.randomUUID().toString(),
                    UtilComboBox.idSelecionado(clienteCombo),
                    UtilComboBox.idSelecionado(ordemCombo),
                    UtilComboBox.idSelecionado(contratoCombo),
                    tipoCombo == null || tipoCombo.getValue() == null ? VisitaAgendada.VisitType.ONE_OFF : tipoCombo.getValue(),
                    recorrenciaCombo == null || recorrenciaCombo.getValue() == null ? VisitaAgendada.Recurrence.NONE : recorrenciaCombo.getValue(),
                    data,
                    tituloField == null ? "" : tituloField.getText(),
                    "servico",
                    "",
                    data.atTime(inicio),
                    data.atTime(fim),
                    diaInteiroCheck != null && diaInteiroCheck.isSelected(),
                    statusCombo == null || statusCombo.getValue() == null ? VisitaAgendada.VisitStatus.SCHEDULED : statusCombo.getValue(),
                    prioridadeCombo == null || prioridadeCombo.getValue() == null ? VisitaAgendada.VisitPriority.NORMAL : prioridadeCombo.getValue(),
                    UtilComboBox.idSelecionado(responsavelCombo),
                    lembreteCheck != null && lembreteCheck.isSelected(),
                    parseInt(diasLembreteField == null ? "" : diasLembreteField.getText(), 1),
                    descricaoArea == null ? "" : descricaoArea.getText()
            ));
            gerenciadorNavegacao.navigateTo(VisaoAplicacao.AGENDA);
            UtilJanela.fecharJanela(clienteCombo);
        } catch (Exception exception) {
            setFeedback(exception.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        UtilJanela.fecharJanela(clienteCombo);
    }

    private void atualizarRelacionadosPorCliente() {
        String clienteId = UtilComboBox.idSelecionado(clienteCombo);
        UtilComboBox.preencher(contratoCombo, clienteId.isBlank()
                ? servicoConsultaReferencias.contratos()
                : servicoConsultaReferencias.contratosPorCliente(clienteId), true);
        UtilComboBox.preencher(ordemCombo, clienteId.isBlank()
                ? servicoConsultaReferencias.ordensServico()
                : servicoConsultaReferencias.ordensServicoPorCliente(clienteId), true);
    }

    private void sincronizarClientePorContrato() {
        String contratoId = UtilComboBox.idSelecionado(contratoCombo);
        if (!contratoId.isBlank()) {
            UtilComboBox.selecionarPorId(clienteCombo, servicoConsultaReferencias.clienteIdPorContrato(contratoId));
            UtilComboBox.selecionarPorId(contratoCombo, contratoId);
            UtilComboBox.preencher(ordemCombo, servicoConsultaReferencias.ordensServicoPorContrato(contratoId), true);
        }
    }

    private void sincronizarPorOrdem() {
        String ordemId = UtilComboBox.idSelecionado(ordemCombo);
        if (!ordemId.isBlank()) {
            UtilComboBox.selecionarPorId(clienteCombo, servicoConsultaReferencias.clienteIdPorOrdem(ordemId));
            UtilComboBox.selecionarPorId(contratoCombo, servicoConsultaReferencias.contratoIdPorOrdem(ordemId));
            UtilComboBox.selecionarPorId(ordemCombo, ordemId);
        }
    }

    private LocalTime parseHora(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return LocalTime.parse(value.trim());
    }

    private int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(value.trim());
    }

    private void setFeedback(String message) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message == null ? "" : message);
        }
    }
}

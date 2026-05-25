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
<<<<<<< Updated upstream
=======
import javafx.scene.control.CheckBox;
>>>>>>> Stashed changes
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class ControladorNovoServico {

    private final CasoDeUsoAgenda casoDeUsoAgenda;
    private final ServicoConsultaReferencias servicoConsultaReferencias;
    private final GerenciadorNavegacao gerenciadorNavegacao;

    @FXML
    private ComboBox<OpcaoId> clienteCombo;
    @FXML
    private ComboBox<OpcaoId> ordemCombo;
    @FXML
    private TextField tituloField;
    @FXML
    private TextField descricaoField;
    @FXML
    private DatePicker dataInicioPicker;
    @FXML
    private DatePicker dataFimPicker;
    @FXML
    private DatePicker diaInteiroPicker;
    @FXML
    private ComboBox<VisitaAgendada.VisitStatus> statusCombo;
    @FXML
    private ComboBox<VisitaAgendada.VisitPriority> prioridadeCombo;
    @FXML
    private ComboBox<OpcaoId> responsavelCombo;
    @FXML
    private Label feedbackLabel;

    public ControladorNovoServico(
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
        LocalDate hoje = LocalDate.now();
        if (dataInicioPicker != null && dataInicioPicker.getValue() == null) {
            dataInicioPicker.setValue(hoje);
        }
        if (dataFimPicker != null && dataFimPicker.getValue() == null) {
            dataFimPicker.setValue(hoje);
        }
        UtilComboBox.preencher(clienteCombo, servicoConsultaReferencias.clientes(), true);
        UtilComboBox.preencher(ordemCombo, servicoConsultaReferencias.ordensServico(), true);
        UtilComboBox.preencher(responsavelCombo, servicoConsultaReferencias.funcionarios(), true);
<<<<<<< Updated upstream
        if (clienteCombo != null) {
            clienteCombo.valueProperty().addListener((observable, oldValue, newValue) -> atualizarOrdensPorCliente());
        }
        if (ordemCombo != null) {
            ordemCombo.valueProperty().addListener((observable, oldValue, newValue) -> sincronizarClientePorOrdem());
        }
        if (statusCombo != null) {
            statusCombo.getItems().setAll(VisitaAgendada.VisitStatus.values());
            statusCombo.getSelectionModel().select(VisitaAgendada.VisitStatus.SCHEDULED);
        }
        if (prioridadeCombo != null) {
            prioridadeCombo.getItems().setAll(VisitaAgendada.VisitPriority.values());
            prioridadeCombo.getSelectionModel().select(VisitaAgendada.VisitPriority.NORMAL);
=======
        if (statusCombo != null) {
            statusCombo.getItems().setAll(VisitaAgendada.VisitStatus.values());
            statusCombo.getSelectionModel().select(VisitaAgendada.VisitStatus.SCHEDULED);
        }
        if (prioridadeCombo != null) {
            prioridadeCombo.getItems().setAll(VisitaAgendada.VisitPriority.values());
            prioridadeCombo.getSelectionModel().select(VisitaAgendada.VisitPriority.NORMAL);
        }
        if (clienteCombo != null) {
            clienteCombo.valueProperty().addListener((observable, oldValue, newValue) -> atualizarOrdensPorCliente());
        }
        if (ordemCombo != null) {
            ordemCombo.valueProperty().addListener((observable, oldValue, newValue) -> sincronizarClientePorOrdem());
>>>>>>> Stashed changes
        }
        setFeedback("");
    }

    @FXML
    private void onConfirmar() {
        try {
<<<<<<< Updated upstream
            String clienteId = UtilComboBox.idSelecionado(clienteCombo);
=======
            OpcaoId cliente = UtilComboBox.obrigatorio(clienteCombo, "Selecione um cliente.");
>>>>>>> Stashed changes
            String ordemId = UtilComboBox.idSelecionado(ordemCombo);
            LocalDate dataInicio = dataInicioPicker == null || dataInicioPicker.getValue() == null ? LocalDate.now() : dataInicioPicker.getValue();
            LocalDate dataFim = dataFimPicker == null || dataFimPicker.getValue() == null ? dataInicio : dataFimPicker.getValue();

            casoDeUsoAgenda.schedule(new CasoDeUsoAgenda.ScheduleVisitCommand(
                    UUID.randomUUID().toString(),
<<<<<<< Updated upstream
                    clienteId,
                    ordemId,
                    "",
=======
                    cliente.id(),
                    ordemId,
                    "",
                    "",
>>>>>>> Stashed changes
                    VisitaAgendada.VisitType.ONE_OFF,
                    VisitaAgendada.Recurrence.NONE,
                    dataInicio,
                    tituloField == null ? "" : tituloField.getText(),
                    "servico",
                    "",
                    dataInicio.atStartOfDay(),
                    dataFim.atTime(23, 59),
<<<<<<< Updated upstream
                    diaInteiroPicker != null && diaInteiroPicker.getValue() != null,
=======
                    diaInteiroCheck != null && diaInteiroCheck.isSelected(),
>>>>>>> Stashed changes
                    statusCombo == null || statusCombo.getValue() == null ? VisitaAgendada.VisitStatus.SCHEDULED : statusCombo.getValue(),
                    prioridadeCombo == null || prioridadeCombo.getValue() == null ? VisitaAgendada.VisitPriority.NORMAL : prioridadeCombo.getValue(),
                    UtilComboBox.idSelecionado(responsavelCombo),
                    false,
<<<<<<< Updated upstream
                    1,
=======
                    0,
>>>>>>> Stashed changes
                    mergeDescricao()
            ));
            gerenciadorNavegacao.navigateTo(VisaoAplicacao.SERVICES);
            UtilJanela.fecharJanela(clienteCombo);
        } catch (Exception exception) {
            setFeedback(exception.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        UtilJanela.fecharJanela(clienteCombo);
    }

    private String mergeDescricao() {
        String descricao = descricaoField == null ? "" : descricaoField.getText().trim();
        if (descricao.isBlank()) {
            throw new IllegalArgumentException("Descreva o servico prestado.");
        }
        String titulo = tituloField == null ? "" : tituloField.getText().trim();
        String texto = titulo.isBlank() ? descricao : titulo + " - " + descricao;
<<<<<<< Updated upstream
        return diaInteiroPicker != null && diaInteiroPicker.getValue() != null ? texto + " | Dia inteiro" : texto;
=======
        return diaInteiroCheck != null && diaInteiroCheck.isSelected() ? texto + " | Dia inteiro" : texto;
>>>>>>> Stashed changes
    }

    private void setFeedback(String message) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message == null ? "" : message);
        }
    }

    private void atualizarOrdensPorCliente() {
        String clienteId = UtilComboBox.idSelecionado(clienteCombo);
        UtilComboBox.preencher(ordemCombo, clienteId.isBlank()
                ? servicoConsultaReferencias.ordensServico()
                : servicoConsultaReferencias.ordensServicoPorCliente(clienteId), true);
    }

    private void sincronizarClientePorOrdem() {
        String ordemId = UtilComboBox.idSelecionado(ordemCombo);
        if (!ordemId.isBlank()) {
            UtilComboBox.selecionarPorId(clienteCombo, servicoConsultaReferencias.clienteIdPorOrdem(ordemId));
            UtilComboBox.selecionarPorId(ordemCombo, ordemId);
        }
    }
}

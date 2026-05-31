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
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

import static br.com.sigla.interfacegrafica.util.ResolvedorEntradaTexto.parseEnum;
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
    private ComboBox<OpcaoId> contratoCombo;
    @FXML
    private TextField tituloField;
    @FXML
    private TextField descricaoField;
    @FXML
    private DatePicker dataInicioPicker;
    @FXML
    private DatePicker dataFimPicker;
    @FXML
    private CheckBox diaInteiroCheck;
    @FXML
    private TextField statusField;
    @FXML
    private TextField prioridadeField;
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
        UtilComboBox.preencher(clienteCombo, servicoConsultaReferencias.clientes(), false);
        UtilComboBox.preencher(ordemCombo, servicoConsultaReferencias.ordensServico(), true);
        UtilComboBox.preencher(contratoCombo, servicoConsultaReferencias.contratos(), true);
        UtilComboBox.preencher(responsavelCombo, servicoConsultaReferencias.funcionarios(), true);
        if (clienteCombo != null) {
            clienteCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
                String clienteId = UtilComboBox.idSelecionado(clienteCombo);
                UtilComboBox.preencher(ordemCombo, servicoConsultaReferencias.ordensServicoDoCliente(clienteId), true);
                UtilComboBox.preencher(contratoCombo, servicoConsultaReferencias.contratosDoCliente(clienteId), true);
            });
        }
        LocalDate hoje = LocalDate.now();
        if (dataInicioPicker != null && dataInicioPicker.getValue() == null) {
            dataInicioPicker.setValue(hoje);
        }
        if (dataFimPicker != null && dataFimPicker.getValue() == null) {
            dataFimPicker.setValue(hoje);
        }
        if (statusField != null && statusField.getText().isBlank()) {
            statusField.setText(VisitaAgendada.VisitStatus.SCHEDULED.name());
        }
        if (prioridadeField != null && prioridadeField.getText().isBlank()) {
            prioridadeField.setText(VisitaAgendada.VisitPriority.NORMAL.name());
        }
        setFeedback("");
    }

    @FXML
    private void onConfirmar() {
        try {
            OpcaoId cliente = requiredOption(UtilComboBox.selecionado(clienteCombo), "Selecione um cliente.");
            OpcaoId ordem = UtilComboBox.selecionado(ordemCombo);
            OpcaoId contrato = UtilComboBox.selecionado(contratoCombo);
            LocalDate dataInicio = dataInicioPicker == null || dataInicioPicker.getValue() == null ? LocalDate.now() : dataInicioPicker.getValue();
            LocalDate dataFim = dataFimPicker == null || dataFimPicker.getValue() == null ? dataInicio : dataFimPicker.getValue();

            casoDeUsoAgenda.schedule(new CasoDeUsoAgenda.ScheduleVisitCommand(
                    UUID.randomUUID().toString(),
                    cliente.id(),
                    ordem == null ? "" : ordem.id(),
                    contrato == null ? "" : contrato.id(),
                    "",
                    VisitaAgendada.VisitType.ONE_OFF,
                    VisitaAgendada.Recurrence.NONE,
                    dataInicio,
                    tituloField == null ? "" : tituloField.getText(),
                    "servico",
                    "",
                    dataInicio.atStartOfDay(),
                    dataFim.atTime(23, 59),
                    diaInteiroCheck != null && diaInteiroCheck.isSelected(),
                    parseEnum(VisitaAgendada.VisitStatus.class, statusField == null ? "" : statusField.getText(), VisitaAgendada.VisitStatus.SCHEDULED),
                    parseEnum(VisitaAgendada.VisitPriority.class, prioridadeField == null ? "" : prioridadeField.getText(), VisitaAgendada.VisitPriority.NORMAL),
                    UtilComboBox.idSelecionado(responsavelCombo),
                    false,
                    0,
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
        return diaInteiroCheck != null && diaInteiroCheck.isSelected() ? texto + " | Dia inteiro" : texto;
    }

    private OpcaoId requiredOption(OpcaoId value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private void setFeedback(String message) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message == null ? "" : message);
        }
    }
}

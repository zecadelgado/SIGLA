package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.agenda.porta.entrada.CasoDeUsoAgenda;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaReferencias;
import br.com.sigla.interfacegrafica.modelo.OpcaoId;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.UtilJanela;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

import static br.com.sigla.interfacegrafica.util.ResolvedorEntradaTexto.parseEnum;
import static br.com.sigla.interfacegrafica.util.ResolvedorEntradaTexto.resolveOpcional;

@Component
public class ControladorNovoServico {

    private final CasoDeUsoAgenda casoDeUsoAgenda;
    private final ServicoConsultaReferencias servicoConsultaReferencias;
    private final GerenciadorNavegacao gerenciadorNavegacao;

    @FXML
    private TextField clienteField;
    @FXML
    private TextField ordemField;
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
    private TextField responsavelField;
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
            OpcaoId cliente = requiredOption(resolveOpcional(servicoConsultaReferencias.clientes(), clienteField == null ? "" : clienteField.getText()), "Selecione um cliente.");
            OpcaoId ordem = resolveOpcional(servicoConsultaReferencias.ordensServico(), ordemField == null ? "" : ordemField.getText());
            LocalDate dataInicio = dataInicioPicker == null || dataInicioPicker.getValue() == null ? LocalDate.now() : dataInicioPicker.getValue();
            LocalDate dataFim = dataFimPicker == null || dataFimPicker.getValue() == null ? dataInicio : dataFimPicker.getValue();

            casoDeUsoAgenda.schedule(new CasoDeUsoAgenda.ScheduleVisitCommand(
                    UUID.randomUUID().toString(),
                    cliente.id(),
                    ordem == null ? "" : ordem.id(),
                    VisitaAgendada.VisitType.ONE_OFF,
                    dataInicio,
                    tituloField == null ? "" : tituloField.getText(),
                    "servico",
                    "",
                    dataInicio.atStartOfDay(),
                    dataFim.atTime(23, 59),
                    diaInteiroCheck != null && diaInteiroCheck.isSelected(),
                    parseEnum(VisitaAgendada.VisitStatus.class, statusField == null ? "" : statusField.getText(), VisitaAgendada.VisitStatus.SCHEDULED),
                    parseEnum(VisitaAgendada.VisitPriority.class, prioridadeField == null ? "" : prioridadeField.getText(), VisitaAgendada.VisitPriority.NORMAL),
                    onlyUuid(responsavelField == null ? "" : responsavelField.getText()),
                    mergeDescricao()
            ));
            gerenciadorNavegacao.navigateTo(VisaoAplicacao.SERVICES);
            UtilJanela.fecharJanela(clienteField);
        } catch (Exception exception) {
            setFeedback(exception.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        UtilJanela.fecharJanela(clienteField);
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

    private String onlyUuid(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            return UUID.fromString(value.trim()).toString();
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private void setFeedback(String message) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message == null ? "" : message);
        }
    }
}

package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.agenda.porta.entrada.CasoDeUsoAgenda;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaReferencias;
import br.com.sigla.interfacegrafica.modelo.OpcaoId;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.UtilJanela;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static br.com.sigla.interfacegrafica.util.ResolvedorEntradaTexto.parseEnum;
import static br.com.sigla.interfacegrafica.util.ResolvedorEntradaTexto.resolveOpcional;

@Component
public class ControladorNovaOrdemServico {

    private final CasoDeUsoAgenda casoDeUsoAgenda;
    private final ServicoConsultaReferencias servicoConsultaReferencias;
    private final GerenciadorNavegacao gerenciadorNavegacao;

    @FXML
    private TextField clienteField;
    @FXML
    private TextField tituloField;
    @FXML
    private TextField descricaoField;
    @FXML
    private TextField tipoServicoField;
    @FXML
    private TextField statusField;
    @FXML
    private TextField responsavelInternoField;
    @FXML
    private DatePicker dataAgendadaPicker;
    @FXML
    private DatePicker dataInicioPicker;
    @FXML
    private DatePicker dataFimPicker;
    @FXML
    private TextField responsavelSecundarioField;
    @FXML
    private TextField executadoPorField;
    @FXML
    private TextField valorServicoField;
    @FXML
    private TextField observacoesField;
    @FXML
    private Label feedbackLabel;

    public ControladorNovaOrdemServico(
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
        if (dataAgendadaPicker != null && dataAgendadaPicker.getValue() == null) {
            dataAgendadaPicker.setValue(hoje);
        }
        if (dataInicioPicker != null && dataInicioPicker.getValue() == null) {
            dataInicioPicker.setValue(hoje);
        }
        if (dataFimPicker != null && dataFimPicker.getValue() == null) {
            dataFimPicker.setValue(hoje);
        }
        if (statusField != null && statusField.getText().isBlank()) {
            statusField.setText(VisitaAgendada.VisitStatus.SCHEDULED.name());
        }
        setFeedback("");
    }

    @FXML
    private void onConfirmar() {
        try {
            OpcaoId cliente = requiredOption(resolveOpcional(servicoConsultaReferencias.clientes(), clienteField == null ? "" : clienteField.getText()), "Selecione um cliente.");
            LocalDate dataAgendada = dataAgendadaPicker == null ? LocalDate.now() : dataAgendadaPicker.getValue();
            LocalDate dataInicio = dataInicioPicker == null || dataInicioPicker.getValue() == null ? dataAgendada : dataInicioPicker.getValue();
            LocalDate dataFim = dataFimPicker == null || dataFimPicker.getValue() == null ? dataAgendada : dataFimPicker.getValue();
            LocalDateTime inicio = dataInicio.atTime(8, 0);
            LocalDateTime fim = dataFim.atTime(18, 0);

            casoDeUsoAgenda.schedule(new CasoDeUsoAgenda.ScheduleVisitCommand(
                    "OS-" + System.currentTimeMillis(),
                    cliente.id(),
                    "",
                    VisitaAgendada.VisitType.ONE_OFF,
                    dataAgendada,
                    tituloField == null ? "" : tituloField.getText(),
                    tipoServicoField == null ? "" : tipoServicoField.getText(),
                    chooseResponsible(),
                    inicio,
                    fim,
                    false,
                    parseEnum(VisitaAgendada.VisitStatus.class, statusField == null ? "" : statusField.getText(), VisitaAgendada.VisitStatus.SCHEDULED),
                    mergeObservacoes()
            ));
            gerenciadorNavegacao.navigateTo(VisaoAplicacao.SERVICE_ORDER);
            UtilJanela.fecharJanela(clienteField);
        } catch (Exception exception) {
            setFeedback(exception.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        UtilJanela.fecharJanela(clienteField);
    }

    private OpcaoId requiredOption(OpcaoId value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String chooseResponsible() {
        if (responsavelInternoField != null && !responsavelInternoField.getText().isBlank()) {
            return responsavelInternoField.getText();
        }
        if (responsavelSecundarioField != null && !responsavelSecundarioField.getText().isBlank()) {
            return responsavelSecundarioField.getText();
        }
        return executadoPorField == null ? "" : executadoPorField.getText();
    }

    private String mergeObservacoes() {
        return Stream.of(
                        descricaoField == null ? "" : descricaoField.getText(),
                        observacoesField == null ? "" : observacoesField.getText(),
                        valorServicoField == null || valorServicoField.getText().isBlank() ? "" : "Valor: " + valorServicoField.getText()
                )
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + " | " + right)
                .orElse("");
    }

    private void setFeedback(String message) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message == null ? "" : message);
        }
    }
}

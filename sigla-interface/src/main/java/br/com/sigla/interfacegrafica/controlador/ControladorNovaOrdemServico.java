package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.dominio.servicos.OrdemServico;
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
import java.util.UUID;
import java.util.stream.Stream;

import static br.com.sigla.interfacegrafica.util.ResolvedorEntradaTexto.parseEnum;
import static br.com.sigla.interfacegrafica.util.ResolvedorEntradaTexto.resolveOpcional;

@Component
public class ControladorNovaOrdemServico {

    private final CasoDeUsoOrdemServico casoDeUsoOrdemServico;
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
            CasoDeUsoOrdemServico casoDeUsoOrdemServico,
            ServicoConsultaReferencias servicoConsultaReferencias,
            GerenciadorNavegacao gerenciadorNavegacao
    ) {
        this.casoDeUsoOrdemServico = casoDeUsoOrdemServico;
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
            statusField.setText(OrdemServico.OrdemServicoStatus.AGENDADA.name());
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

            casoDeUsoOrdemServico.create(new CasoDeUsoOrdemServico.CreateOrdemServicoCommand(
                    UUID.randomUUID().toString(),
                    cliente.id(),
                    tituloField == null ? "" : tituloField.getText(),
                    descricaoField == null ? "" : descricaoField.getText(),
                    tipoServicoField == null ? "" : tipoServicoField.getText(),
                    parseEnum(OrdemServico.OrdemServicoStatus.class, statusField == null ? "" : statusField.getText(), OrdemServico.OrdemServicoStatus.AGENDADA),
                    dataAgendada.atStartOfDay(),
                    inicio,
                    fim,
                    onlyUuid(chooseResponsible()),
                    onlyUuid(executadoPorField == null ? "" : executadoPorField.getText()),
                    parseMoney(valorServicoField == null ? "" : valorServicoField.getText()),
                    observacoesField == null ? "" : observacoesField.getText()
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

    private java.math.BigDecimal parseMoney(String value) {
        if (value == null || value.isBlank()) {
            return java.math.BigDecimal.ZERO;
        }
        return new java.math.BigDecimal(value.trim().replace(",", "."));
    }

    private void setFeedback(String message) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message == null ? "" : message);
        }
    }
}

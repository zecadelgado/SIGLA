package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.dominio.servicos.OrdemServico;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaReferencias;
import br.com.sigla.interfacegrafica.modelo.OpcaoId;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.UtilComboBox;
import br.com.sigla.interfacegrafica.util.UtilJanela;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class ControladorNovaOrdemServico {

    private final CasoDeUsoOrdemServico casoDeUsoOrdemServico;
    private final ServicoConsultaReferencias servicoConsultaReferencias;
    private final GerenciadorNavegacao gerenciadorNavegacao;

    @FXML
    private ComboBox<OpcaoId> clienteCombo;
    @FXML
    private ComboBox<OpcaoId> contratoCombo;
    @FXML
    private TextField tituloField;
    @FXML
    private TextField descricaoField;
    @FXML
    private TextField tipoServicoField;
    @FXML
    private ComboBox<OrdemServico.OrdemServicoStatus> statusCombo;
    @FXML
    private ComboBox<OpcaoId> responsavelInternoCombo;
    @FXML
    private DatePicker dataAgendadaPicker;
    @FXML
    private DatePicker dataInicioPicker;
    @FXML
    private DatePicker dataFimPicker;
    @FXML
    private ComboBox<OpcaoId> responsavelSecundarioCombo;
    @FXML
    private ComboBox<OpcaoId> executadoPorCombo;
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
        UtilComboBox.preencher(clienteCombo, servicoConsultaReferencias.clientes(), true);
        UtilComboBox.preencher(contratoCombo, servicoConsultaReferencias.contratos(), true);
        UtilComboBox.preencher(responsavelInternoCombo, servicoConsultaReferencias.funcionarios(), true);
        UtilComboBox.preencher(responsavelSecundarioCombo, servicoConsultaReferencias.funcionarios(), true);
        UtilComboBox.preencher(executadoPorCombo, servicoConsultaReferencias.funcionarios(), true);
        if (clienteCombo != null) {
            clienteCombo.valueProperty().addListener((observable, oldValue, newValue) -> atualizarContratosPorCliente());
        }
        if (contratoCombo != null) {
            contratoCombo.valueProperty().addListener((observable, oldValue, newValue) -> sincronizarClientePorContrato());
        }
        if (statusCombo != null) {
            statusCombo.getItems().setAll(OrdemServico.OrdemServicoStatus.values());
            statusCombo.getSelectionModel().select(OrdemServico.OrdemServicoStatus.AGENDADA);
        }
        setFeedback("");
    }

    @FXML
    private void onConfirmar() {
        try {
            LocalDate dataAgendada = dataAgendadaPicker == null ? LocalDate.now() : dataAgendadaPicker.getValue();
            LocalDate dataInicio = dataInicioPicker == null || dataInicioPicker.getValue() == null ? dataAgendada : dataInicioPicker.getValue();
            LocalDate dataFim = dataFimPicker == null || dataFimPicker.getValue() == null ? dataAgendada : dataFimPicker.getValue();
            LocalDateTime inicio = dataInicio.atTime(8, 0);
            LocalDateTime fim = dataFim.atTime(18, 0);

            casoDeUsoOrdemServico.create(new CasoDeUsoOrdemServico.CreateOrdemServicoCommand(
                    UUID.randomUUID().toString(),
                    UtilComboBox.idSelecionado(clienteCombo),
                    UtilComboBox.idSelecionado(contratoCombo),
                    tituloField == null ? "" : tituloField.getText(),
                    descricaoField == null ? "" : descricaoField.getText(),
                    tipoServicoField == null ? "" : tipoServicoField.getText(),
                    statusCombo == null || statusCombo.getValue() == null ? OrdemServico.OrdemServicoStatus.AGENDADA : statusCombo.getValue(),
                    dataAgendada.atStartOfDay(),
                    inicio,
                    fim,
                    chooseResponsible(),
                    UtilComboBox.idSelecionado(executadoPorCombo),
                    parseMoney(valorServicoField == null ? "" : valorServicoField.getText()),
                    observacoesField == null ? "" : observacoesField.getText()
            ));
            gerenciadorNavegacao.navigateTo(VisaoAplicacao.SERVICE_ORDER);
            UtilJanela.fecharJanela(clienteCombo);
        } catch (Exception exception) {
            setFeedback(exception.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        UtilJanela.fecharJanela(clienteCombo);
    }

    @FXML
    private void onDragDropped(DragEvent event) {
        event.consume();
    }

    @FXML
    private void onDragOver(DragEvent event) {
        event.consume();
    }

    @FXML
    private void onClickSelecionar(MouseEvent event) {
        event.consume();
    }

    private String chooseResponsible() {
        String responsavelInterno = UtilComboBox.idSelecionado(responsavelInternoCombo);
        if (!responsavelInterno.isBlank()) {
            return responsavelInterno;
        }
        String responsavelSecundario = UtilComboBox.idSelecionado(responsavelSecundarioCombo);
        if (!responsavelSecundario.isBlank()) {
            return responsavelSecundario;
        }
        return UtilComboBox.idSelecionado(executadoPorCombo);
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

    private void atualizarContratosPorCliente() {
        String clienteId = UtilComboBox.idSelecionado(clienteCombo);
        UtilComboBox.preencher(contratoCombo, clienteId.isBlank()
                ? servicoConsultaReferencias.contratos()
                : servicoConsultaReferencias.contratosPorCliente(clienteId), true);
    }

    private void sincronizarClientePorContrato() {
        String contratoId = UtilComboBox.idSelecionado(contratoCombo);
        if (!contratoId.isBlank()) {
            UtilComboBox.selecionarPorId(clienteCombo, servicoConsultaReferencias.clienteIdPorContrato(contratoId));
            UtilComboBox.selecionarPorId(contratoCombo, contratoId);
        }
    }
}

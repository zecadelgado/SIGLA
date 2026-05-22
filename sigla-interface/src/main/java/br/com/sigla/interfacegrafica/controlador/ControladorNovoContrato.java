package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.dominio.contratos.Contrato;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class ControladorNovoContrato {

    private final CasoDeUsoContrato casoDeUsoContrato;
    private final ServicoConsultaReferencias servicoConsultaReferencias;
    private final GerenciadorNavegacao gerenciadorNavegacao;

    @FXML
    private ComboBox<OpcaoId> clienteCombo;
    @FXML
    private TextField descricaoField;
    @FXML
    private DatePicker inicioPicker;
    @FXML
    private DatePicker fimPicker;
    @FXML
    private ComboBox<Contrato.ContratoType> tipoCombo;
    @FXML
    private ComboBox<Contrato.ServiceFrequency> frequenciaCombo;
    @FXML
    private ComboBox<Contrato.ContratoStatus> statusCombo;
    @FXML
    private TextField valorMensalField;
    @FXML
    private TextField diasAlertaField;
    @FXML
    private CheckBox alertaCheck;
    @FXML
    private TextArea observacoesArea;
    @FXML
    private Label feedbackLabel;

    public ControladorNovoContrato(
            CasoDeUsoContrato casoDeUsoContrato,
            ServicoConsultaReferencias servicoConsultaReferencias,
            GerenciadorNavegacao gerenciadorNavegacao
    ) {
        this.casoDeUsoContrato = casoDeUsoContrato;
        this.servicoConsultaReferencias = servicoConsultaReferencias;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
    }

    @FXML
    public void initialize() {
        LocalDate hoje = LocalDate.now();
        UtilComboBox.preencher(clienteCombo, servicoConsultaReferencias.clientes(), false);
        if (inicioPicker != null) {
            inicioPicker.setValue(hoje);
        }
        if (fimPicker != null) {
            fimPicker.setValue(hoje.plusYears(1));
        }
        if (tipoCombo != null) {
            tipoCombo.getItems().setAll(Contrato.ContratoType.values());
            tipoCombo.getSelectionModel().select(Contrato.ContratoType.MONTHLY);
        }
        if (frequenciaCombo != null) {
            frequenciaCombo.getItems().setAll(Contrato.ServiceFrequency.values());
            frequenciaCombo.getSelectionModel().select(Contrato.ServiceFrequency.MONTHLY);
        }
        if (statusCombo != null) {
            statusCombo.getItems().setAll(Contrato.ContratoStatus.values());
            statusCombo.getSelectionModel().select(Contrato.ContratoStatus.ACTIVE);
        }
        if (valorMensalField != null) {
            valorMensalField.setText("0");
        }
        if (diasAlertaField != null) {
            diasAlertaField.setText("30");
        }
        if (alertaCheck != null) {
            alertaCheck.setSelected(true);
        }
        setFeedback("");
    }

    @FXML
    private void onConfirmar() {
        try {
            OpcaoId cliente = UtilComboBox.obrigatorio(clienteCombo, "Selecione um cliente.");
            casoDeUsoContrato.create(new CasoDeUsoContrato.CreateContratoCommand(
                    UUID.randomUUID().toString(),
                    cliente.id(),
                    descricaoField == null ? "" : descricaoField.getText(),
                    inicioPicker == null || inicioPicker.getValue() == null ? LocalDate.now() : inicioPicker.getValue(),
                    fimPicker == null || fimPicker.getValue() == null ? LocalDate.now().plusYears(1) : fimPicker.getValue(),
                    tipoCombo == null || tipoCombo.getValue() == null ? Contrato.ContratoType.MONTHLY : tipoCombo.getValue(),
                    frequenciaCombo == null || frequenciaCombo.getValue() == null ? Contrato.ServiceFrequency.MONTHLY : frequenciaCombo.getValue(),
                    statusCombo == null || statusCombo.getValue() == null ? Contrato.ContratoStatus.ACTIVE : statusCombo.getValue(),
                    Contrato.RenewalRule.MANUAL,
                    parseMoney(valorMensalField == null ? "" : valorMensalField.getText()),
                    alertaCheck == null || alertaCheck.isSelected(),
                    parseInt(diasAlertaField == null ? "" : diasAlertaField.getText(), 30),
                    observacoesArea == null ? "" : observacoesArea.getText()
            ));
            gerenciadorNavegacao.navigateTo(VisaoAplicacao.CONTRACTS);
            UtilJanela.fecharJanela(clienteCombo);
        } catch (Exception exception) {
            setFeedback(exception.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        UtilJanela.fecharJanela(clienteCombo);
    }

    private BigDecimal parseMoney(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.trim().replace(",", "."));
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

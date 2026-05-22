package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.potenciaisclientes.porta.entrada.CasoDeUsoPotencialCliente;
import br.com.sigla.dominio.potenciaisclientes.PotencialCliente;
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
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ControladorNovaIndicacao {

    private final CasoDeUsoPotencialCliente casoDeUsoPotencialCliente;
    private final ServicoConsultaReferencias servicoConsultaReferencias;
    private final GerenciadorNavegacao gerenciadorNavegacao;

    @FXML
    private TextField nomeField;
    @FXML
    private TextField telefoneField;
    @FXML
    private ComboBox<OpcaoId> clienteCombo;
    @FXML
    private DatePicker dataPicker;
    @FXML
    private ComboBox<PotencialCliente.PotencialClienteStatus> statusCombo;
    @FXML
    private Label feedbackLabel;

    public ControladorNovaIndicacao(
            CasoDeUsoPotencialCliente casoDeUsoPotencialCliente,
            ServicoConsultaReferencias servicoConsultaReferencias,
            GerenciadorNavegacao gerenciadorNavegacao
    ) {
        this.casoDeUsoPotencialCliente = casoDeUsoPotencialCliente;
        this.servicoConsultaReferencias = servicoConsultaReferencias;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
    }

    @FXML
    public void initialize() {
        if (dataPicker != null) {
            dataPicker.setValue(LocalDate.now());
        }
        UtilComboBox.preencher(clienteCombo, servicoConsultaReferencias.clientes(), true);
        if (statusCombo != null) {
            statusCombo.getItems().setAll(PotencialCliente.PotencialClienteStatus.values());
            statusCombo.getSelectionModel().select(PotencialCliente.PotencialClienteStatus.NEW);
        }
        setFeedback("");
    }

    @FXML
    private void onConfirmar() {
        try {
            String customerId = UtilComboBox.idSelecionado(clienteCombo);
            casoDeUsoPotencialCliente.register(new CasoDeUsoPotencialCliente.RegisterPotencialClienteCommand(
                    "LEAD-" + System.currentTimeMillis(),
                    nomeField.getText(),
                    telefoneField.getText(),
                    "INDICACAO:" + customerId,
                    statusCombo == null || statusCombo.getValue() == null ? PotencialCliente.PotencialClienteStatus.NEW : statusCombo.getValue(),
                    dataPicker == null ? LocalDate.now() : dataPicker.getValue(),
                    "Indicacao",
                    ""
            ));
            gerenciadorNavegacao.navigateTo(VisaoAplicacao.CUSTOMERS);
            UtilJanela.fecharJanela(nomeField);
        } catch (IllegalArgumentException exception) {
            setFeedback(exception.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        UtilJanela.fecharJanela(nomeField);
    }

    private void setFeedback(String message) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message == null ? "" : message);
        }
    }
}

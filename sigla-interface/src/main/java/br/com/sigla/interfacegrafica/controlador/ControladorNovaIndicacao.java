package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.potenciaisclientes.porta.entrada.CasoDeUsoPotencialCliente;
import br.com.sigla.dominio.potenciaisclientes.PotencialCliente;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaReferencias;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.UtilJanela;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static br.com.sigla.interfacegrafica.util.ResolvedorEntradaTexto.parseEnum;
import static br.com.sigla.interfacegrafica.util.ResolvedorEntradaTexto.resolveOpcional;

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
    private TextField clienteField;
    @FXML
    private DatePicker dataPicker;
    @FXML
    private TextField statusField;
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
        if (statusField != null && statusField.getText().isBlank()) {
            statusField.setText(PotencialCliente.PotencialClienteStatus.NEW.name());
        }
        setFeedback("");
    }

    @FXML
    private void onConfirmar() {
        try {
            var cliente = resolveOpcional(servicoConsultaReferencias.clientes(), clienteField == null ? "" : clienteField.getText());
            String customerId = cliente == null ? "" : cliente.id();
            casoDeUsoPotencialCliente.register(new CasoDeUsoPotencialCliente.RegisterPotencialClienteCommand(
                    "LEAD-" + System.currentTimeMillis(),
                    nomeField.getText(),
                    telefoneField.getText(),
                    "INDICACAO:" + customerId,
                    parseEnum(PotencialCliente.PotencialClienteStatus.class, statusField == null ? "" : statusField.getText(), PotencialCliente.PotencialClienteStatus.NEW),
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

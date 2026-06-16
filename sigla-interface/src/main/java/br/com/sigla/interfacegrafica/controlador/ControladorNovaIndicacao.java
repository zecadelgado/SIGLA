package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.potenciaisclientes.porta.entrada.CasoDeUsoPotencialCliente;
import br.com.sigla.dominio.potenciaisclientes.PotencialCliente;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaReferencias;
import br.com.sigla.interfacegrafica.formatador.FormatadorMascaraCpf;
import br.com.sigla.interfacegrafica.modelo.OpcaoId;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.TradutorInterface;
import br.com.sigla.interfacegrafica.util.UtilComboBox;
import br.com.sigla.interfacegrafica.util.UtilJanela;
import br.com.sigla.interfacegrafica.util.ValidadorEntrada;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class ControladorNovaIndicacao {

    private final CasoDeUsoPotencialCliente casoDeUsoPotencialCliente;
    private final ServicoConsultaReferencias servicoConsultaReferencias;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final FormatadorMascaraCpf formatadorMascaraCpf;

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
    private TextArea observacoesArea;
    @FXML
    private Label feedbackLabel;

    public ControladorNovaIndicacao(
            CasoDeUsoPotencialCliente casoDeUsoPotencialCliente,
            ServicoConsultaReferencias servicoConsultaReferencias,
            GerenciadorNavegacao gerenciadorNavegacao,
            FormatadorMascaraCpf formatadorMascaraCpf
    ) {
        this.casoDeUsoPotencialCliente = casoDeUsoPotencialCliente;
        this.servicoConsultaReferencias = servicoConsultaReferencias;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
        this.formatadorMascaraCpf = formatadorMascaraCpf;
    }

    @FXML
    public void initialize() {
        if (dataPicker != null) {
            dataPicker.setValue(LocalDate.now());
        }
        if (statusCombo != null) {
            TradutorInterface.aplicar(statusCombo);
            statusCombo.getItems().setAll(
                    PotencialCliente.PotencialClienteStatus.NOVO,
                    PotencialCliente.PotencialClienteStatus.CONTATADO,
                    PotencialCliente.PotencialClienteStatus.AGUARDANDO_RETORNO,
                    PotencialCliente.PotencialClienteStatus.CONVERTIDO,
                    PotencialCliente.PotencialClienteStatus.PERDIDO,
                    PotencialCliente.PotencialClienteStatus.CANCELADO
            );
            statusCombo.getSelectionModel().select(PotencialCliente.PotencialClienteStatus.NOVO);
        }
        UtilComboBox.preencher(clienteCombo, servicoConsultaReferencias.clientes(), true);
        formatadorMascaraCpf.aplicarTelefone(telefoneField);
        setFeedback("");
    }

    @FXML
    private void onConfirmar() {
        try {
            ValidadorEntrada validador = ValidadorEntrada.nova();
            String nome = validador.texto(texto(nomeField), "o nome do indicado");
            String telefone = validador.texto(texto(telefoneField), "o telefone do indicado");
            validador.validar();

            var cliente = UtilComboBox.selecionado(clienteCombo);
            String customerId = cliente == null ? "" : cliente.id();
            casoDeUsoPotencialCliente.register(new CasoDeUsoPotencialCliente.RegisterPotencialClienteCommand(
                    UUID.randomUUID().toString(),
                    nome,
                    telefone,
                    "INDICACAO:" + customerId,
                    customerId,
                    statusCombo == null || statusCombo.getValue() == null ? PotencialCliente.PotencialClienteStatus.NOVO : statusCombo.getValue(),
                    dataPicker == null ? LocalDate.now() : dataPicker.getValue(),
                    "Indicacao",
                    observacoesArea == null ? "" : observacoesArea.getText()
            ));
            gerenciadorNavegacao.navigateTo(VisaoAplicacao.CUSTOMERS);
            UtilJanela.fecharJanela(nomeField);
        } catch (IllegalArgumentException exception) {
            setFeedback(br.com.sigla.interfacegrafica.util.MensagensErro.descrever(exception));
        }
    }

    @FXML
    private void onCancelar() {
        UtilJanela.fecharJanela(nomeField);
    }

    private String texto(TextField campo) {
        return campo == null || campo.getText() == null ? "" : campo.getText();
    }

    private void setFeedback(String message) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message == null ? "" : message);
        }
    }
}

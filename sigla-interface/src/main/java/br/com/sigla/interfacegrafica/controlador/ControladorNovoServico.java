package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoServicoPrestado;
import br.com.sigla.dominio.servicos.ServicoPrestado;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaReferencias;
import br.com.sigla.interfacegrafica.modelo.OpcaoId;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static br.com.sigla.interfacegrafica.util.ResolvedorEntradaTexto.parseEnum;
import static br.com.sigla.interfacegrafica.util.ResolvedorEntradaTexto.resolveOpcional;

@Component
public class ControladorNovoServico {

    private final CasoDeUsoServicoPrestado casoDeUsoServicoPrestado;
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
    private DatePicker diaInteiroPicker;
    @FXML
    private TextField statusField;
    @FXML
    private TextField prioridadeField;
    @FXML
    private TextField responsavelField;
    @FXML
    private Label feedbackLabel;

    public ControladorNovoServico(
            CasoDeUsoServicoPrestado casoDeUsoServicoPrestado,
            ServicoConsultaReferencias servicoConsultaReferencias,
            GerenciadorNavegacao gerenciadorNavegacao
    ) {
        this.casoDeUsoServicoPrestado = casoDeUsoServicoPrestado;
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
            statusField.setText(ServicoPrestado.ServiceStatus.SCHEDULED.name());
        }
        if (prioridadeField != null && prioridadeField.getText().isBlank()) {
            prioridadeField.setText(ServicoPrestado.ServicePriority.NORMAL.name());
        }
        setFeedback("");
    }

    @FXML
    private void onConfirmar() {
        try {
            OpcaoId cliente = requiredOption(resolveOpcional(servicoConsultaReferencias.clientes(), clienteField == null ? "" : clienteField.getText()), "Selecione um cliente.");
            OpcaoId responsavel = requiredOption(resolveOpcional(servicoConsultaReferencias.funcionarios(), responsavelField == null ? "" : responsavelField.getText()), "Selecione um responsavel.");
            OpcaoId ordem = resolveOpcional(servicoConsultaReferencias.ordensServico(), ordemField == null ? "" : ordemField.getText());

            casoDeUsoServicoPrestado.register(new CasoDeUsoServicoPrestado.RegisterServicoPrestadoCommand(
                    "SRV-" + System.currentTimeMillis(),
                    cliente.id(),
                    "",
                    ordem == null ? "" : ordem.id(),
                    responsavel.id(),
                    dataInicioPicker == null ? LocalDate.now() : dataInicioPicker.getValue(),
                    mergeDescricao(),
                    BigDecimal.ZERO,
                    ServicoPrestado.PaymentStatus.PENDING,
                    ServicoPrestado.SignatureType.NONE,
                    null,
                    null,
                    List.of(),
                    parseEnum(ServicoPrestado.ServiceStatus.class, statusField == null ? "" : statusField.getText(), ServicoPrestado.ServiceStatus.SCHEDULED),
                    parseEnum(ServicoPrestado.ServicePriority.class, prioridadeField == null ? "" : prioridadeField.getText(), ServicoPrestado.ServicePriority.NORMAL),
                    ""
            ));
            gerenciadorNavegacao.navigateTo(VisaoAplicacao.SERVICES);
        } catch (Exception exception) {
            setFeedback(exception.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.SERVICES);
    }

    private String mergeDescricao() {
        String descricao = descricaoField == null ? "" : descricaoField.getText().trim();
        if (descricao.isBlank()) {
            throw new IllegalArgumentException("Descreva o servico prestado.");
        }
        String titulo = tituloField == null ? "" : tituloField.getText().trim();
        String texto = titulo.isBlank() ? descricao : titulo + " - " + descricao;
        return diaInteiroPicker != null && diaInteiroPicker.getValue() != null ? texto + " | Dia inteiro" : texto;
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

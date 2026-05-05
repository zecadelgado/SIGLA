package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.interfacegrafica.aplicativo.SessaoLocalAplicacao;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaReferencias;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.UtilJanela;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static br.com.sigla.interfacegrafica.util.ResolvedorEntradaTexto.parseBoolean;
import static br.com.sigla.interfacegrafica.util.ResolvedorEntradaTexto.parseEnum;
import static br.com.sigla.interfacegrafica.util.ResolvedorEntradaTexto.resolveOpcional;

@Component
public class ControladorNovaTransacao {

    private final CasoDeUsoFinanceiro casoDeUsoFinanceiro;
    private final ServicoConsultaReferencias servicoConsultaReferencias;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final SessaoLocalAplicacao sessaoLocalAplicacao;

    @FXML
    private ComboBox<CasoDeUsoFinanceiro.TransactionType> tipoCombo;
    @FXML
    private TextField categoriaField;
    @FXML
    private TextField descricaoField;
    @FXML
    private TextField clienteField;
    @FXML
    private TextField valorField;
    @FXML
    private TextField ordemField;
    @FXML
    private DatePicker emissaoPicker;
    @FXML
    private DatePicker vencimentoPicker;
    @FXML
    private DatePicker pagamentoPicker;
    @FXML
    private TextField formaPagamentoField;
    @FXML
    private TextField parceladoField;
    @FXML
    private TextField parcelasField;
    @FXML
    private TextField observacoesField;
    @FXML
    private TextField criadoPorField;
    @FXML
    private TextField statusField;
    @FXML
    private Label feedbackLabel;

    public ControladorNovaTransacao(
            CasoDeUsoFinanceiro casoDeUsoFinanceiro,
            ServicoConsultaReferencias servicoConsultaReferencias,
            GerenciadorNavegacao gerenciadorNavegacao,
            SessaoLocalAplicacao sessaoLocalAplicacao
    ) {
        this.casoDeUsoFinanceiro = casoDeUsoFinanceiro;
        this.servicoConsultaReferencias = servicoConsultaReferencias;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
        this.sessaoLocalAplicacao = sessaoLocalAplicacao;
    }

    @FXML
    public void initialize() {
        if (tipoCombo != null) {
            tipoCombo.getItems().setAll(CasoDeUsoFinanceiro.TransactionType.values());
            tipoCombo.getSelectionModel().select(CasoDeUsoFinanceiro.TransactionType.ENTRY);
        }
        if (emissaoPicker != null) {
            emissaoPicker.setValue(LocalDate.now());
        }
        if (statusField != null && statusField.getText().isBlank()) {
            statusField.setText(CasoDeUsoFinanceiro.TransactionStatus.PENDING.name());
        }
        if (criadoPorField != null && criadoPorField.getText().isBlank() && sessaoLocalAplicacao.usuarioAtual() != null) {
            criadoPorField.setText(sessaoLocalAplicacao.usuarioAtual().id());
        }
        setFeedback("");
    }

    @FXML
    private void onAdicionar() {
        try {
            var cliente = resolveOpcional(servicoConsultaReferencias.clientes(), clienteField == null ? "" : clienteField.getText());
            var ordem = resolveOpcional(servicoConsultaReferencias.ordensServico(), ordemField == null ? "" : ordemField.getText());
            casoDeUsoFinanceiro.registerTransaction(new CasoDeUsoFinanceiro.RegisterTransacaoFinanceiraCommand(
                    UUID.randomUUID().toString(),
                    tipoCombo == null || tipoCombo.getValue() == null ? CasoDeUsoFinanceiro.TransactionType.ENTRY : tipoCombo.getValue(),
                    categoriaField.getText(),
                    descricaoField.getText(),
                    cliente == null ? "" : cliente.id(),
                    "",
                    ordem == null ? "" : ordem.id(),
                    new BigDecimal(valorField.getText()),
                    emissaoPicker == null ? LocalDate.now() : emissaoPicker.getValue(),
                    vencimentoPicker == null ? null : vencimentoPicker.getValue(),
                    pagamentoPicker == null ? null : pagamentoPicker.getValue(),
                    formaPagamentoField.getText(),
                    parseBoolean(parceladoField == null ? "" : parceladoField.getText()),
                    parcelasField.getText().isBlank() ? 1 : Integer.parseInt(parcelasField.getText()),
                    resolveUsuarioAtual(),
                    observacoesField == null ? "" : observacoesField.getText(),
                    parseEnum(CasoDeUsoFinanceiro.TransactionStatus.class, statusField == null ? "" : statusField.getText(), CasoDeUsoFinanceiro.TransactionStatus.PENDING)
            ));
            gerenciadorNavegacao.navigateTo(VisaoAplicacao.FINANCE);
            UtilJanela.fecharJanela(tipoCombo);
        } catch (Exception exception) {
            setFeedback(exception.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        UtilJanela.fecharJanela(tipoCombo);
    }

    private void setFeedback(String message) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message == null ? "" : message);
        }
    }

    private String resolveUsuarioAtual() {
        String digitado = criadoPorField == null ? "" : criadoPorField.getText();
        if (digitado != null && !digitado.isBlank()) {
            return digitado.trim();
        }
        return sessaoLocalAplicacao.usuarioAtual() == null ? "" : sessaoLocalAplicacao.usuarioAtual().id();
    }
}

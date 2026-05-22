package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class ControladorNovaTransacao {

    private final CasoDeUsoFinanceiro casoDeUsoFinanceiro;
    private final ServicoConsultaReferencias servicoConsultaReferencias;
    private final GerenciadorNavegacao gerenciadorNavegacao;

    @FXML
    private ComboBox<CasoDeUsoFinanceiro.TransactionType> tipoCombo;
    @FXML
    private ComboBox<OpcaoId> categoriaCombo;
    @FXML
    private TextField descricaoField;
    @FXML
    private ComboBox<OpcaoId> clienteCombo;
    @FXML
    private TextField valorField;
    @FXML
    private ComboBox<OpcaoId> ordemCombo;
    @FXML
    private DatePicker emissaoPicker;
    @FXML
    private DatePicker vencimentoPicker;
    @FXML
    private DatePicker pagamentoPicker;
    @FXML
    private ComboBox<OpcaoId> formaPagamentoCombo;
    @FXML
    private ComboBox<Boolean> parceladoCombo;
    @FXML
    private TextField parcelasField;
    @FXML
    private TextField observacoesField;
    @FXML
    private TextField criadoPorField;
    @FXML
    private ComboBox<CasoDeUsoFinanceiro.TransactionStatus> statusCombo;
    @FXML
    private Label feedbackLabel;

    public ControladorNovaTransacao(
            CasoDeUsoFinanceiro casoDeUsoFinanceiro,
            ServicoConsultaReferencias servicoConsultaReferencias,
            GerenciadorNavegacao gerenciadorNavegacao
    ) {
        this.casoDeUsoFinanceiro = casoDeUsoFinanceiro;
        this.servicoConsultaReferencias = servicoConsultaReferencias;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
    }

    @FXML
    public void initialize() {
        if (tipoCombo != null) {
            tipoCombo.getItems().setAll(CasoDeUsoFinanceiro.TransactionType.values());
            tipoCombo.getSelectionModel().select(CasoDeUsoFinanceiro.TransactionType.ENTRY);
            tipoCombo.valueProperty().addListener((observable, oldValue, newValue) -> atualizarCategorias());
        }
        UtilComboBox.preencher(clienteCombo, servicoConsultaReferencias.clientes(), true);
        UtilComboBox.preencher(ordemCombo, servicoConsultaReferencias.ordensServico(), true);
        UtilComboBox.preencher(formaPagamentoCombo, servicoConsultaReferencias.formasPagamento(), false);
        atualizarCategorias();
        if (clienteCombo != null) {
            clienteCombo.valueProperty().addListener((observable, oldValue, newValue) -> atualizarOrdensPorCliente());
        }
        if (ordemCombo != null) {
            ordemCombo.valueProperty().addListener((observable, oldValue, newValue) -> sincronizarClientePorOrdem());
        }
        if (parceladoCombo != null) {
            parceladoCombo.getItems().setAll(false, true);
            parceladoCombo.getSelectionModel().select(Boolean.FALSE);
        }
        if (emissaoPicker != null) {
            emissaoPicker.setValue(LocalDate.now());
        }
        if (statusCombo != null) {
            statusCombo.getItems().setAll(CasoDeUsoFinanceiro.TransactionStatus.values());
            statusCombo.getSelectionModel().select(CasoDeUsoFinanceiro.TransactionStatus.PENDING);
        }
        setFeedback("");
    }

    @FXML
    private void onAdicionar() {
        try {
            String clienteId = UtilComboBox.idSelecionado(clienteCombo);
            String ordemId = UtilComboBox.idSelecionado(ordemCombo);
            OpcaoId categoria = UtilComboBox.obrigatorio(categoriaCombo, "Selecione uma categoria.");
            OpcaoId formaPagamento = UtilComboBox.obrigatorio(formaPagamentoCombo, "Selecione uma forma de pagamento.");
            casoDeUsoFinanceiro.registerTransaction(new CasoDeUsoFinanceiro.RegisterTransacaoFinanceiraCommand(
                    UUID.randomUUID().toString(),
                    tipoCombo == null || tipoCombo.getValue() == null ? CasoDeUsoFinanceiro.TransactionType.ENTRY : tipoCombo.getValue(),
                    categoria.label(),
                    categoria.id(),
                    descricaoField.getText(),
                    clienteId,
                    "",
                    ordemId,
                    new BigDecimal(valorField.getText()),
                    emissaoPicker == null ? LocalDate.now() : emissaoPicker.getValue(),
                    vencimentoPicker == null ? null : vencimentoPicker.getValue(),
                    pagamentoPicker == null ? null : pagamentoPicker.getValue(),
                    formaPagamento.label(),
                    formaPagamento.id(),
                    parceladoCombo != null && Boolean.TRUE.equals(parceladoCombo.getValue()),
                    parcelasField.getText().isBlank() ? 1 : Integer.parseInt(parcelasField.getText()),
                    criadoPorField == null || criadoPorField.getText().isBlank() ? "Sistema" : criadoPorField.getText(),
                    observacoesField == null ? "" : observacoesField.getText(),
                    statusCombo == null || statusCombo.getValue() == null ? CasoDeUsoFinanceiro.TransactionStatus.PENDING : statusCombo.getValue()
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

    private void atualizarCategorias() {
        CasoDeUsoFinanceiro.TransactionType tipo = tipoCombo == null || tipoCombo.getValue() == null
                ? CasoDeUsoFinanceiro.TransactionType.ENTRY
                : tipoCombo.getValue();
        UtilComboBox.preencher(categoriaCombo, servicoConsultaReferencias.categoriasFinanceiras(tipo), false);
    }

    private void atualizarOrdensPorCliente() {
        String clienteId = UtilComboBox.idSelecionado(clienteCombo);
        UtilComboBox.preencher(ordemCombo, clienteId.isBlank()
                ? servicoConsultaReferencias.ordensServico()
                : servicoConsultaReferencias.ordensServicoPorCliente(clienteId), true);
    }

    private void sincronizarClientePorOrdem() {
        String ordemId = UtilComboBox.idSelecionado(ordemCombo);
        if (!ordemId.isBlank()) {
            UtilComboBox.selecionarPorId(clienteCombo, servicoConsultaReferencias.clienteIdPorOrdem(ordemId));
            UtilComboBox.selecionarPorId(ordemCombo, ordemId);
        }
    }
}

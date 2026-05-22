package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.dominio.estoque.ItemEstoque;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaReferencias;
import br.com.sigla.interfacegrafica.modelo.OpcaoId;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.UtilComboBox;
import br.com.sigla.interfacegrafica.util.UtilJanela;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class ControladorNovaMovimentacao {

    private final CasoDeUsoEstoque casoDeUsoEstoque;
    private final ServicoConsultaReferencias servicoConsultaReferencias;
    private final GerenciadorNavegacao gerenciadorNavegacao;

    @FXML
    private ComboBox<OpcaoId> produtoCombo;
    @FXML
    private ComboBox<ItemEstoque.MovementType> tipoCombo;
    @FXML
    private TextField quantidadeField;
    @FXML
    private TextField valorUnitarioField;
    @FXML
    private TextField valorTotalField;
    @FXML
    private TextField usuarioField;
    @FXML
    private ComboBox<OpcaoId> clienteCombo;
    @FXML
    private ComboBox<OpcaoId> ordemCombo;
    @FXML
    private TextField destinoField;
    @FXML
    private TextField observacoesField;
    @FXML
    private Label feedbackLabel;

    public ControladorNovaMovimentacao(
            CasoDeUsoEstoque casoDeUsoEstoque,
            ServicoConsultaReferencias servicoConsultaReferencias,
            GerenciadorNavegacao gerenciadorNavegacao
    ) {
        this.casoDeUsoEstoque = casoDeUsoEstoque;
        this.servicoConsultaReferencias = servicoConsultaReferencias;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
    }

    @FXML
    public void initialize() {
        UtilComboBox.preencher(produtoCombo, servicoConsultaReferencias.produtos(), false);
        UtilComboBox.preencher(clienteCombo, servicoConsultaReferencias.clientes(), true);
        UtilComboBox.preencher(ordemCombo, servicoConsultaReferencias.ordensServico(), true);
        if (tipoCombo != null) {
            tipoCombo.getItems().setAll(ItemEstoque.MovementType.values());
            tipoCombo.getSelectionModel().select(ItemEstoque.MovementType.OUTBOUND);
        }
        if (clienteCombo != null) {
            clienteCombo.valueProperty().addListener((observable, oldValue, newValue) -> atualizarOrdensPorCliente());
        }
        quantidadeField.textProperty().addListener((observable, oldValue, newValue) -> recomputeTotal());
        valorUnitarioField.textProperty().addListener((observable, oldValue, newValue) -> recomputeTotal());
        setFeedback("");
    }

    @FXML
    private void onConfirmarMovimentacao() {
        try {
            var produto = UtilComboBox.obrigatorio(produtoCombo, "Selecione um produto valido.");
            String clienteId = UtilComboBox.idSelecionado(clienteCombo);
            String ordemId = UtilComboBox.idSelecionado(ordemCombo);

            casoDeUsoEstoque.recordMovement(new CasoDeUsoEstoque.RecordInventoryMovementCommand(
                    produto.id(),
                    UUID.randomUUID().toString(),
                    tipoCombo == null || tipoCombo.getValue() == null ? ItemEstoque.MovementType.OUTBOUND : tipoCombo.getValue(),
                    Integer.parseInt(quantidadeField.getText()),
                    LocalDate.now(),
                    new BigDecimal(valorUnitarioField.getText()),
                    new BigDecimal(valorTotalField.getText()),
                    usuarioField == null || usuarioField.getText().isBlank() ? "Sistema" : usuarioField.getText(),
                    clienteId,
                    ordemId,
                    destinoField.getText(),
                    observacoesField == null ? "" : observacoesField.getText()
            ));
            gerenciadorNavegacao.navigateTo(VisaoAplicacao.INVENTORY);
            UtilJanela.fecharJanela(produtoCombo);
        } catch (Exception exception) {
            setFeedback(exception.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        UtilJanela.fecharJanela(produtoCombo);
    }

    private void atualizarOrdensPorCliente() {
        String clienteId = UtilComboBox.idSelecionado(clienteCombo);
        UtilComboBox.preencher(ordemCombo, clienteId.isBlank()
                ? servicoConsultaReferencias.ordensServico()
                : servicoConsultaReferencias.ordensServicoPorCliente(clienteId), true);
    }

    private void recomputeTotal() {
        try {
            BigDecimal quantidade = new BigDecimal(quantidadeField.getText());
            BigDecimal valorUnitario = new BigDecimal(valorUnitarioField.getText());
            valorTotalField.setText(quantidade.multiply(valorUnitario).toPlainString());
        } catch (Exception ignored) {
            // campos incompletos
        }
    }

    private void setFeedback(String message) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message == null ? "" : message);
        }
    }
}

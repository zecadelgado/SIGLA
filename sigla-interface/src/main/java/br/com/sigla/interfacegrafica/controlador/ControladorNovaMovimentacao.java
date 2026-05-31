package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.dominio.estoque.ItemEstoque;
import br.com.sigla.interfacegrafica.aplicativo.SessaoLocalAplicacao;
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
    private final SessaoLocalAplicacao sessaoLocalAplicacao;

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
            GerenciadorNavegacao gerenciadorNavegacao,
            SessaoLocalAplicacao sessaoLocalAplicacao
    ) {
        this.casoDeUsoEstoque = casoDeUsoEstoque;
        this.servicoConsultaReferencias = servicoConsultaReferencias;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
        this.sessaoLocalAplicacao = sessaoLocalAplicacao;
    }

    @FXML
    public void initialize() {
        UtilComboBox.preencher(produtoCombo, servicoConsultaReferencias.produtos(), false);
        UtilComboBox.preencher(clienteCombo, servicoConsultaReferencias.clientes(), true);
        UtilComboBox.preencher(ordemCombo, servicoConsultaReferencias.ordensServico(), true);
        if (clienteCombo != null) {
            clienteCombo.valueProperty().addListener((observable, oldValue, newValue) ->
                    UtilComboBox.preencher(ordemCombo, servicoConsultaReferencias.ordensServicoDoCliente(UtilComboBox.idSelecionado(clienteCombo)), true)
            );
        }
        if (tipoCombo != null) {
            tipoCombo.getItems().setAll(ItemEstoque.MovementType.values());
            tipoCombo.getSelectionModel().select(ItemEstoque.MovementType.SAIDA);
        }
        if (usuarioField != null && usuarioField.getText().isBlank() && sessaoLocalAplicacao.usuarioAtual() != null) {
            usuarioField.setText(sessaoLocalAplicacao.usuarioAtual().id());
        }
        quantidadeField.textProperty().addListener((observable, oldValue, newValue) -> recomputeTotal());
        valorUnitarioField.textProperty().addListener((observable, oldValue, newValue) -> recomputeTotal());
        setFeedback("");
    }

    @FXML
    private void onConfirmarMovimentacao() {
        try {
            var produto = UtilComboBox.selecionado(produtoCombo);
            if (produto == null) {
                throw new IllegalArgumentException("Selecione um produto valido.");
            }
            var cliente = UtilComboBox.selecionado(clienteCombo);
            var ordem = UtilComboBox.selecionado(ordemCombo);
            ItemEstoque.MovementType tipo = tipoCombo == null || tipoCombo.getValue() == null ? ItemEstoque.MovementType.SAIDA : tipoCombo.getValue();

            casoDeUsoEstoque.recordMovement(new CasoDeUsoEstoque.RecordInventoryMovementCommand(
                    produto.id(),
                    UUID.randomUUID().toString(),
                    tipo,
                    Integer.parseInt(quantidadeField.getText()),
                    LocalDate.now(),
                    new BigDecimal(valorUnitarioField.getText()),
                    BigDecimal.ZERO,
                    resolveUsuarioAtual(),
                    cliente == null ? "" : cliente.id(),
                    ordem == null ? "" : ordem.id(),
                    destinoField.getText(),
                    "",
                    usuarioField == null ? "" : usuarioField.getText(),
                    tipo == ItemEstoque.MovementType.COMPRA ? usuarioField.getText() : "",
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

    private String resolveUsuarioAtual() {
        String digitado = usuarioField == null ? "" : usuarioField.getText();
        if (digitado != null && !digitado.isBlank()) {
            return digitado.trim();
        }
        return sessaoLocalAplicacao.usuarioAtual() == null ? "" : sessaoLocalAplicacao.usuarioAtual().id();
    }
}

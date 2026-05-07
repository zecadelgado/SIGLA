package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.dominio.estoque.ItemEstoque;
import br.com.sigla.interfacegrafica.aplicativo.SessaoLocalAplicacao;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaReferencias;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.UtilJanela;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static br.com.sigla.interfacegrafica.util.ResolvedorEntradaTexto.parseEnum;
import static br.com.sigla.interfacegrafica.util.ResolvedorEntradaTexto.resolveOpcional;

@Component
public class ControladorNovaMovimentacao {

    private final CasoDeUsoEstoque casoDeUsoEstoque;
    private final ServicoConsultaReferencias servicoConsultaReferencias;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final SessaoLocalAplicacao sessaoLocalAplicacao;

    @FXML
    private TextField produtoField;
    @FXML
    private TextField tipoField;
    @FXML
    private TextField quantidadeField;
    @FXML
    private TextField valorUnitarioField;
    @FXML
    private TextField valorTotalField;
    @FXML
    private TextField usuarioField;
    @FXML
    private TextField clienteField;
    @FXML
    private TextField ordemField;
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
        if (tipoField != null && tipoField.getText().isBlank()) {
            tipoField.setText(ItemEstoque.MovementType.SAIDA.name());
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
            var produto = resolveOpcional(servicoConsultaReferencias.produtos(), produtoField == null ? "" : produtoField.getText());
            if (produto == null) {
                throw new IllegalArgumentException("Selecione um produto valido.");
            }
            var cliente = resolveOpcional(servicoConsultaReferencias.clientes(), clienteField == null ? "" : clienteField.getText());
            var ordem = resolveOpcional(servicoConsultaReferencias.ordensServico(), ordemField == null ? "" : ordemField.getText());

            casoDeUsoEstoque.recordMovement(new CasoDeUsoEstoque.RecordInventoryMovementCommand(
                    produto.id(),
                    UUID.randomUUID().toString(),
                    ItemEstoque.MovementType.from(tipoField == null ? "" : tipoField.getText()),
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
                    tipoField != null && ItemEstoque.MovementType.from(tipoField.getText()) == ItemEstoque.MovementType.COMPRA ? usuarioField.getText() : "",
                    observacoesField == null ? "" : observacoesField.getText()
            ));
            gerenciadorNavegacao.navigateTo(VisaoAplicacao.INVENTORY);
            UtilJanela.fecharJanela(produtoField);
        } catch (Exception exception) {
            setFeedback(exception.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        UtilJanela.fecharJanela(produtoField);
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

package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.UtilJanela;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class ControladorNovoProduto {

    private final CasoDeUsoEstoque casoDeUsoEstoque;
    private final GerenciadorNavegacao gerenciadorNavegacao;

    @FXML
    private TextField nomeField;
    @FXML
    private TextField descricaoField;
    @FXML
    private TextField valorCustoField;
    @FXML
    private TextField valorVendaField;
    @FXML
    private TextField quantidadeField;
    @FXML
    private TextField quantidadeMinimaField;
    @FXML
    private Label feedbackLabel;

    public ControladorNovoProduto(CasoDeUsoEstoque casoDeUsoEstoque, GerenciadorNavegacao gerenciadorNavegacao) {
        this.casoDeUsoEstoque = casoDeUsoEstoque;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
    }

    @FXML
    public void initialize() {
        setFeedback("");
    }

    @FXML
    private void onConfirmar() {
        try {
            casoDeUsoEstoque.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand(
                    UUID.randomUUID().toString(),
                    nomeField.getText(),
                    descricaoField.getText(),
                    new BigDecimal(valorCustoField.getText()),
                    new BigDecimal(valorVendaField.getText()),
                    Integer.parseInt(quantidadeField.getText()),
                    Integer.parseInt(quantidadeMinimaField.getText()),
                    "un"
            ));
            gerenciadorNavegacao.navigateTo(VisaoAplicacao.INVENTORY);
            UtilJanela.fecharJanela(nomeField);
        } catch (Exception exception) {
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

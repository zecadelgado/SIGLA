package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.interfacegrafica.formatador.FormatadorMascaraMoeda;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.UtilJanela;
import br.com.sigla.interfacegrafica.util.ValidadorEntrada;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ControladorNovoProduto {

    private final CasoDeUsoEstoque casoDeUsoEstoque;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final FormatadorMascaraMoeda formatadorMoeda;

    @FXML
    private TextField nomeField;
    @FXML
    private TextField descricaoField;
    @FXML
    private TextField skuField;
    @FXML
    private ComboBox<String> unidadeCombo;
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

    public ControladorNovoProduto(CasoDeUsoEstoque casoDeUsoEstoque, GerenciadorNavegacao gerenciadorNavegacao, FormatadorMascaraMoeda formatadorMoeda) {
        this.casoDeUsoEstoque = casoDeUsoEstoque;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
        this.formatadorMoeda = formatadorMoeda;
    }

    @FXML
    public void initialize() {
        if (unidadeCombo != null) {
            unidadeCombo.getItems().setAll("un", "litro", "kg", "caixa", "pacote", "frasco");
            unidadeCombo.getSelectionModel().select("un");
        }
        formatadorMoeda.aplicar(valorCustoField);
        formatadorMoeda.aplicar(valorVendaField);
        setFeedback("");
    }

    @FXML
    private void onConfirmar() {
        try {
            ValidadorEntrada validador = ValidadorEntrada.nova();
            String nome = validador.texto(texto(nomeField), "o nome do produto");
            int quantidade = validador.inteiroNaoNegativo(texto(quantidadeField), "a quantidade em estoque");
            int quantidadeMinima = validador.inteiroNaoNegativo(texto(quantidadeMinimaField), "a quantidade mínima");
            validador.validar();

            casoDeUsoEstoque.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand(
                    UUID.randomUUID().toString(),
                    nome,
                    texto(descricaoField),
                    skuField == null ? "" : skuField.getText(),
                    formatadorMoeda.valor(valorCustoField),
                    formatadorMoeda.valor(valorVendaField),
                    quantidade,
                    quantidadeMinima,
                    unidadeCombo == null || unidadeCombo.getValue() == null ? "un" : unidadeCombo.getValue(),
                    true
            ));
            gerenciadorNavegacao.navigateTo(VisaoAplicacao.INVENTORY);
            UtilJanela.fecharJanela(nomeField);
        } catch (Exception exception) {
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

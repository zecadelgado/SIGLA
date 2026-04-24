package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.funcionarios.porta.entrada.CasoDeUsoFuncionario;
import br.com.sigla.dominio.funcionarios.Funcionario;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.UtilJanela;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ControladorNovoCadastro {

    private final CasoDeUsoCliente casoDeUsoCliente;
    private final CasoDeUsoFuncionario casoDeUsoFuncionario;
    private final GerenciadorNavegacao gerenciadorNavegacao;

    @FXML
    private TextField nomeField;
    @FXML
    private TextField cpfField;
    @FXML
    private TextField razaoSocialField;
    @FXML
    private TextField cnpjField;
    @FXML
    private TextField telefoneField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField ruaField;
    @FXML
    private TextField numeroField;
    @FXML
    private TextField complementoField;
    @FXML
    private TextField bairroField;
    @FXML
    private TextField cidadeField;
    @FXML
    private TextField cepField;
    @FXML
    private TextField estadoField;
    @FXML
    private TextField observacoesField;
    @FXML
    private Label feedbackLabel;

    public ControladorNovoCadastro(
            CasoDeUsoCliente casoDeUsoCliente,
            CasoDeUsoFuncionario casoDeUsoFuncionario,
            GerenciadorNavegacao gerenciadorNavegacao
    ) {
        this.casoDeUsoCliente = casoDeUsoCliente;
        this.casoDeUsoFuncionario = casoDeUsoFuncionario;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
    }

    @FXML
    public void initialize() {
        setFeedback("");
    }

    @FXML
    private void onCadastrar() {
        try {
            if (isCadastroFuncionario()) {
                cadastrarFuncionario();
            } else {
                cadastrarCliente();
            }
            gerenciadorNavegacao.navigateTo(VisaoAplicacao.REGISTRY);
            UtilJanela.fecharJanela(nomeField);
        } catch (IllegalArgumentException exception) {
            setFeedback(exception.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        UtilJanela.fecharJanela(nomeField);
    }

    private void cadastrarCliente() {
        String id = "CUS-" + System.currentTimeMillis();
        String nomeCliente = !razaoSocialField.getText().isBlank() ? razaoSocialField.getText() : nomeField.getText();
        casoDeUsoCliente.register(new CasoDeUsoCliente.RegisterClienteCommand(
                id,
                nomeCliente,
                montarLocalizacao(),
                cnpjField.getText(),
                telefoneField.getText(),
                List.of(new CasoDeUsoCliente.ContactCommand(
                        nomeField.getText().isBlank() ? nomeCliente : nomeField.getText(),
                        emailField.getText().isBlank() ? "Contato principal" : emailField.getText(),
                        telefoneField.getText()
                )),
                observacoesField == null ? "" : observacoesField.getText()
        ));
    }

    private void cadastrarFuncionario() {
        String nome = nomeField.getText() == null ? "" : nomeField.getText().trim();
        if (nome.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do funcionario.");
        }
        String id = "EMP-" + System.currentTimeMillis();
        casoDeUsoFuncionario.register(new CasoDeUsoFuncionario.RegisterFuncionarioCommand(
                id,
                nome,
                "Equipe",
                telefoneField.getText().isBlank() ? emailField.getText() : telefoneField.getText(),
                Funcionario.FuncionarioStatus.ACTIVE
        ));
    }

    private String montarLocalizacao() {
        return String.join(" - ", List.of(
                ruaField.getText(),
                numeroField.getText(),
                complementoField.getText(),
                bairroField.getText(),
                cidadeField.getText(),
                estadoField.getText(),
                cepField.getText()
        ).stream().filter(value -> value != null && !value.isBlank()).toList());
    }

    private boolean isCadastroFuncionario() {
        return (razaoSocialField.getText() == null || razaoSocialField.getText().isBlank())
                && (cnpjField.getText() == null || cnpjField.getText().isBlank());
    }

    private void setFeedback(String message) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message == null ? "" : message);
        }
    }
}

package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.funcionarios.porta.entrada.CasoDeUsoFuncionario;
import br.com.sigla.dominio.funcionarios.Funcionario;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.UtilJanela;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

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
    private ComboBox<String> tipoCombo;
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
        if (tipoCombo != null) {
            tipoCombo.getItems().setAll("CLIENTE", "FUNCIONARIO");
            tipoCombo.getSelectionModel().select("CLIENTE");
        }
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
        String id = UUID.randomUUID().toString();
        String nomeCliente = !razaoSocialField.getText().isBlank() ? razaoSocialField.getText() : nomeField.getText();
        casoDeUsoCliente.register(new CasoDeUsoCliente.RegisterClienteCommand(
                id,
                nomeCliente,
                razaoSocialField.getText(),
                nomeField.getText(),
                cpfField.getText(),
                cnpjField.getText(),
                telefoneField.getText(),
                emailField.getText(),
                cepField.getText(),
                ruaField.getText(),
                numeroField.getText(),
                complementoField.getText(),
                bairroField.getText(),
                cidadeField.getText(),
                estadoField.getText(),
                List.of(new CasoDeUsoCliente.ContactCommand(
                        nomeField.getText().isBlank() ? nomeCliente : nomeField.getText(),
                        "Contato principal",
                        telefoneField.getText()
                )),
                observacoesField == null ? "" : observacoesField.getText(),
                true
        ));
    }

    private void cadastrarFuncionario() {
        String nome = nomeField.getText() == null ? "" : nomeField.getText().trim();
        if (nome.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do funcionario.");
        }
        String contato = telefoneField.getText().isBlank() ? emailField.getText() : telefoneField.getText();
        if (contato == null || contato.isBlank()) {
            throw new IllegalArgumentException("Informe telefone ou e-mail do funcionario.");
        }
        String id = UUID.randomUUID().toString();
        casoDeUsoFuncionario.register(new CasoDeUsoFuncionario.RegisterFuncionarioCommand(
                id,
                nome,
                "Equipe",
                contato,
                Funcionario.FuncionarioStatus.ACTIVE
        ));
    }

    private boolean isCadastroFuncionario() {
        return tipoCombo != null && "FUNCIONARIO".equals(tipoCombo.getValue());
    }

    private void setFeedback(String message) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message == null ? "" : message);
        }
    }
}

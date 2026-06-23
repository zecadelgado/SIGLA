package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.usuarios.porta.entrada.CasoDeUsoUsuario;
import br.com.sigla.dominio.usuarios.Usuario;
import br.com.sigla.interfacegrafica.aplicativo.FluxoAplicacao;
import br.com.sigla.interfacegrafica.async.ExecutorTarefasUi;
import br.com.sigla.interfacegrafica.util.MensagensErro;
import br.com.sigla.interfacegrafica.util.ValidadorEntrada;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ControladorCadastroUsuario {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControladorCadastroUsuario.class);

    private final CasoDeUsoUsuario casoDeUsoUsuario;
    private final FluxoAplicacao fluxoAplicacao;
    private final ExecutorTarefasUi executorTarefasUi;

    @FXML
    private TextField nomeField;

    @FXML
    private TextField usuarioField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField senhaField;

    @FXML
    private PasswordField confirmacaoSenhaField;

    @FXML
    private Label feedbackLabel;

    @FXML
    private Button cadastrarButton;

    @FXML
    private Button voltarLoginButton;

    public ControladorCadastroUsuario(
            CasoDeUsoUsuario casoDeUsoUsuario,
            FluxoAplicacao fluxoAplicacao,
            ExecutorTarefasUi executorTarefasUi
    ) {
        this.casoDeUsoUsuario = casoDeUsoUsuario;
        this.fluxoAplicacao = fluxoAplicacao;
        this.executorTarefasUi = executorTarefasUi;
    }

    @FXML
    public void initialize() {
        setFeedback("");
    }

    @FXML
    private void onCadastrar() {
        CasoDeUsoUsuario.RegistrarUsuarioCommand command;
        try {
            ValidadorEntrada validador = ValidadorEntrada.nova();
            String nome = validador.texto(textValue(nomeField), "o nome");
            String usuario = validador.texto(textValue(usuarioField), "o usuario");
            String email = validador.texto(textValue(emailField), "o e-mail");
            String senha = validador.texto(textValue(senhaField), "a senha");
            String confirmacao = validador.texto(textValue(confirmacaoSenhaField), "a confirmacao da senha");
            validador.exigir(email.isBlank() || email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"),
                    "Informe um e-mail valido.");
            validador.exigir(senha.equals(confirmacao), "A confirmacao da senha deve ser igual a senha.");
            validador.validar();

            command = new CasoDeUsoUsuario.RegistrarUsuarioCommand(
                    UUID.randomUUID().toString(),
                    nome,
                    usuario,
                    email,
                    senha,
                    Usuario.TipoUsuario.OPERADOR,
                    true
            );
        } catch (IllegalArgumentException exception) {
            setFeedback(MensagensErro.descrever(exception), true);
            return;
        }

        setFeedback("Criando sua conta...", false);
        mostrarCarregando(true);
        executorTarefasUi.executar(
                () -> {
                    casoDeUsoUsuario.registrar(command);
                    return null;
                },
                ignored -> {
                    mostrarCarregando(false);
                    br.com.sigla.interfacegrafica.util.DialogoUi.sucesso(
                            "Conta criada com sucesso. Entre com seu e-mail e senha.");
                    fluxoAplicacao.showLogin();
                },
                erro -> {
                    LOGGER.error("Falha ao cadastrar usuario pelo fluxo publico.", erro);
                    mostrarCarregando(false);
                    setFeedback(MensagensErro.descrever(erro), true);
                }
        );
    }

    @FXML
    private void onVoltarLogin() {
        fluxoAplicacao.showLogin();
    }

    private String textValue(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    private void mostrarCarregando(boolean carregando) {
        setDisabled(nomeField, carregando);
        setDisabled(usuarioField, carregando);
        setDisabled(emailField, carregando);
        setDisabled(senhaField, carregando);
        setDisabled(confirmacaoSenhaField, carregando);
        setDisabled(cadastrarButton, carregando);
        setDisabled(voltarLoginButton, carregando);
    }

    private void setDisabled(javafx.scene.Node node, boolean disabled) {
        if (node != null) {
            node.setDisable(disabled);
        }
    }

    private void setFeedback(String message) {
        setFeedback(message, true);
    }

    private void setFeedback(String message, boolean erro) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message == null ? "" : message);
            feedbackLabel.setStyle("-fx-text-fill: " + (erro ? "#b3261e" : "#0b5d2a") + ";");
            feedbackLabel.setVisible(message != null && !message.isBlank());
            feedbackLabel.setManaged(message != null && !message.isBlank());
        }
    }
}

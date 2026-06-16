package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.usuarios.porta.entrada.CasoDeUsoUsuario;
import br.com.sigla.dominio.usuarios.Usuario;
import br.com.sigla.interfacegrafica.aplicativo.FluxoAplicacao;
import br.com.sigla.interfacegrafica.util.ValidadorEntrada;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ControladorCadastroUsuario {

    private final CasoDeUsoUsuario casoDeUsoUsuario;
    private final FluxoAplicacao fluxoAplicacao;

    @FXML
    private TextField nomeField;

    @FXML
    private PasswordField senhaField;

    @FXML
    private Label feedbackLabel;

    public ControladorCadastroUsuario(CasoDeUsoUsuario casoDeUsoUsuario, FluxoAplicacao fluxoAplicacao) {
        this.casoDeUsoUsuario = casoDeUsoUsuario;
        this.fluxoAplicacao = fluxoAplicacao;
    }

    @FXML
    public void initialize() {
        setFeedback("");
    }

    @FXML
    private void onCadastrar() {
        try {
            ValidadorEntrada validador = ValidadorEntrada.nova();
            String nome = validador.texto(textValue(nomeField), "o nome de usuário");
            String senha = validador.texto(textValue(senhaField), "a senha");
            validador.validar();

            casoDeUsoUsuario.registrar(new CasoDeUsoUsuario.RegistrarUsuarioCommand(
                    UUID.randomUUID().toString(),
                    nome,
                    nome,
                    "",
                    senha,
                    Usuario.TipoUsuario.OPERADOR,
                    true
            ));
            fluxoAplicacao.showLogin();
        } catch (IllegalArgumentException exception) {
            setFeedback(br.com.sigla.interfacegrafica.util.MensagensErro.descrever(exception));
        }
    }

    @FXML
    private void onVoltarLogin() {
        fluxoAplicacao.showLogin();
    }

    private String textValue(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    private void setFeedback(String message) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message == null ? "" : message);
            feedbackLabel.setVisible(message != null && !message.isBlank());
            feedbackLabel.setManaged(message != null && !message.isBlank());
        }
    }
}

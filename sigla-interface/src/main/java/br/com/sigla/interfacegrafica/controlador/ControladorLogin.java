package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.interfacegrafica.aplicativo.FluxoAplicacao;
import br.com.sigla.interfacegrafica.aplicativo.SessaoLocalAplicacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.ValidadorEntrada;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class ControladorLogin {

    private static final Logger LOGGER = Logger.getLogger(ControladorLogin.class.getName());

    private final SessaoLocalAplicacao sessaoLocalAplicacao;
    private final FluxoAplicacao fluxoAplicacao;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    public ControladorLogin(SessaoLocalAplicacao sessaoLocalAplicacao, FluxoAplicacao fluxoAplicacao) {
        this.sessaoLocalAplicacao = sessaoLocalAplicacao;
        this.fluxoAplicacao = fluxoAplicacao;
    }

    @FXML
    public void initialize() {
        setErrorVisible(false);
    }

    @FXML
    private void onLogin() {
        String username = usernameField == null ? "" : usernameField.getText();
        String password = passwordField == null ? "" : passwordField.getText();

        ValidadorEntrada validador = ValidadorEntrada.nova();
        validador.texto(username, "o usuário ou e-mail");
        validador.texto(password, "a senha");
        try {
            validador.validar();
        } catch (IllegalArgumentException erro) {
            mostrarErro(br.com.sigla.interfacegrafica.util.MensagensErro.descrever(erro));
            return;
        }

        boolean authenticated = sessaoLocalAplicacao.login(username, password);
        if (authenticated) {
            try {
                setErrorVisible(false);
                fluxoAplicacao.showShell();
            } catch (RuntimeException exception) {
                LOGGER.log(Level.SEVERE, "Falha ao abrir a tela inicial apos login.", exception);
                sessaoLocalAplicacao.logout();
                mostrarErro("Login validado, mas não foi possível abrir a tela inicial. Veja o console.");
            }
            return;
        }

        mostrarErro("Usuário ou senha inválidos.");
    }

    @FXML
    private void onOpenCadastroUsuario() {
        setErrorVisible(false);
        fluxoAplicacao.showView(VisaoAplicacao.ACCOUNT_REGISTRATION);
    }

    @FXML
    private void onEsqueciSenha() {
        mostrarErro("Solicite a redefinição de senha a um administrador.");
    }

    private void mostrarErro(String mensagem) {
        if (errorLabel != null) {
            errorLabel.setText(mensagem == null ? "" : mensagem);
        }
        setErrorVisible(true);
    }

    private void setErrorVisible(boolean visible) {
        if (errorLabel == null) {
            return;
        }
        errorLabel.setVisible(visible);
        errorLabel.setManaged(visible);
    }
}

package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.interfacegrafica.aplicativo.FluxoAplicacao;
import br.com.sigla.interfacegrafica.aplicativo.SessaoLocalAplicacao;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

@Component
public class ControladorLogin {

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
        boolean authenticated = sessaoLocalAplicacao.login(username, password);
        if (authenticated) {
            setErrorVisible(false);
            fluxoAplicacao.showShell();
            return;
        }

        if (errorLabel != null) {
            errorLabel.setText("Usuário ou senha inválidos.");
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

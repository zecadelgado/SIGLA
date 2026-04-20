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
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @FXML
    private void onLogin() {
        boolean authenticated = sessaoLocalAplicacao.login(usernameField.getText(), passwordField.getText());
        if (authenticated) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            fluxoAplicacao.showShell();
            return;
        }

        errorLabel.setText("Usuário ou senha inválidos.");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}

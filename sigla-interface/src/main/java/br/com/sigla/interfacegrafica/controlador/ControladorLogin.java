package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.interfacegrafica.aplicativo.FluxoAplicacao;
import br.com.sigla.interfacegrafica.aplicativo.SessaoLocalAplicacao;
import br.com.sigla.interfacegrafica.async.ExecutorTarefasUi;
import br.com.sigla.interfacegrafica.async.SobreposicaoCarregamento;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.ValidadorEntrada;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import org.springframework.stereotype.Component;

import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class ControladorLogin {

    private static final Logger LOGGER = Logger.getLogger(ControladorLogin.class.getName());

    private final SessaoLocalAplicacao sessaoLocalAplicacao;
    private final FluxoAplicacao fluxoAplicacao;
    private final ExecutorTarefasUi executorTarefasUi;

    private SobreposicaoCarregamento overlayCarregamento;

    @FXML
    private AnchorPane txtLoginInsira;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    public ControladorLogin(
            SessaoLocalAplicacao sessaoLocalAplicacao,
            FluxoAplicacao fluxoAplicacao,
            ExecutorTarefasUi executorTarefasUi
    ) {
        this.sessaoLocalAplicacao = sessaoLocalAplicacao;
        this.fluxoAplicacao = fluxoAplicacao;
        this.executorTarefasUi = executorTarefasUi;
    }

    @FXML
    public void initialize() {
        setErrorVisible(false);
        instalarOverlay();
    }

    // Veu de "Entrando..." sobre a tela de login. A autenticacao bate no banco remoto e
    // pode passar de 1 segundo; sem isso a tela congelava sem nenhum retorno ao usuario.
    private void instalarOverlay() {
        if (txtLoginInsira == null) {
            return;
        }
        overlayCarregamento = new SobreposicaoCarregamento("Entrando...");
        overlayCarregamento.setVisible(false);
        AnchorPane.setTopAnchor(overlayCarregamento, 0.0);
        AnchorPane.setRightAnchor(overlayCarregamento, 0.0);
        AnchorPane.setBottomAnchor(overlayCarregamento, 0.0);
        AnchorPane.setLeftAnchor(overlayCarregamento, 0.0);
        txtLoginInsira.getChildren().add(overlayCarregamento);
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

        // Autentica fora da thread de UI: a consulta ao banco remoto pode demorar e nao
        // pode congelar a tela. O veu de carregamento da o retorno visual enquanto isso.
        setErrorVisible(false);
        mostrarCarregando(true);
        executorTarefasUi.executar(
                () -> sessaoLocalAplicacao.login(username, password),
                autenticado -> {
                    if (autenticado) {
                        abrirTelaInicial();
                    } else {
                        mostrarCarregando(false);
                        mostrarErro("Usuário ou senha inválidos.");
                    }
                },
                erro -> {
                    mostrarCarregando(false);
                    mostrarErro(br.com.sigla.interfacegrafica.util.MensagensErro.descrever(
                            "Nao foi possivel validar o login (verifique a conexao com o banco):", erro));
                }
        );
    }

    private void abrirTelaInicial() {
        try {
            setErrorVisible(false);
            fluxoAplicacao.showShell();
            // Sucesso: a cena troca para o shell e este overlay e descartado junto.
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Falha ao abrir a tela inicial apos login.", exception);
            sessaoLocalAplicacao.logout();
            mostrarCarregando(false);
            mostrarErro("Login validado, mas não foi possível abrir a tela inicial. Veja o console.");
        }
    }

    private void mostrarCarregando(boolean carregando) {
        if (overlayCarregamento != null) {
            overlayCarregamento.setVisible(carregando);
            if (carregando) {
                overlayCarregamento.toFront();
                overlayCarregamento.iniciarAnimacao();
            } else {
                overlayCarregamento.pararAnimacao();
            }
        }
        if (usernameField != null) {
            usernameField.setDisable(carregando);
        }
        if (passwordField != null) {
            passwordField.setDisable(carregando);
        }
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

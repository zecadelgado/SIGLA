package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.interfacegrafica.aplicativo.FluxoAplicacao;
import br.com.sigla.interfacegrafica.aplicativo.SessaoLocalAplicacao;
import br.com.sigla.interfacegrafica.async.ExecutorTarefasUi;
import br.com.sigla.interfacegrafica.async.SobreposicaoCarregamento;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.ValidadorEntrada;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class ControladorLogin {

    private static final Logger LOGGER = Logger.getLogger(ControladorLogin.class.getName());
    private static final Duration ATRASO_REVELACAO_CARREGAMENTO = Duration.seconds(3);

    private final SessaoLocalAplicacao sessaoLocalAplicacao;
    private final FluxoAplicacao fluxoAplicacao;
    private final ExecutorTarefasUi executorTarefasUi;

    private SobreposicaoCarregamento overlayCarregamento;
    private PauseTransition agendamentoRevelacaoCarregamento;
    private boolean loginEmAndamento;

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

    // Veu de "Entrando..." sobre a tela de login. A autenticacao bate no banco remoto;
    // o indicador so aparece se a espera passar de 3 segundos.
    private void instalarOverlay() {
        if (txtLoginInsira == null) {
            return;
        }
        overlayCarregamento = new SobreposicaoCarregamento("Entrando...");
        overlayCarregamento.setVisible(false);
        overlayCarregamento.setMouseTransparent(true);
        AnchorPane.setTopAnchor(overlayCarregamento, 0.0);
        AnchorPane.setRightAnchor(overlayCarregamento, 0.0);
        AnchorPane.setBottomAnchor(overlayCarregamento, 0.0);
        AnchorPane.setLeftAnchor(overlayCarregamento, 0.0);
        txtLoginInsira.getChildren().add(overlayCarregamento);
    }

    @FXML
    private void onLogin() {
        if (loginEmAndamento) {
            return;
        }
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
            mostrarCarregando(false);
            // Sucesso: a cena troca para o shell e este overlay e descartado junto.
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Falha ao abrir a tela inicial apos login.", exception);
            sessaoLocalAplicacao.logout();
            mostrarCarregando(false);
            mostrarErro("Login validado, mas não foi possível abrir a tela inicial. Veja o console.");
        }
    }

    private void mostrarCarregando(boolean carregando) {
        loginEmAndamento = carregando;
        if (overlayCarregamento != null) {
            if (carregando) {
                agendarRevelacaoCarregamento();
            } else {
                cancelarAgendamentoRevelacaoCarregamento();
                overlayCarregamento.setVisible(false);
                overlayCarregamento.setMouseTransparent(true);
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

    private void agendarRevelacaoCarregamento() {
        cancelarAgendamentoRevelacaoCarregamento();
        overlayCarregamento.setVisible(false);
        overlayCarregamento.setMouseTransparent(true);
        overlayCarregamento.pararAnimacao();

        agendamentoRevelacaoCarregamento = new PauseTransition(ATRASO_REVELACAO_CARREGAMENTO);
        agendamentoRevelacaoCarregamento.setOnFinished(evento -> {
            agendamentoRevelacaoCarregamento = null;
            if (!loginEmAndamento || overlayCarregamento == null) {
                return;
            }
            overlayCarregamento.toFront();
            overlayCarregamento.setVisible(true);
            overlayCarregamento.setMouseTransparent(false);
            overlayCarregamento.iniciarAnimacao();
        });
        agendamentoRevelacaoCarregamento.playFromStart();
    }

    private void cancelarAgendamentoRevelacaoCarregamento() {
        if (agendamentoRevelacaoCarregamento != null) {
            agendamentoRevelacaoCarregamento.stop();
            agendamentoRevelacaoCarregamento = null;
        }
    }

    @FXML
    private void onOpenCadastroUsuario() {
        if (loginEmAndamento) {
            return;
        }
        setErrorVisible(false);
        fluxoAplicacao.showView(VisaoAplicacao.ACCOUNT_REGISTRATION);
    }

    @FXML
    private void onEsqueciSenha() {
        if (loginEmAndamento) {
            return;
        }
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

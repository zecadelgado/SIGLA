package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.interfacegrafica.aplicativo.FluxoAplicacao;
import br.com.sigla.interfacegrafica.aplicativo.SessaoLocalAplicacao;
import br.com.sigla.interfacegrafica.async.ExecutorTarefasUi;
import br.com.sigla.interfacegrafica.async.SobreposicaoCarregamento;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.DialogoUi;
import br.com.sigla.interfacegrafica.util.MensagensErro;
import br.com.sigla.interfacegrafica.util.ValidadorEntrada;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ControladorLogin {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControladorLogin.class);
    private static final Duration ATRASO_REVELACAO_CARREGAMENTO = Duration.millis(1500);

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

    @FXML
    private Button loginButton;

    @FXML
    private Button cadastroButton;

    @FXML
    private Hyperlink esqueciSenhaLink;

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
        setFeedbackVisible(false);
        instalarOverlay();
    }

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
        String username = textValue(usernameField);
        String password = passwordField == null ? "" : passwordField.getText();

        ValidadorEntrada validador = ValidadorEntrada.nova();
        validador.texto(username, "o usuario ou e-mail");
        validador.texto(password, "a senha");
        try {
            validador.validar();
        } catch (IllegalArgumentException erro) {
            mostrarFeedback(MensagensErro.descrever(erro), true);
            return;
        }

        setFeedbackVisible(false);
        mostrarCarregando(true);
        executorTarefasUi.executar(
                () -> sessaoLocalAplicacao.login(username, password),
                autenticado -> {
                    if (autenticado) {
                        abrirTelaInicial();
                    } else {
                        mostrarCarregando(false);
                        mostrarFeedback("E-mail ou senha inválidos.", true);
                    }
                },
                erro -> {
                    LOGGER.error("Falha tecnica ao autenticar usuario.", erro);
                    mostrarCarregando(false);
                    mostrarFeedback("Não foi possível concluir a ação agora. Verifique sua conexão e tente novamente.", true);
                }
        );
    }

    private void abrirTelaInicial() {
        try {
            setFeedbackVisible(false);
            fluxoAplicacao.showShell();
            mostrarCarregando(false);
        } catch (RuntimeException exception) {
            LOGGER.error("Falha ao abrir a tela inicial apos login.", exception);
            sessaoLocalAplicacao.logout();
            mostrarCarregando(false);
            mostrarFeedback("Login validado, mas nao foi possivel abrir a tela inicial.", true);
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
        setDisabled(usernameField, carregando);
        setDisabled(passwordField, carregando);
        setDisabled(loginButton, carregando);
        setDisabled(cadastroButton, carregando);
        setDisabled(esqueciSenhaLink, carregando);
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
        setFeedbackVisible(false);
        fluxoAplicacao.showView(VisaoAplicacao.ACCOUNT_REGISTRATION);
    }

    @FXML
    private void onEsqueciSenha() {
        if (loginEmAndamento) {
            return;
        }
        abrirDialogoRecuperacaoSenha();
    }

    private void abrirDialogoRecuperacaoSenha() {
        Dialog<Void> dialog = new Dialog<>();
        DialogoUi.estilizar(dialog);
        dialog.setTitle("Recuperar senha");

        ButtonType enviarCodigo = new ButtonType("Enviar código", ButtonBar.ButtonData.OTHER);
        ButtonType redefinirSenha = new ButtonType("Redefinir senha", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(enviarCodigo, redefinirSenha, ButtonType.CANCEL);

        TextField email = new TextField(textValue(usernameField));
        TextField codigo = new TextField();
        PasswordField novaSenha = new PasswordField();
        PasswordField confirmacaoSenha = new PasswordField();
        Label feedback = new Label();
        feedback.setWrapText(true);
        feedback.setManaged(false);
        feedback.setVisible(false);

        email.setPromptText("E-mail");
        codigo.setPromptText("Código recebido");
        novaSenha.setPromptText("Nova senha");
        confirmacaoSenha.setPromptText("Confirmar nova senha");
        setCamposRedefinicaoHabilitados(false, codigo, novaSenha, confirmacaoSenha);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12, 0, 0, 0));
        grid.addRow(0, new Label("E-mail"), email);
        grid.addRow(1, new Label("Código"), codigo);
        grid.addRow(2, new Label("Nova senha"), novaSenha);
        grid.addRow(3, new Label("Confirmar senha"), confirmacaoSenha);
        grid.add(feedback, 0, 4, 2, 1);
        dialog.getDialogPane().setContent(grid);

        Node enviarButton = dialog.getDialogPane().lookupButton(enviarCodigo);
        Node redefinirButton = dialog.getDialogPane().lookupButton(redefinirSenha);
        redefinirButton.setDisable(true);

        enviarButton.addEventFilter(ActionEvent.ACTION, evento -> {
            evento.consume();
            String emailInformado = textValue(email);
            try {
                validarEmail(emailInformado);
            } catch (IllegalArgumentException exception) {
                setDialogFeedback(feedback, MensagensErro.descrever(exception), true);
                return;
            }

            setDialogLoading(true, email, codigo, novaSenha, confirmacaoSenha, enviarButton, redefinirButton);
            executorTarefasUi.executar(
                    () -> {
                        sessaoLocalAplicacao.solicitarRecuperacaoSenha(emailInformado);
                        return null;
                    },
                    ignored -> {
                        setDialogLoading(false, email, codigo, novaSenha, confirmacaoSenha, enviarButton, redefinirButton);
                        setCamposRedefinicaoHabilitados(true, codigo, novaSenha, confirmacaoSenha);
                        redefinirButton.setDisable(false);
                        setDialogFeedback(feedback,
                                "Se o e-mail estiver cadastrado, enviaremos um codigo de recuperacao.", false);
                    },
                    erro -> {
                        LOGGER.error("Falha ao solicitar recuperacao de senha.", erro);
                        setDialogLoading(false, email, codigo, novaSenha, confirmacaoSenha, enviarButton, redefinirButton);
                        setDialogFeedback(feedback, MensagensErro.descrever(erro), true);
                    }
            );
        });

        redefinirButton.addEventFilter(ActionEvent.ACTION, evento -> {
            evento.consume();
            String emailInformado = textValue(email);
            String codigoInformado = textValue(codigo);
            String senhaInformada = novaSenha.getText() == null ? "" : novaSenha.getText();
            String confirmacaoInformada = confirmacaoSenha.getText() == null ? "" : confirmacaoSenha.getText();
            try {
                validarRedefinicao(emailInformado, codigoInformado, senhaInformada, confirmacaoInformada);
            } catch (IllegalArgumentException exception) {
                setDialogFeedback(feedback, MensagensErro.descrever(exception), true);
                return;
            }

            setDialogLoading(true, email, codigo, novaSenha, confirmacaoSenha, enviarButton, redefinirButton);
            executorTarefasUi.executar(
                    () -> {
                        sessaoLocalAplicacao.redefinirSenhaComCodigo(emailInformado, codigoInformado, senhaInformada);
                        return null;
                    },
                    ignored -> {
                        dialog.close();
                        mostrarFeedback("Senha redefinida com sucesso. Entre com sua nova senha.", false);
                    },
                    erro -> {
                        LOGGER.error("Falha ao redefinir senha com codigo.", erro);
                        setDialogLoading(false, email, codigo, novaSenha, confirmacaoSenha, enviarButton, redefinirButton);
                        setDialogFeedback(feedback, MensagensErro.descrever(erro), true);
                    }
            );
        });

        dialog.showAndWait();
    }

    private void validarRedefinicao(String email, String codigo, String novaSenha, String confirmacaoSenha) {
        ValidadorEntrada validador = ValidadorEntrada.nova();
        validador.texto(email, "o e-mail");
        validador.texto(codigo, "o código de recuperação");
        validador.texto(novaSenha, "a nova senha");
        validador.texto(confirmacaoSenha, "a confirmação da senha");
        validador.exigir(email.isBlank() || email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"),
                "Informe um e-mail válido.");
        validador.exigir(novaSenha.length() >= 6, "A senha deve ter pelo menos 6 caracteres.");
        validador.exigir(novaSenha.equals(confirmacaoSenha), "A confirmação da senha deve ser igual à senha.");
        validador.validar();
    }

    private void validarEmail(String email) {
        ValidadorEntrada validador = ValidadorEntrada.nova();
        validador.texto(email, "o e-mail");
        validador.exigir(email.isBlank() || email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"),
                "Informe um e-mail válido.");
        validador.validar();
    }

    private void setDialogLoading(
            boolean carregando,
            TextField email,
            TextField codigo,
            PasswordField novaSenha,
            PasswordField confirmacaoSenha,
            Node enviarButton,
            Node redefinirButton
    ) {
        email.setDisable(carregando);
        enviarButton.setDisable(carregando);
        if (!codigo.isDisabled() || carregando) {
            codigo.setDisable(carregando);
            novaSenha.setDisable(carregando);
            confirmacaoSenha.setDisable(carregando);
            redefinirButton.setDisable(carregando);
        }
    }

    private void setCamposRedefinicaoHabilitados(boolean habilitados, Node... nodes) {
        for (Node node : nodes) {
            node.setDisable(!habilitados);
        }
    }

    private void setDialogFeedback(Label feedback, String mensagem, boolean erro) {
        feedback.setText(mensagem == null ? "" : mensagem);
        feedback.setStyle("-fx-text-fill: " + (erro ? "#b3261e" : "#0b5d2a") + "; -fx-font-weight: bold;");
        feedback.setVisible(mensagem != null && !mensagem.isBlank());
        feedback.setManaged(mensagem != null && !mensagem.isBlank());
    }

    private void mostrarFeedback(String mensagem, boolean erro) {
        if (errorLabel != null) {
            errorLabel.setText(mensagem == null ? "" : mensagem);
            errorLabel.setStyle("-fx-text-fill: " + (erro ? "#ffcccc" : "#bff2ce") + "; -fx-font-weight: bold;");
        }
        setFeedbackVisible(true);
    }

    private void setFeedbackVisible(boolean visible) {
        if (errorLabel == null) {
            return;
        }
        errorLabel.setVisible(visible);
        errorLabel.setManaged(visible);
    }

    private String textValue(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    private void setDisabled(Node node, boolean disabled) {
        if (node != null) {
            node.setDisable(disabled);
        }
    }
}

package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.funcionarios.porta.entrada.CasoDeUsoFuncionario;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.dominio.funcionarios.Funcionario;
import br.com.sigla.interfacegrafica.formatador.FormatadorMascaraCpf;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.TradutorInterface;
import br.com.sigla.interfacegrafica.util.UtilJanela;
import br.com.sigla.interfacegrafica.util.ValidadorEntrada;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

@Component
public class ControladorNovoCadastro {

    private final CasoDeUsoCliente casoDeUsoCliente;
    private final CasoDeUsoFuncionario casoDeUsoFuncionario;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final FormatadorMascaraCpf formatadorMascaraCpf;

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
    private TextField cargoField;
    @FXML
    private ComboBox<String> tipoCombo;
    @FXML
    private ComboBox<Funcionario.FuncionarioStatus> statusFuncionarioCombo;
    @FXML
    private Label feedbackLabel;

    public ControladorNovoCadastro(
            CasoDeUsoCliente casoDeUsoCliente,
            CasoDeUsoFuncionario casoDeUsoFuncionario,
            GerenciadorNavegacao gerenciadorNavegacao,
            FormatadorMascaraCpf formatadorMascaraCpf
    ) {
        this.casoDeUsoCliente = casoDeUsoCliente;
        this.casoDeUsoFuncionario = casoDeUsoFuncionario;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
        this.formatadorMascaraCpf = formatadorMascaraCpf;
    }

    @FXML
    public void initialize() {
        if (tipoCombo != null) {
            TradutorInterface.aplicar(tipoCombo);
            tipoCombo.getItems().setAll("CLIENTE", "FUNCIONARIO");
            tipoCombo.getSelectionModel().select("CLIENTE");
            tipoCombo.valueProperty().addListener((observable, oldValue, newValue) -> atualizarModoCadastro());
        }
        if (statusFuncionarioCombo != null) {
            TradutorInterface.aplicar(statusFuncionarioCombo);
            statusFuncionarioCombo.getItems().setAll(Funcionario.FuncionarioStatus.values());
            statusFuncionarioCombo.getSelectionModel().select(Funcionario.FuncionarioStatus.ACTIVE);
        }

        formatadorMascaraCpf.aplicarCpf(cpfField);
        formatadorMascaraCpf.aplicarCnpj(cnpjField);
        formatadorMascaraCpf.aplicarTelefone(telefoneField);
        aplicarFiltroNumerico(cepField, 8);
        aplicarFiltroNumerico(numeroField, 10);
        aplicarFiltroEstado(estadoField);
        aplicarFiltroSemEspaco(emailField);
        atualizarModoCadastro();
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
        } catch (RuntimeException exception) {
            setFeedback(exception.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        UtilJanela.fecharJanela(nomeField);
    }

    private void cadastrarCliente() {
        validarCliente();
        String id = UUID.randomUUID().toString();
        String nome = texto(nomeField);
        String razaoSocial = texto(razaoSocialField);
        String cnpj = texto(cnpjField);
        Cliente.TipoCliente tipoCliente = cnpj.isBlank() ? Cliente.TipoCliente.PESSOA_FISICA : Cliente.TipoCliente.PESSOA_JURIDICA;
        String nomeCliente = !razaoSocial.isBlank() ? razaoSocial : nome;
        List<CasoDeUsoCliente.ContactCommand> contatos = contatosCliente(nomeCliente);

        casoDeUsoCliente.register(new CasoDeUsoCliente.RegisterClienteCommand(
                id,
                tipoCliente,
                nomeCliente,
                razaoSocial,
                nome,
                texto(cpfField),
                cnpj,
                texto(telefoneField),
                texto(emailField),
                texto(cepField),
                texto(ruaField),
                texto(numeroField),
                texto(complementoField),
                texto(bairroField),
                texto(cidadeField),
                texto(estadoField),
                contatos,
                texto(observacoesField),
                true
        ));
    }

    private void cadastrarFuncionario() {
        validarFuncionario();
        Funcionario.FuncionarioStatus status = statusFuncionarioCombo == null || statusFuncionarioCombo.getValue() == null
                ? Funcionario.FuncionarioStatus.ACTIVE
                : statusFuncionarioCombo.getValue();

        casoDeUsoFuncionario.register(new CasoDeUsoFuncionario.RegisterFuncionarioCommand(
                UUID.randomUUID().toString(),
                texto(nomeField),
                texto(cpfField),
                texto(cargoField),
                texto(telefoneField),
                texto(emailField),
                texto(cepField),
                texto(ruaField),
                texto(numeroField),
                texto(complementoField),
                texto(bairroField),
                texto(cidadeField),
                texto(estadoField),
                status
        ));
    }

    private void validarCliente() {
        ValidadorEntrada validador = ValidadorEntrada.nova();
        validador.exigir(!(texto(nomeField).isBlank() && texto(razaoSocialField).isBlank()),
                "Informe o nome completo ou a razão social.");
        validarEmail(validador);
        validador.validar();
    }

    private void validarFuncionario() {
        ValidadorEntrada validador = ValidadorEntrada.nova();
        validador.exigir(!texto(nomeField).isBlank(), "Informe o nome do funcionário.");
        validador.exigir(!texto(cargoField).isBlank(), "Informe o cargo do funcionário.");
        validador.exigir(!(texto(telefoneField).isBlank() && texto(emailField).isBlank()),
                "Informe o telefone ou o e-mail do funcionário.");
        validarEmail(validador);
        validador.validar();
    }

    private void validarEmail(ValidadorEntrada validador) {
        String email = texto(emailField);
        if (!email.isBlank() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            validador.erro("Informe um e-mail válido.");
        }
    }

    private List<CasoDeUsoCliente.ContactCommand> contatosCliente(String nomeCliente) {
        List<CasoDeUsoCliente.ContactCommand> contatos = new ArrayList<>();
        String telefone = texto(telefoneField);
        String email = texto(emailField);
        if (!telefone.isBlank() || !email.isBlank()) {
            contatos.add(new CasoDeUsoCliente.ContactCommand(
                    UUID.randomUUID().toString(),
                    texto(nomeField).isBlank() ? nomeCliente : texto(nomeField),
                    "Contato principal",
                    telefone,
                    email,
                    true
            ));
        }
        return contatos;
    }

    private boolean isCadastroFuncionario() {
        return tipoCombo != null && "FUNCIONARIO".equals(tipoCombo.getValue());
    }

    private void atualizarModoCadastro() {
        boolean funcionario = isCadastroFuncionario();
        // CNPJ e razao social sao exclusivos de pessoa juridica (cliente).
        setDisabled(cnpjField, funcionario);
        setDisabled(razaoSocialField, funcionario);
        // CPF e endereco agora valem para cliente E funcionario.
        setDisabled(cpfField, false);
        setDisabled(ruaField, false);
        setDisabled(numeroField, false);
        setDisabled(complementoField, false);
        setDisabled(bairroField, false);
        setDisabled(cidadeField, false);
        setDisabled(cepField, false);
        setDisabled(estadoField, false);
        // Cargo e situacao sao exclusivos de funcionario.
        setDisabled(cargoField, !funcionario);
        setDisabled(statusFuncionarioCombo, !funcionario);
    }

    private void aplicarFiltroNumerico(TextField field, int maxDigits) {
        if (field == null) {
            return;
        }
        field.setTextFormatter(new TextFormatter<>(limitar(text -> text.replaceAll("\\D", ""), maxDigits)));
    }

    private void aplicarFiltroEstado(TextField field) {
        if (field == null) {
            return;
        }
        field.setTextFormatter(new TextFormatter<>(limitar(text -> text.replaceAll("[^A-Za-z]", "").toUpperCase(), 2)));
    }

    private void aplicarFiltroSemEspaco(TextField field) {
        if (field == null) {
            return;
        }
        field.setTextFormatter(new TextFormatter<>(change -> {
            String next = change.getControlNewText();
            return next == null || next.matches("\\S*") ? change : null;
        }));
    }

    private UnaryOperator<TextFormatter.Change> limitar(java.util.function.Function<String, String> normalizer, int maxLength) {
        return change -> {
            String normalized = normalizer.apply(change.getControlNewText());
            if (normalized.length() > maxLength) {
                normalized = normalized.substring(0, maxLength);
            }
            change.setRange(0, change.getControlText().length());
            change.setText(normalized);
            change.setCaretPosition(normalized.length());
            change.setAnchor(normalized.length());
            return change;
        };
    }

    private void setDisabled(javafx.scene.Node node, boolean disabled) {
        if (node != null) {
            node.setDisable(disabled);
        }
    }

    private String texto(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    private void setFeedback(String message) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message == null ? "" : message);
        }
    }
}

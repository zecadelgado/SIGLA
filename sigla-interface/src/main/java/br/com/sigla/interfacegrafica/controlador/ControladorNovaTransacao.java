package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.dominio.financeiro.CategoriaFinanceira;
import br.com.sigla.dominio.financeiro.FormaPagamentoFinanceira;
import br.com.sigla.interfacegrafica.aplicativo.SessaoLocalAplicacao;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaReferencias;
import br.com.sigla.interfacegrafica.formatador.FormatadorMascaraMoeda;
import br.com.sigla.interfacegrafica.modelo.OpcaoId;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.TradutorInterface;
import br.com.sigla.interfacegrafica.util.UtilComboBox;
import br.com.sigla.interfacegrafica.util.UtilJanela;
import br.com.sigla.interfacegrafica.util.ValidadorEntrada;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class ControladorNovaTransacao {

    private final CasoDeUsoFinanceiro casoDeUsoFinanceiro;
    private final ServicoConsultaReferencias servicoConsultaReferencias;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final SessaoLocalAplicacao sessaoLocalAplicacao;
    private final FormatadorMascaraMoeda formatadorMoeda;

    @FXML
    private ComboBox<CasoDeUsoFinanceiro.TransactionType> tipoCombo;
    @FXML
    private ComboBox<CategoriaFinanceira> categoriaCombo;
    @FXML
    private TextField descricaoField;
    @FXML
    private ComboBox<OpcaoId> clienteCombo;
    @FXML
    private TextField valorField;
    @FXML
    private ComboBox<OpcaoId> ordemCombo;
    @FXML
    private DatePicker emissaoPicker;
    @FXML
    private DatePicker vencimentoPicker;
    @FXML
    private DatePicker pagamentoPicker;
    @FXML
    private ComboBox<FormaPagamentoFinanceira> formaPagamentoCombo;
    @FXML
    private ComboBox<Boolean> parceladoCombo;
    @FXML
    private TextField parcelasField;
    @FXML
    private TextField observacoesField;
    @FXML
    private TextField criadoPorField;
    @FXML
    private ComboBox<CasoDeUsoFinanceiro.TransactionStatus> statusCombo;
    @FXML
    private Label feedbackLabel;

    public ControladorNovaTransacao(
            CasoDeUsoFinanceiro casoDeUsoFinanceiro,
            ServicoConsultaReferencias servicoConsultaReferencias,
            GerenciadorNavegacao gerenciadorNavegacao,
            SessaoLocalAplicacao sessaoLocalAplicacao,
            FormatadorMascaraMoeda formatadorMoeda
    ) {
        this.casoDeUsoFinanceiro = casoDeUsoFinanceiro;
        this.servicoConsultaReferencias = servicoConsultaReferencias;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
        this.sessaoLocalAplicacao = sessaoLocalAplicacao;
        this.formatadorMoeda = formatadorMoeda;
    }

    @FXML
    public void initialize() {
        if (tipoCombo != null) {
            TradutorInterface.aplicar(tipoCombo);
            tipoCombo.getItems().setAll(CasoDeUsoFinanceiro.TransactionType.values());
            tipoCombo.getSelectionModel().select(CasoDeUsoFinanceiro.TransactionType.ENTRY);
            tipoCombo.valueProperty().addListener((observable, oldValue, newValue) -> carregarCategorias());
        }
        configurarCombos();
        UtilComboBox.preencher(clienteCombo, servicoConsultaReferencias.clientes(), true);
        UtilComboBox.preencher(ordemCombo, servicoConsultaReferencias.ordensServico(), true);
        if (clienteCombo != null) {
            clienteCombo.valueProperty().addListener((observable, oldValue, newValue) ->
                    UtilComboBox.preencher(ordemCombo, servicoConsultaReferencias.ordensServicoDoCliente(UtilComboBox.idSelecionado(clienteCombo)), true)
            );
        }
        formatadorMoeda.aplicar(valorField);
        carregarCategorias();
        if (emissaoPicker != null) {
            emissaoPicker.setValue(LocalDate.now());
        }
        if (criadoPorField != null) {
            criadoPorField.setEditable(false);
            if (sessaoLocalAplicacao.usuarioAtual() != null) {
                criadoPorField.setText(sessaoLocalAplicacao.usuarioAtual().nome());
            }
        }
        setFeedback("");
    }

    @FXML
    private void onAdicionar() {
        try {
            ValidadorEntrada validador = ValidadorEntrada.nova();
            String descricao = validador.texto(texto(descricaoField), "a descrição da transação");
            BigDecimal valor = validador.valorPositivo(formatadorMoeda.valor(valorField), "o valor da transação");
            boolean parcelado = parceladoCombo != null && Boolean.TRUE.equals(parceladoCombo.getValue());
            String parcelasTexto = texto(parcelasField);
            int parcelas = 1;
            if (parcelado || !parcelasTexto.isBlank()) {
                parcelas = validador.inteiroPositivo(parcelasTexto, "o número de parcelas");
            }
            validador.validar();

            var cliente = UtilComboBox.selecionado(clienteCombo);
            var ordem = UtilComboBox.selecionado(ordemCombo);
            casoDeUsoFinanceiro.registerTransaction(new CasoDeUsoFinanceiro.RegisterTransacaoFinanceiraCommand(
                    UUID.randomUUID().toString(),
                    tipoCombo == null || tipoCombo.getValue() == null ? CasoDeUsoFinanceiro.TransactionType.ENTRY : tipoCombo.getValue(),
                    categoriaCombo == null || categoriaCombo.getValue() == null ? "" : categoriaCombo.getValue().id(),
                    descricao,
                    cliente == null ? "" : cliente.id(),
                    "",
                    ordem == null ? "" : ordem.id(),
                    valor,
                    emissaoPicker == null ? LocalDate.now() : emissaoPicker.getValue(),
                    vencimentoPicker == null ? null : vencimentoPicker.getValue(),
                    pagamentoPicker == null ? null : pagamentoPicker.getValue(),
                    formaPagamentoCombo == null || formaPagamentoCombo.getValue() == null ? "" : formaPagamentoCombo.getValue().id(),
                    parcelado,
                    parcelas,
                    resolveUsuarioAtual(),
                    observacoesField == null ? "" : observacoesField.getText(),
                    statusCombo == null || statusCombo.getValue() == null ? CasoDeUsoFinanceiro.TransactionStatus.PENDING : statusCombo.getValue()
            ));
            gerenciadorNavegacao.navigateTo(VisaoAplicacao.FINANCE);
            UtilJanela.fecharJanela(tipoCombo);
        } catch (Exception exception) {
            setFeedback(exception.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        UtilJanela.fecharJanela(tipoCombo);
    }

    private String texto(TextField campo) {
        return campo == null || campo.getText() == null ? "" : campo.getText();
    }

    private void setFeedback(String message) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message == null ? "" : message);
        }
    }

    private String resolveUsuarioAtual() {
        // criado_por tem FK para usuarios(id); usa sempre o id da sessao (o campo so exibe o nome).
        return sessaoLocalAplicacao.usuarioAtual() == null ? "" : sessaoLocalAplicacao.usuarioAtual().id();
    }

    private void configurarCombos() {
        if (categoriaCombo != null) {
            categoriaCombo.setConverter(new StringConverter<>() {
                @Override
                public String toString(CategoriaFinanceira categoria) {
                    return categoria == null ? "" : TradutorInterface.texto(categoria.nome());
                }

                @Override
                public CategoriaFinanceira fromString(String string) {
                    return null;
                }
            });
        }
        if (formaPagamentoCombo != null) {
            formaPagamentoCombo.setConverter(new StringConverter<>() {
                @Override
                public String toString(FormaPagamentoFinanceira forma) {
                    return forma == null ? "" : forma.nome();
                }

                @Override
                public FormaPagamentoFinanceira fromString(String string) {
                    return null;
                }
            });
            formaPagamentoCombo.getItems().setAll(casoDeUsoFinanceiro.listFormasPagamentoAtivas());
            if (!formaPagamentoCombo.getItems().isEmpty()) {
                formaPagamentoCombo.getSelectionModel().selectFirst();
            }
        }
        if (parceladoCombo != null) {
            TradutorInterface.aplicar(parceladoCombo);
            parceladoCombo.getItems().setAll(Boolean.FALSE, Boolean.TRUE);
            parceladoCombo.getSelectionModel().select(Boolean.FALSE);
        }
        if (statusCombo != null) {
            TradutorInterface.aplicar(statusCombo);
            statusCombo.getItems().setAll(CasoDeUsoFinanceiro.TransactionStatus.PENDING, CasoDeUsoFinanceiro.TransactionStatus.PAID);
            statusCombo.getSelectionModel().select(CasoDeUsoFinanceiro.TransactionStatus.PENDING);
        }
    }

    private void carregarCategorias() {
        if (categoriaCombo == null) {
            return;
        }
        CasoDeUsoFinanceiro.TransactionType tipo = tipoCombo == null || tipoCombo.getValue() == null
                ? CasoDeUsoFinanceiro.TransactionType.ENTRY
                : tipoCombo.getValue();
        categoriaCombo.getItems().setAll(casoDeUsoFinanceiro.listCategoriasAtivas().stream()
                .filter(categoria -> tipo == CasoDeUsoFinanceiro.TransactionType.EXPENSE
                        ? categoria.tipo().equalsIgnoreCase("EXPENSE")
                        : categoria.tipo().equalsIgnoreCase("ENTRY"))
                .toList());
        if (!categoriaCombo.getItems().isEmpty()) {
            categoriaCombo.getSelectionModel().selectFirst();
        }
    }
}

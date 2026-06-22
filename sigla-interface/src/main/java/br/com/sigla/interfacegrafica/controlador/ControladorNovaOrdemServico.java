package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.dominio.servicos.DadosFormularioServico;
import br.com.sigla.dominio.servicos.OpcoesFormularioServico;
import br.com.sigla.dominio.servicos.OrdemServico;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaReferencias;
import br.com.sigla.interfacegrafica.formatador.FormatadorMascaraMoeda;
import br.com.sigla.interfacegrafica.modelo.OpcaoId;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.GrupoCheckboxes;
import br.com.sigla.interfacegrafica.util.GrupoProdutosQtde;
import br.com.sigla.interfacegrafica.util.UtilComboBox;
import br.com.sigla.interfacegrafica.util.UtilJanela;
import br.com.sigla.interfacegrafica.util.ValidadorEntrada;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static br.com.sigla.interfacegrafica.util.ResolvedorEntradaTexto.parseEnum;

@Component
public class ControladorNovaOrdemServico {

    private final CasoDeUsoOrdemServico casoDeUsoOrdemServico;
    private final ServicoConsultaReferencias servicoConsultaReferencias;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final FormatadorMascaraMoeda formatadorMoeda;

    @FXML
    private ComboBox<OpcaoId> clienteCombo;
    @FXML
    private ComboBox<OpcaoId> contratoCombo;
    @FXML
    private TextField tituloField;
    @FXML
    private TextField descricaoField;
    @FXML
    private TextField tipoServicoField;
    @FXML
    private TextField statusField;
    @FXML
    private ComboBox<OpcaoId> responsavelInternoCombo;
    @FXML
    private DatePicker dataAgendadaPicker;
    @FXML
    private DatePicker dataInicioPicker;
    @FXML
    private DatePicker dataFimPicker;
    @FXML
    private ComboBox<OpcaoId> responsavelSecundarioCombo;
    @FXML
    private ComboBox<OpcaoId> executadoPorCombo;
    @FXML
    private TextField valorServicoField;
    @FXML
    private TextField observacoesField;
    @FXML
    private Label feedbackLabel;
    @FXML
    private VBox formularioPdfBox;

    // Campos do PDF da Ordem de Servico (construidos em initialize).
    private CheckBox manhaCheck;
    private CheckBox tardeCheck;
    private TextField horaInicioField;
    private TextField horaTerminoField;
    private TextField etapaField;
    private TextField etapaDeField;
    private TextField produto1QtdField;
    private TextField produto1CaldaField;
    private TextField produto2QtdField;
    private TextField produto2CaldaField;
    private GrupoCheckboxes aplicacaoGeralGrupo;
    private GrupoCheckboxes manutencaoGrupo;
    private GrupoProdutosQtde produtosGrupo;

    public ControladorNovaOrdemServico(
            CasoDeUsoOrdemServico casoDeUsoOrdemServico,
            ServicoConsultaReferencias servicoConsultaReferencias,
            GerenciadorNavegacao gerenciadorNavegacao,
            FormatadorMascaraMoeda formatadorMoeda
    ) {
        this.casoDeUsoOrdemServico = casoDeUsoOrdemServico;
        this.servicoConsultaReferencias = servicoConsultaReferencias;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
        this.formatadorMoeda = formatadorMoeda;
    }

    @FXML
    public void initialize() {
        UtilComboBox.preencher(clienteCombo, servicoConsultaReferencias.clientes(), false);
        UtilComboBox.preencher(contratoCombo, servicoConsultaReferencias.contratos(), true);
        UtilComboBox.preencher(responsavelInternoCombo, servicoConsultaReferencias.funcionarios(), true);
        UtilComboBox.preencher(responsavelSecundarioCombo, servicoConsultaReferencias.funcionarios(), true);
        UtilComboBox.preencher(executadoPorCombo, servicoConsultaReferencias.funcionarios(), true);
        if (clienteCombo != null) {
            clienteCombo.valueProperty().addListener((observable, oldValue, newValue) ->
                    UtilComboBox.preencher(contratoCombo, servicoConsultaReferencias.contratosDoCliente(UtilComboBox.idSelecionado(clienteCombo)), true)
            );
        }
        LocalDate hoje = LocalDate.now();
        if (dataAgendadaPicker != null && dataAgendadaPicker.getValue() == null) {
            dataAgendadaPicker.setValue(hoje);
        }
        if (dataInicioPicker != null && dataInicioPicker.getValue() == null) {
            dataInicioPicker.setValue(hoje);
        }
        if (dataFimPicker != null && dataFimPicker.getValue() == null) {
            dataFimPicker.setValue(hoje);
        }
        if (statusField != null && statusField.getText().isBlank()) {
            statusField.setText(OrdemServico.OrdemServicoStatus.AGENDADA.name());
        }
        formatadorMoeda.aplicar(valorServicoField);
        construirCamposPdf();
        setFeedback("");
    }

    private void construirCamposPdf() {
        if (formularioPdfBox == null) {
            return;
        }
        manhaCheck = new CheckBox("Manhã");
        tardeCheck = new CheckBox("Tarde");
        horaInicioField = campoCurto("08:00");
        horaTerminoField = campoCurto("");
        etapaField = campoCurto("");
        etapaDeField = campoCurto("");
        produto1QtdField = new TextField();
        produto1CaldaField = new TextField();
        produto2QtdField = new TextField();
        produto2CaldaField = new TextField();
        aplicacaoGeralGrupo = new GrupoCheckboxes(OpcoesFormularioServico.OS_APLICACAO_GERAL);
        manutencaoGrupo = new GrupoCheckboxes(OpcoesFormularioServico.OS_MANUTENCAO);
        produtosGrupo = new GrupoProdutosQtde(OpcoesFormularioServico.OS_PRODUTO);

        HBox topo = new HBox(16,
                rotulado("Período", new HBox(10, manhaCheck, tardeCheck)),
                rotulado("Hora início", horaInicioField),
                rotulado("Hora término", horaTerminoField),
                rotulado("Etapa", etapaField),
                rotulado("De", etapaDeField));

        HBox diluicao1 = new HBox(16,
                rotulado("Produto utilizado 1 — quantidade", produto1QtdField),
                rotulado("Diluído em (calda)", produto1CaldaField));
        HBox diluicao2 = new HBox(16,
                rotulado("Produto utilizado 2 — quantidade", produto2QtdField),
                rotulado("Diluído em (calda)", produto2CaldaField));

        formularioPdfBox.getChildren().setAll(
                tituloSecao("Dados para o PDF da Ordem de Serviço"),
                topo,
                rotulado("Serviço aplicação geral", aplicacaoGeralGrupo.no()),
                rotulado("Serviço de manutenção", manutencaoGrupo.no()),
                rotulado("Produtos / iscas (QTDE)", produtosGrupo.no()),
                diluicao1,
                diluicao2);
    }

    @FXML
    private void onConfirmar() {
        try {
            ValidadorEntrada validador = ValidadorEntrada.nova();
            OpcaoId cliente = validador.selecao(UtilComboBox.selecionado(clienteCombo), "o cliente");
            String titulo = validador.texto(texto(tituloField), "o título da ordem de serviço");
            String tipoServico = validador.texto(texto(tipoServicoField), "o tipo de serviço");
            validador.validar();

            OpcaoId contrato = UtilComboBox.selecionado(contratoCombo);
            LocalDate dataAgendada = dataAgendadaPicker == null ? LocalDate.now() : dataAgendadaPicker.getValue();
            LocalDate dataInicio = dataInicioPicker == null || dataInicioPicker.getValue() == null ? dataAgendada : dataInicioPicker.getValue();
            LocalDate dataFim = dataFimPicker == null || dataFimPicker.getValue() == null ? dataAgendada : dataFimPicker.getValue();
            LocalDateTime inicio = dataInicio.atTime(8, 0);
            LocalDateTime fim = dataFim.atTime(18, 0);

            casoDeUsoOrdemServico.create(new CasoDeUsoOrdemServico.CreateOrdemServicoCommand(
                    UUID.randomUUID().toString(),
                    cliente.id(),
                    contrato == null ? "" : contrato.id(),
                    titulo,
                    descricaoField == null ? "" : descricaoField.getText(),
                    tipoServico,
                    parseEnum(OrdemServico.OrdemServicoStatus.class, statusField == null ? "" : statusField.getText(), OrdemServico.OrdemServicoStatus.AGENDADA),
                    dataAgendada.atStartOfDay(),
                    inicio,
                    fim,
                    chooseResponsible(),
                    UtilComboBox.idSelecionado(executadoPorCombo),
                    formatadorMoeda.valor(valorServicoField),
                    observacoesField == null ? "" : observacoesField.getText(),
                    montarDadosFormulario()
            ));
            gerenciadorNavegacao.navigateTo(VisaoAplicacao.SERVICE_ORDER);
            UtilJanela.fecharJanela(clienteCombo);
        } catch (Exception exception) {
            setFeedback(br.com.sigla.interfacegrafica.util.MensagensErro.descrever(exception));
        }
    }

    private DadosFormularioServico montarDadosFormulario() {
        if (formularioPdfBox == null || aplicacaoGeralGrupo == null) {
            return DadosFormularioServico.vazio();
        }
        DadosFormularioServico.Os os = new DadosFormularioServico.Os(
                manhaCheck.isSelected(),
                tardeCheck.isSelected(),
                horaInicioField.getText(),
                horaTerminoField.getText(),
                etapaField.getText(),
                etapaDeField.getText(),
                aplicacaoGeralGrupo.selecionados(),
                manutencaoGrupo.selecionados(),
                produtosGrupo.selecionados(),
                produto1QtdField.getText(),
                produto1CaldaField.getText(),
                produto2QtdField.getText(),
                produto2CaldaField.getText());
        return new DadosFormularioServico(os, DadosFormularioServico.Visita.vazio());
    }

    @FXML
    private void onCancelar() {
        UtilJanela.fecharJanela(clienteCombo);
    }

    private String texto(TextField campo) {
        return campo == null || campo.getText() == null ? "" : campo.getText();
    }

    private TextField campoCurto(String valor) {
        TextField campo = new TextField(valor);
        campo.setPrefWidth(90);
        return campo;
    }

    private Node rotulado(String titulo, Node campo) {
        Label rotulo = new Label(titulo);
        return new VBox(2, rotulo, campo);
    }

    private Node tituloSecao(String titulo) {
        Label rotulo = new Label(titulo);
        rotulo.setFont(Font.font("System", FontWeight.BOLD, 16));
        rotulo.setTextFill(Color.web("#00417e"));
        return rotulo;
    }

    private String chooseResponsible() {
        String principal = UtilComboBox.idSelecionado(responsavelInternoCombo);
        if (!principal.isBlank()) {
            return principal;
        }
        String secundario = UtilComboBox.idSelecionado(responsavelSecundarioCombo);
        if (!secundario.isBlank()) {
            return secundario;
        }
        return UtilComboBox.idSelecionado(executadoPorCombo);
    }

    private void setFeedback(String message) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message == null ? "" : message);
        }
    }
}

package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.dominio.servicos.DadosFormularioServico;
import br.com.sigla.dominio.servicos.OpcoesFormularioServico;
import br.com.sigla.dominio.servicos.OrdemServico;
import br.com.sigla.interfacegrafica.consulta.ContextoEdicaoOrdemServico;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaReferencias;
import br.com.sigla.interfacegrafica.formatador.FormatadorMascaraMoeda;
import br.com.sigla.interfacegrafica.modelo.OpcaoId;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.GrupoCheckboxes;
import br.com.sigla.interfacegrafica.util.GrupoProdutosQtde;
import br.com.sigla.interfacegrafica.util.TradutorInterface;
import br.com.sigla.interfacegrafica.util.UtilComboBox;
import br.com.sigla.interfacegrafica.util.UtilJanela;
import br.com.sigla.interfacegrafica.util.ValidadorEntrada;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
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
import java.util.List;
import java.util.UUID;

/**
 * Tela unica de Ordem de Servico, usada tanto para CRIAR quanto para EDITAR. O
 * modo e definido por {@link ContextoEdicaoOrdemServico}: assim a edicao oferece
 * exatamente os mesmos campos da criacao (cabecalho, datas, status e os campos
 * que alimentam o PDF), evitando divergencias entre as duas telas.
 */
@Component
public class ControladorNovaOrdemServico {

    private final CasoDeUsoOrdemServico casoDeUsoOrdemServico;
    private final ServicoConsultaReferencias servicoConsultaReferencias;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final FormatadorMascaraMoeda formatadorMoeda;
    private final ContextoEdicaoOrdemServico contextoEdicao;

    @FXML
    private Label tituloTela;
    @FXML
    private Button confirmarButton;
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
    private ComboBox<OrdemServico.OrdemServicoStatus> statusCombo;
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

    // Estado do modo edicao.
    private boolean modoEdicao;
    private OrdemServico ordemEmEdicao;

    public ControladorNovaOrdemServico(
            CasoDeUsoOrdemServico casoDeUsoOrdemServico,
            ServicoConsultaReferencias servicoConsultaReferencias,
            GerenciadorNavegacao gerenciadorNavegacao,
            FormatadorMascaraMoeda formatadorMoeda,
            ContextoEdicaoOrdemServico contextoEdicao
    ) {
        this.casoDeUsoOrdemServico = casoDeUsoOrdemServico;
        this.servicoConsultaReferencias = servicoConsultaReferencias;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
        this.formatadorMoeda = formatadorMoeda;
        this.contextoEdicao = contextoEdicao;
    }

    @FXML
    public void initialize() {
        modoEdicao = false;
        ordemEmEdicao = null;
        UtilComboBox.preencher(clienteCombo, servicoConsultaReferencias.clientes(), false);
        UtilComboBox.preencher(contratoCombo, servicoConsultaReferencias.contratos(), true);
        UtilComboBox.preencher(responsavelInternoCombo, servicoConsultaReferencias.funcionarios(), true);
        UtilComboBox.preencher(responsavelSecundarioCombo, servicoConsultaReferencias.funcionarios(), true);
        UtilComboBox.preencher(executadoPorCombo, servicoConsultaReferencias.funcionarios(), true);
        if (clienteCombo != null) {
            clienteCombo.valueProperty().addListener((observable, oldValue, newValue) ->
                    atualizarContratosDoCliente(UtilComboBox.idSelecionado(clienteCombo))
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
        if (statusCombo != null) {
            TradutorInterface.aplicar(statusCombo);
            statusCombo.getItems().setAll(OrdemServico.OrdemServicoStatus.values());
            statusCombo.getSelectionModel().select(OrdemServico.OrdemServicoStatus.AGENDADA);
        }
        formatadorMoeda.aplicar(valorServicoField);
        construirCamposPdf();
        setFeedback("");
        aplicarModoEdicao();
    }

    private void atualizarContratosDoCliente(String clienteId) {
        String contratoSelecionadoId = UtilComboBox.idSelecionado(contratoCombo);
        List<OpcaoId> contratos = servicoConsultaReferencias.contratosDoCliente(clienteId);
        if (contratos.isEmpty()) {
            contratos = servicoConsultaReferencias.contratos();
        }
        UtilComboBox.preencher(contratoCombo, contratos, true);
        UtilComboBox.selecionarPorId(contratoCombo, contratoSelecionadoId);
    }

    /**
     * Le o contexto (consumindo-o uma unica vez) e, se houver uma OS para editar,
     * pre-popula todos os campos da tela com os dados atuais da OS.
     */
    private void aplicarModoEdicao() {
        String id = contextoEdicao == null ? null : contextoEdicao.ordemServicoId();
        if (contextoEdicao != null) {
            contextoEdicao.limpar();
        }
        if (id == null || id.isBlank()) {
            return;
        }
        OrdemServico ordem = buscarOrdem(id);
        if (ordem == null) {
            return;
        }
        modoEdicao = true;
        ordemEmEdicao = ordem;

        // Cliente primeiro: ao selecionar, o contratoCombo e repreenchido com os
        // contratos do cliente; so entao selecionamos o contrato da OS.
        UtilComboBox.selecionarPorId(clienteCombo, ordem.clienteId());
        UtilComboBox.selecionarPorId(contratoCombo, ordem.contratoId());
        if (tituloField != null) {
            tituloField.setText(ordem.titulo());
        }
        if (tipoServicoField != null) {
            tipoServicoField.setText(ordem.tipoServico());
        }
        if (descricaoField != null) {
            descricaoField.setText(ordem.descricao());
        }
        if (statusCombo != null) {
            statusCombo.getSelectionModel().select(ordem.status());
        }
        UtilComboBox.selecionarPorId(responsavelInternoCombo, ordem.responsavelInternoId());
        UtilComboBox.selecionarPorId(executadoPorCombo, ordem.executadoPorId());
        formatadorMoeda.definir(valorServicoField, ordem.valorServico());
        if (observacoesField != null) {
            observacoesField.setText(ordem.observacoes());
        }
        LocalDate agendada = ordem.dataAgendada() == null ? LocalDate.now() : ordem.dataAgendada().toLocalDate();
        if (dataAgendadaPicker != null) {
            dataAgendadaPicker.setValue(agendada);
        }
        if (dataInicioPicker != null) {
            dataInicioPicker.setValue(ordem.dataInicio() == null ? agendada : ordem.dataInicio().toLocalDate());
        }
        if (dataFimPicker != null) {
            dataFimPicker.setValue(ordem.dataFim() == null ? agendada : ordem.dataFim().toLocalDate());
        }
        prefillCamposPdf(ordem.dadosFormulario().os());

        if (tituloTela != null) {
            tituloTela.setText("Editar Ordem de Serviço");
        }
        if (confirmarButton != null) {
            confirmarButton.setText("Salvar");
        }
        definirTituloJanela("Editar Ordem de Serviço");
    }

    /**
     * Ajusta o título da janela flutuante. No momento do {@code initialize} a cena/janela
     * ainda não existem, então registra um listener que aplica o título assim que a janela
     * estiver disponível (evitando o título "Nova Ordem de Serviço" durante a edição).
     */
    private void definirTituloJanela(String titulo) {
        if (tituloTela == null) {
            return;
        }
        Runnable aplicar = () -> {
            var scene = tituloTela.getScene();
            if (scene != null && scene.getWindow() instanceof javafx.stage.Stage stage) {
                stage.setTitle("S.I.G.L.A - " + titulo);
            }
        };
        var scene = tituloTela.getScene();
        if (scene != null && scene.getWindow() != null) {
            aplicar.run();
            return;
        }
        tituloTela.sceneProperty().addListener((obsScene, anterior, nova) -> {
            if (nova == null) {
                return;
            }
            if (nova.getWindow() != null) {
                aplicar.run();
            } else {
                nova.windowProperty().addListener((obsJanela, semJanela, comJanela) -> {
                    if (comJanela != null) {
                        aplicar.run();
                    }
                });
            }
        });
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

    private void prefillCamposPdf(DadosFormularioServico.Os os) {
        if (formularioPdfBox == null || aplicacaoGeralGrupo == null || os == null) {
            return;
        }
        manhaCheck.setSelected(os.manha());
        tardeCheck.setSelected(os.tarde());
        horaInicioField.setText(os.horaInicio());
        horaTerminoField.setText(os.horaTermino());
        etapaField.setText(os.etapa());
        etapaDeField.setText(os.etapaDe());
        produto1QtdField.setText(os.produto1Qtd());
        produto1CaldaField.setText(os.produto1Calda());
        produto2QtdField.setText(os.produto2Qtd());
        produto2CaldaField.setText(os.produto2Calda());
        aplicacaoGeralGrupo.marcar(os.aplicacaoGeral());
        manutencaoGrupo.marcar(os.manutencao());
        produtosGrupo.marcar(os.produtos());
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

            if (modoEdicao && ordemEmEdicao != null) {
                casoDeUsoOrdemServico.update(new CasoDeUsoOrdemServico.UpdateOrdemServicoCommand(
                        ordemEmEdicao.id(),
                        cliente.id(),
                        contrato == null ? "" : contrato.id(),
                        titulo,
                        descricaoField == null ? "" : descricaoField.getText(),
                        tipoServico,
                        null,
                        dataAgendada.atStartOfDay(),
                        chooseResponsible(),
                        UtilComboBox.idSelecionado(executadoPorCombo),
                        formatadorMoeda.valor(valorServicoField),
                        observacoesField == null ? "" : observacoesField.getText(),
                        montarDadosFormulario()
                ));
            } else {
                casoDeUsoOrdemServico.create(new CasoDeUsoOrdemServico.CreateOrdemServicoCommand(
                        UUID.randomUUID().toString(),
                        cliente.id(),
                        contrato == null ? "" : contrato.id(),
                        titulo,
                        descricaoField == null ? "" : descricaoField.getText(),
                        tipoServico,
                        statusSelecionado(OrdemServico.OrdemServicoStatus.AGENDADA),
                        dataAgendada.atStartOfDay(),
                        inicio,
                        fim,
                        chooseResponsible(),
                        UtilComboBox.idSelecionado(executadoPorCombo),
                        formatadorMoeda.valor(valorServicoField),
                        observacoesField == null ? "" : observacoesField.getText(),
                        montarDadosFormulario()
                ));
            }
            gerenciadorNavegacao.navigateTo(VisaoAplicacao.SERVICE_ORDER);
            UtilJanela.fecharJanela(clienteCombo);
        } catch (Exception exception) {
            setFeedback(br.com.sigla.interfacegrafica.util.MensagensErro.descrever(exception));
        }
    }

    private DadosFormularioServico montarDadosFormulario() {
        if (formularioPdfBox == null || aplicacaoGeralGrupo == null) {
            return modoEdicao && ordemEmEdicao != null ? ordemEmEdicao.dadosFormulario() : DadosFormularioServico.vazio();
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
        // Preserva a parte de Visita do formulario, que e editada em outra tela.
        DadosFormularioServico.Visita visita = modoEdicao && ordemEmEdicao != null
                ? ordemEmEdicao.dadosFormulario().visita()
                : DadosFormularioServico.Visita.vazio();
        return new DadosFormularioServico(os, visita);
    }

    @FXML
    private void onCancelar() {
        UtilJanela.fecharJanela(clienteCombo);
    }

    private OrdemServico buscarOrdem(String id) {
        return casoDeUsoOrdemServico.listAll().stream()
                .filter(ordem -> ordem.id().equals(id))
                .findFirst()
                .orElse(null);
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

    private OrdemServico.OrdemServicoStatus statusSelecionado(OrdemServico.OrdemServicoStatus padrao) {
        OrdemServico.OrdemServicoStatus selecionado = statusCombo == null ? null : statusCombo.getValue();
        return selecionado == null ? padrao : selecionado;
    }

    private void setFeedback(String message) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message == null ? "" : message);
        }
    }
}

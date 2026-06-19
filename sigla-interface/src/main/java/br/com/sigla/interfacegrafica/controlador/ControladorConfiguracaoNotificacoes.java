package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoConfiguracaoNotificacao;
import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoConfiguracaoNotificacao.ComandoSalvarConfiguracao;
import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoConfiguracaoNotificacao.ComandoTesteEnvio;
import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoEnvioNotificacao;
import br.com.sigla.aplicacao.notificacoes.porta.saida.PortaEnvioWhatsapp;
import br.com.sigla.dominio.notificacoes.CanalNotificacao;
import br.com.sigla.dominio.notificacoes.CatalogoVariaveisTemplate;
import br.com.sigla.dominio.notificacoes.Destinatario;
import br.com.sigla.dominio.notificacoes.FonteTelefone;
import br.com.sigla.dominio.notificacoes.Notificacao;
import br.com.sigla.dominio.notificacoes.NotificacaoConfiguracao;
import br.com.sigla.dominio.notificacoes.OrigemNotificacao;
import br.com.sigla.interfacegrafica.util.DialogoUi;
import br.com.sigla.interfacegrafica.util.MensagensErro;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Tela de configuracao de notificacoes por WhatsApp: cadastro/edicao de templates, ativar/desativar,
 * variaveis disponiveis, envio de teste e monitoramento das notificacoes agendadas (com reprocesso).
 */
@Component
public class ControladorConfiguracaoNotificacoes {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String AUTOR_PADRAO = "operador";

    private final CasoDeUsoConfiguracaoNotificacao casoDeUsoConfiguracao;
    private final CasoDeUsoEnvioNotificacao casoDeUsoEnvio;

    @FXML
    private Label title;
    @FXML
    private TableView<NotificacaoConfiguracao> configuracoesTable;
    @FXML
    private TextArea variaveisArea;
    @FXML
    private TableView<Notificacao> notificacoesTable;

    public ControladorConfiguracaoNotificacoes(
            CasoDeUsoConfiguracaoNotificacao casoDeUsoConfiguracao,
            CasoDeUsoEnvioNotificacao casoDeUsoEnvio
    ) {
        this.casoDeUsoConfiguracao = casoDeUsoConfiguracao;
        this.casoDeUsoEnvio = casoDeUsoEnvio;
    }

    @FXML
    public void initialize() {
        if (title != null) {
            title.setText("Notificações (WhatsApp)");
        }
        configurarColunasConfiguracao();
        configurarColunasNotificacao();
        preencherVariaveis();
        atualizarTudo();
    }

    @FXML
    private void onNovaConfiguracao() {
        abrirDialogoConfiguracao(null).ifPresent(comando -> {
            executar(() -> casoDeUsoConfiguracao.salvar(comando));
            atualizarConfiguracoes();
        });
    }

    @FXML
    private void onEditarConfiguracao() {
        NotificacaoConfiguracao selecionada = configuracaoSelecionada();
        if (selecionada == null) {
            return;
        }
        abrirDialogoConfiguracao(selecionada).ifPresent(comando -> {
            executar(() -> casoDeUsoConfiguracao.salvar(comando));
            atualizarConfiguracoes();
        });
    }

    @FXML
    private void onAlternarAtivo() {
        NotificacaoConfiguracao selecionada = configuracaoSelecionada();
        if (selecionada == null) {
            return;
        }
        executar(() -> {
            if (selecionada.ativo()) {
                casoDeUsoConfiguracao.desativar(selecionada.id());
            } else {
                casoDeUsoConfiguracao.ativar(selecionada.id());
            }
        });
        atualizarConfiguracoes();
    }

    @FXML
    private void onExcluirConfiguracao() {
        NotificacaoConfiguracao selecionada = configuracaoSelecionada();
        if (selecionada == null) {
            return;
        }
        if (!confirmar("Excluir a configuração \"" + selecionada.nome() + "\"?")) {
            return;
        }
        executar(() -> casoDeUsoConfiguracao.excluir(selecionada.id()));
        atualizarConfiguracoes();
    }

    @FXML
    private void onEnviarTeste() {
        abrirDialogoTeste().ifPresent(comando -> executar(() -> {
            PortaEnvioWhatsapp.ResultadoEnvio resultado = casoDeUsoConfiguracao.enviarTeste(comando);
            DialogoUi.informacao("Resultado do teste: " + resultado.situacao()
                    + (resultado.detalhe().isBlank() ? "" : " - " + resultado.detalhe()));
        }));
    }

    @FXML
    private void onAtualizar() {
        atualizarConfiguracoes();
    }

    @FXML
    private void onAtualizarNotificacoes() {
        atualizarNotificacoes();
    }

    @FXML
    private void onReprocessarFalhas() {
        executar(() -> {
            int reprocessadas = casoDeUsoEnvio.reprocessarFalhas();
            DialogoUi.informacao("Notificações reprocessadas: " + reprocessadas);
        });
        atualizarNotificacoes();
    }

    private void atualizarTudo() {
        atualizarConfiguracoes();
        atualizarNotificacoes();
    }

    private void atualizarConfiguracoes() {
        if (configuracoesTable != null) {
            configuracoesTable.getItems().setAll(casoDeUsoConfiguracao.listar());
        }
    }

    private void atualizarNotificacoes() {
        if (notificacoesTable != null) {
            notificacoesTable.getItems().setAll(casoDeUsoEnvio.listar());
        }
    }

    private void preencherVariaveis() {
        if (variaveisArea == null) {
            return;
        }
        StringBuilder texto = new StringBuilder();
        for (CatalogoVariaveisTemplate variavel : CatalogoVariaveisTemplate.todas()) {
            texto.append(variavel.marcador()).append("  ->  ").append(variavel.descricao()).append('\n');
        }
        variaveisArea.setText(texto.toString().trim());
        variaveisArea.setEditable(false);
    }

    private void configurarColunasConfiguracao() {
        if (configuracoesTable == null) {
            return;
        }
        coluna(configuracoesTable, "Evento", 200, c -> c.eventType().name());
        coluna(configuracoesTable, "Nome", 220, NotificacaoConfiguracao::nome);
        coluna(configuracoesTable, "Destinatário", 130, c -> c.destinatario().name());
        coluna(configuracoesTable, "Envio", 110, c -> c.automatico() ? "Automático" : "Manual");
        coluna(configuracoesTable, "Antecedência", 110, c -> c.diasAntecedencia() == null ? "-" : c.diasAntecedencia() + " dia(s)");
        coluna(configuracoesTable, "Status", 100, c -> c.ativo() ? "Ativo" : "Inativo");
    }

    private void configurarColunasNotificacao() {
        if (notificacoesTable == null) {
            return;
        }
        coluna(notificacoesTable, "Evento", 170, n -> n.type().name());
        coluna(notificacoesTable, "Destinatário", 240, this::descreverDestinatario);
        coluna(notificacoesTable, "Status", 110, n -> n.status().name());
        coluna(notificacoesTable, "Quando", 150, n -> DATA_HORA.format(n.momentoDisparo()));
        coluna(notificacoesTable, "Mensagem", 380, n -> resumir(n.message()));
    }

    private <T> void coluna(TableView<T> tabela, String titulo, double largura, Function<T, String> getter) {
        TableColumn<T, String> coluna = new TableColumn<>(titulo);
        coluna.setPrefWidth(largura);
        coluna.setCellValueFactory(dados -> new ReadOnlyStringWrapper(getter.apply(dados.getValue())));
        tabela.getColumns().add(coluna);
    }

    private String descreverDestinatario(Notificacao notificacao) {
        if (notificacao.destinatario() == null) {
            return "-";
        }
        return notificacao.destinatario().nome() + " (" + notificacao.destinatario().telefone() + ")";
    }

    private String resumir(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= 70 ? texto : texto.substring(0, 67) + "...";
    }

    private NotificacaoConfiguracao configuracaoSelecionada() {
        NotificacaoConfiguracao selecionada = configuracoesTable == null
                ? null
                : configuracoesTable.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            DialogoUi.informacao("Selecione uma configuração.");
        }
        return selecionada;
    }

    private Optional<ComandoSalvarConfiguracao> abrirDialogoConfiguracao(NotificacaoConfiguracao existente) {
        Dialog<ComandoSalvarConfiguracao> dialog = new Dialog<>();
        dialog.setTitle(existente == null ? "Nova configuração de notificação" : "Editar configuração");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<Notificacao.NotificacaoType> evento = new ComboBox<>();
        evento.getItems().setAll(Notificacao.NotificacaoType.values());
        TextField nome = new TextField();
        TextField titulo = new TextField();
        TextArea template = new TextArea();
        template.setPrefRowCount(4);
        ComboBox<Destinatario> destinatario = new ComboBox<>();
        destinatario.getItems().setAll(Destinatario.values());
        ComboBox<OrigemNotificacao> origem = new ComboBox<>();
        origem.getItems().setAll(OrigemNotificacao.values());
        ComboBox<FonteTelefone> fonte = new ComboBox<>();
        fonte.getItems().setAll(FonteTelefone.values());
        TextField telefoneInformado = new TextField();
        TextField dias = new TextField();
        CheckBox automatico = new CheckBox("Envio automático");
        CheckBox ativo = new CheckBox("Ativo");

        if (existente == null) {
            evento.getSelectionModel().select(Notificacao.NotificacaoType.VISIT_UPCOMING);
            destinatario.getSelectionModel().select(Destinatario.CLIENTE);
            origem.getSelectionModel().select(OrigemNotificacao.SISTEMA);
            fonte.getSelectionModel().select(FonteTelefone.CLIENTE);
            automatico.setSelected(true);
            ativo.setSelected(true);
        } else {
            evento.getSelectionModel().select(existente.eventType());
            nome.setText(existente.nome());
            titulo.setText(existente.titulo());
            template.setText(existente.templateMensagem());
            destinatario.getSelectionModel().select(existente.destinatario());
            origem.getSelectionModel().select(existente.origemTipo());
            fonte.getSelectionModel().select(existente.fonteTelefone());
            telefoneInformado.setText(existente.telefoneInformado());
            dias.setText(existente.diasAntecedencia() == null ? "" : String.valueOf(existente.diasAntecedencia()));
            automatico.setSelected(existente.automatico());
            ativo.setSelected(existente.ativo());
        }

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.addRow(0, new Label("Evento"), evento);
        grid.addRow(1, new Label("Nome"), nome);
        grid.addRow(2, new Label("Título"), titulo);
        grid.addRow(3, new Label("Mensagem (template)"), template);
        grid.addRow(4, new Label("Destinatário"), destinatario);
        grid.addRow(5, new Label("Origem"), origem);
        grid.addRow(6, new Label("Fonte do telefone"), fonte);
        grid.addRow(7, new Label("Telefone informado"), telefoneInformado);
        grid.addRow(8, new Label("Dias de antecedência"), dias);
        grid.add(automatico, 1, 9);
        grid.add(ativo, 1, 10);
        dialog.getDialogPane().setContent(grid);

        String criadoPor = existente == null ? AUTOR_PADRAO : existente.criadoPor();
        dialog.setResultConverter(botao -> {
            if (botao != ButtonType.OK) {
                return null;
            }
            return new ComandoSalvarConfiguracao(
                    existente == null ? null : existente.id(),
                    evento.getValue(),
                    nome.getText(),
                    titulo.getText(),
                    template.getText(),
                    destinatario.getValue(),
                    origem.getValue(),
                    CanalNotificacao.WHATSAPP_N8N,
                    fonte.getValue(),
                    telefoneInformado.getText(),
                    automatico.isSelected(),
                    parseInteiro(dias.getText()),
                    ativo.isSelected(),
                    criadoPor);
        });
        return dialog.showAndWait();
    }

    private Optional<ComandoTesteEnvio> abrirDialogoTeste() {
        NotificacaoConfiguracao base = configuracoesTable == null
                ? null
                : configuracoesTable.getSelectionModel().getSelectedItem();

        Dialog<ComandoTesteEnvio> dialog = new Dialog<>();
        dialog.setTitle("Enviar teste");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField telefone = new TextField();
        telefone.setPromptText("Ex.: (11) 99999-9999");
        TextArea template = new TextArea(base == null ? "Mensagem de teste do SIGLA." : base.templateMensagem());
        template.setPrefRowCount(4);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.addRow(0, new Label("Telefone do teste"), telefone);
        grid.addRow(1, new Label("Mensagem (template)"), template);
        dialog.getDialogPane().setContent(grid);

        Map<String, String> amostra = new LinkedHashMap<>();
        for (CatalogoVariaveisTemplate variavel : CatalogoVariaveisTemplate.todas()) {
            amostra.put(variavel.chave(), "[" + variavel.descricao() + "]");
        }
        Notificacao.NotificacaoType evento = base == null ? Notificacao.NotificacaoType.MANUAL : base.eventType();
        String titulo = base == null ? "Teste" : base.titulo();

        dialog.setResultConverter(botao -> botao == ButtonType.OK
                ? new ComandoTesteEnvio(telefone.getText(), titulo, template.getText(), amostra, evento)
                : null);
        return dialog.showAndWait();
    }

    private Integer parseInteiro(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean confirmar(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, mensagem, ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(null);
        return alert.showAndWait().filter(botao -> botao == ButtonType.OK).isPresent();
    }

    private void executar(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception exception) {
            DialogoUi.informacao(MensagensErro.descrever(exception));
        }
    }
}

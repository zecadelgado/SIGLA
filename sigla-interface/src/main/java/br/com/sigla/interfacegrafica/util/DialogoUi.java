package br.com.sigla.interfacegrafica.util;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Diálogos e notificações padronizados do SIGLA.
 *
 * <p>Aplica a identidade visual do app (paleta azul, Segoe UI, cabeçalho colorido
 * com ícone) aos {@code Alert}/{@code Dialog} do JavaFX, que por padrão têm aparência
 * genérica. O estilo fica em {@code css/dialogos.css}; este utilitário monta o
 * cabeçalho e anexa as folhas de estilo.
 *
 * <ul>
 *   <li>{@link #erro}, {@link #aviso}, {@link #informacao} — diálogos modais.</li>
 *   <li>{@link #sucesso} — toast não-bloqueante (cai para diálogo se não houver janela).</li>
 *   <li>{@link #confirmar} — confirmação Sim/Não estilizada.</li>
 *   <li>{@link #estilizar} — aplica o mesmo visual a diálogos de formulário existentes.</li>
 * </ul>
 */
public final class DialogoUi {

    /** Tipo do diálogo: define a cor de acento, o ícone e o título padrão. */
    public enum Tipo {
        ERRO("erro", "✕", "Erro"),
        AVISO("aviso", "⚠", "Atenção"),
        INFO("info", "ℹ", "Informação"),
        SUCESSO("sucesso", "✓", "Sucesso"),
        CONFIRMAR("confirmar", "?", "Confirmação");

        private final String styleClass;
        private final String icone;
        private final String tituloPadrao;

        Tipo(String styleClass, String icone, String tituloPadrao) {
            this.styleClass = styleClass;
            this.icone = icone;
            this.tituloPadrao = tituloPadrao;
        }
    }

    private static final String CSS_BASE = "/css/base.css";
    private static final String CSS_DIALOGOS = "/css/dialogos.css";

    private DialogoUi() {
    }

    public static void informacao(String mensagem) {
        exibir(Tipo.INFO, Tipo.INFO.tituloPadrao, mensagem);
    }

    public static void aviso(String mensagem) {
        exibir(Tipo.AVISO, Tipo.AVISO.tituloPadrao, mensagem);
    }

    public static void erro(String mensagem) {
        exibir(Tipo.ERRO, Tipo.ERRO.tituloPadrao, mensagem);
    }

    /**
     * Feedback de sucesso não-bloqueante (toast). Se não houver janela visível
     * para ancorar o toast, cai para um diálogo modal verde.
     */
    public static void sucesso(String mensagem) {
        if (!ToastNotificacao.sucesso(mensagem)) {
            exibir(Tipo.SUCESSO, Tipo.SUCESSO.tituloPadrao, mensagem);
        }
    }

    /** Confirmação estilizada (Sim/Não). Retorna {@code true} se o usuário confirmar. */
    public static boolean confirmar(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                textoOuPadrao(mensagem, "Deseja confirmar esta ação?"));
        ButtonType sim = new ButtonType("Sim", ButtonType.OK.getButtonData());
        ButtonType nao = new ButtonType("Não", ButtonType.CANCEL.getButtonData());
        alert.getButtonTypes().setAll(sim, nao);
        String tituloFinal = titulo != null && !titulo.isBlank() ? titulo : Tipo.CONFIRMAR.tituloPadrao;
        alert.setTitle(tituloFinal);
        aplicar(alert.getDialogPane(), Tipo.CONFIRMAR, tituloFinal);
        return alert.showAndWait().filter(b -> b == sim).isPresent();
    }

    /**
     * Aplica o visual do SIGLA a um {@code Dialog} de formulário já montado
     * (folhas de estilo + cabeçalho colorido), usando o título da janela.
     */
    public static void estilizar(Dialog<?> dialog) {
        estilizar(dialog, Tipo.INFO);
    }

    /**
     * Variante com tipo explícito. O título do cabeçalho acompanha o
     * {@code titleProperty()} do diálogo, então pode ser chamada logo após criá-lo,
     * antes mesmo de {@code setTitle(...)}.
     */
    public static void estilizar(Dialog<?> dialog, Tipo tipo) {
        DialogPane pane = dialog.getDialogPane();
        aplicarEstilo(pane, tipo);

        Label tituloLabel = rotuloTitulo();
        tituloLabel.textProperty().bind(Bindings.createStringBinding(
                () -> dialog.getTitle() != null && !dialog.getTitle().isBlank()
                        ? dialog.getTitle() : tipo.tituloPadrao,
                dialog.titleProperty()));

        // O texto que o controlador define via setHeaderText(...) vira o subtítulo do
        // cabeçalho (caso contrário ele seria descartado por usarmos um header custom).
        Label subtituloLabel = rotuloSubtitulo();
        subtituloLabel.textProperty().bind(dialog.getDialogPane().headerTextProperty());

        pane.setGraphic(null);
        pane.setHeader(montarCabecalho(tipo, tituloLabel, subtituloLabel));

        // Conteúdo de formulário (grid, etc.) costuma vir sem padding, colado no cabeçalho
        // e nas bordas. Damos um respiro assim que o conteúdo é definido pelo controlador.
        pane.contentProperty().addListener((obs, anterior, atual) -> aplicarRespiroConteudo(atual));
        aplicarRespiroConteudo(pane.getContent());
    }

    // ---------- internos ----------

    private static void exibir(Tipo tipo, String titulo, String mensagem) {
        Alert.AlertType alertType = switch (tipo) {
            case ERRO -> Alert.AlertType.ERROR;
            case AVISO -> Alert.AlertType.WARNING;
            default -> Alert.AlertType.INFORMATION;
        };
        Alert alert = new Alert(alertType, textoOuPadrao(mensagem, "Ação não realizada."), ButtonType.OK);
        alert.setTitle(titulo);
        aplicar(alert.getDialogPane(), tipo, titulo);
        alert.showAndWait();
    }

    /** Anexa folhas de estilo, classes e cabeçalho colorido (título fixo) a um DialogPane. */
    private static void aplicar(DialogPane pane, Tipo tipo, String titulo) {
        aplicarEstilo(pane, tipo);
        Label tituloLabel = rotuloTitulo();
        tituloLabel.setText(titulo != null && !titulo.isBlank() ? titulo : tipo.tituloPadrao);
        pane.setHeaderText(null);
        pane.setGraphic(null);
        pane.setHeader(montarCabecalho(tipo, tituloLabel, null));
    }

    /** Garante um espaçamento confortável em conteúdos sem padding próprio (formulários). */
    private static void aplicarRespiroConteudo(javafx.scene.Node conteudo) {
        if (conteudo instanceof Region regiao && Insets.EMPTY.equals(regiao.getPadding())) {
            regiao.setPadding(new Insets(20, 26, 22, 26));
        }
    }

    /** Anexa folhas de estilo e as classes de tipo, sem mexer no cabeçalho. */
    private static void aplicarEstilo(DialogPane pane, Tipo tipo) {
        adicionarStylesheet(pane, CSS_BASE);
        adicionarStylesheet(pane, CSS_DIALOGOS);
        if (!pane.getStyleClass().contains("dialogo-sigla")) {
            pane.getStyleClass().add("dialogo-sigla");
        }
        pane.getStyleClass().removeAll("erro", "aviso", "info", "sucesso", "confirmar");
        pane.getStyleClass().add(tipo.styleClass);
    }

    private static Label rotuloTitulo() {
        Label tituloLabel = new Label();
        tituloLabel.getStyleClass().add("dialogo-titulo");
        return tituloLabel;
    }

    private static Label rotuloSubtitulo() {
        Label subtituloLabel = new Label();
        subtituloLabel.getStyleClass().add("dialogo-subtitulo");
        subtituloLabel.setWrapText(true);
        // Some do layout quando não há texto, sem deixar espaço vazio no cabeçalho.
        subtituloLabel.visibleProperty().bind(subtituloLabel.textProperty().isNotEmpty());
        subtituloLabel.managedProperty().bind(subtituloLabel.visibleProperty());
        return subtituloLabel;
    }

    private static Region montarCabecalho(Tipo tipo, Label tituloLabel, Label subtituloLabel) {
        Label icone = new Label(tipo.icone);
        icone.getStyleClass().add("dialogo-icone");

        VBox textos = new VBox(tituloLabel);
        textos.setAlignment(Pos.CENTER_LEFT);
        textos.setSpacing(2);
        if (subtituloLabel != null) {
            textos.getChildren().add(subtituloLabel);
        }
        HBox.setHgrow(textos, Priority.ALWAYS);

        HBox cabecalho = new HBox(icone, textos);
        cabecalho.getStyleClass().add("dialogo-cabecalho");
        cabecalho.setAlignment(Pos.CENTER_LEFT);
        cabecalho.setMaxWidth(Double.MAX_VALUE);
        return cabecalho;
    }

    private static void adicionarStylesheet(DialogPane pane, String recurso) {
        String url = DialogoUi.class.getResource(recurso).toExternalForm();
        if (!pane.getStylesheets().contains(url)) {
            pane.getStylesheets().add(url);
        }
    }

    private static String textoOuPadrao(String mensagem, String padrao) {
        return mensagem == null || mensagem.isBlank() ? padrao : mensagem;
    }
}

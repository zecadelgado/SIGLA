package br.com.sigla.interfacegrafica.navegacao;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ResourceBundle;

@Component
public class GerenciadorNavegacao {

    private final ConfigurableApplicationContext context;
    private final br.com.sigla.interfacegrafica.aplicativo.FluxoAplicacao fluxoAplicacao;
    private VisaoAplicacao currentView;
    private BorderPane shellContentHost;
    // Onde as telas do shell sao injetadas. E uma camada empilhavel (StackPane) para que a
    // tela de carregamento possa ficar por cima do conteudo. Quando ausente, recai no setCenter.
    private Pane shellContentSlot;
    private Node shellMenu;

    public GerenciadorNavegacao(
            ConfigurableApplicationContext context,
            br.com.sigla.interfacegrafica.aplicativo.FluxoAplicacao fluxoAplicacao
    ) {
        this.context = context;
        this.fluxoAplicacao = fluxoAplicacao;
    }

    public void navigateTo(VisaoAplicacao view) {
        try {
            if (view.isSobreposta()) {
                fluxoAplicacao.showView(view);
                currentView = view;
                return;
            }
            if (view.isShellContent() && shellContentSlot != null) {
                shellContentSlot.getChildren().setAll(loadShellContent(view));
                return;
            }
            if (view.isShellContent() && shellContentHost != null) {
                shellContentHost.setCenter(loadShellContent(view));
                return;
            }
            fluxoAplicacao.showView(view);
            currentView = view;
        } catch (RuntimeException erro) {
            // Falha ao carregar/inicializar a tela (ex.: erro de banco) nao pode quebrar a
            // navegacao em silencio: mostra a causa em vez de deixar a tela morta.
            new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR,
                    br.com.sigla.interfacegrafica.util.MensagensErro.descrever("Nao foi possivel abrir a tela:", erro),
                    javafx.scene.control.ButtonType.OK
            ).showAndWait();
        }
    }

    public void registerShellContentHost(BorderPane shellContentHost) {
        this.shellContentHost = shellContentHost;
    }

    public void registerShellContentSlot(Pane shellContentSlot) {
        this.shellContentSlot = shellContentSlot;
    }

    public void registerShellMenu(Node shellMenu) {
        this.shellMenu = shellMenu;
    }

    public void alternarMenuLateral() {
        if (shellContentHost == null || shellMenu == null) {
            return;
        }
        if (shellContentHost.getLeft() == null) {
            shellContentHost.setLeft(shellMenu);
        } else {
            shellContentHost.setLeft(null);
        }
    }

    public Node loadShellContent(VisaoAplicacao view) {
        if (!view.isShellContent()) {
            throw new IllegalArgumentException("View is not shell content: " + view);
        }
        Node content = extractShellContent(loadView(view));
        ScrollPane container = garantirRolagem(content);
        configureResponsiveContent(container);
        ajustarPreenchimentoVertical(container);
        currentView = view;
        return container;
    }

    // As telas tem estruturas de FXML diferentes: algumas ja vem dentro de um
    // ScrollPane, outras tem AnchorPane/VBox como raiz com tamanhos fixos. Para
    // que todas abram com o mesmo tamanho e rolem quando o conteudo passa da area
    // visivel, padronizamos tudo dentro de um ScrollPane unico na navegacao.
    private ScrollPane garantirRolagem(Node content) {
        if (content instanceof ScrollPane scrollPane) {
            return scrollPane;
        }
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scrollPane;
    }

    // O conteudo ocupa no minimo a altura visivel do shell. Assim a tela nao
    // "sobra" cinza quando e menor que a janela e nao "muda de tamanho" quando
    // os dados chegam de forma assincrona: em vez de redimensionar a tela inteira,
    // ela apenas passa a rolar.
    private void ajustarPreenchimentoVertical(ScrollPane scrollPane) {
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        if (scrollPane.getContent() instanceof Region regiao) {
            scrollPane.viewportBoundsProperty().addListener((observavel, anterior, atual) ->
                    regiao.setMinHeight(atual.getHeight()));
        }
    }

    public Node loadView(VisaoAplicacao view) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("i18n.mensagens");
            FXMLLoader loader = new FXMLLoader(getClass().getResource(view.fxmlPath()), bundle);
            loader.setControllerFactory(context::getBean);
            return loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot navigate to " + view, e);
        }
    }

    public VisaoAplicacao currentView() {
        return currentView;
    }

    private Node extractShellContent(Node viewRoot) {
        if (viewRoot instanceof ScrollPane scrollPane && scrollPane.getContent() != null) {
            Node extracted = extractShellContent(scrollPane.getContent());
            return extracted == scrollPane.getContent() ? viewRoot : extracted;
        }
        if (viewRoot instanceof AnchorPane anchorPane && anchorPane.getChildren().size() == 1) {
            Node child = anchorPane.getChildren().getFirst();
            if (child instanceof HBox) {
                return extractContentFromMenuLayout((HBox) child);
            }
            if (child instanceof BorderPane) {
                return extractContentFromMenuLayout((BorderPane) child);
            }
        }
        if (viewRoot instanceof HBox hBox) {
            return extractContentFromMenuLayout(hBox);
        }
        if (viewRoot instanceof BorderPane borderPane) {
            return extractContentFromMenuLayout(borderPane);
        }
        return viewRoot;
    }

    void configureResponsiveContent(Node content) {
        if (content instanceof ScrollPane scrollPane) {
            scrollPane.setFitToWidth(true);
            // Mantem rolagem vertical: fitToHeight=true impedia o scroll e cortava
            // telas mais altas que a viewport (ex.: Dashboard em tela cheia).
            scrollPane.setFitToHeight(false);
            configureResponsiveContent(scrollPane.getContent());
        }
        if (content instanceof Region region) {
            region.setMinWidth(0.0);
            region.setMinHeight(0.0);
            region.setMaxWidth(Double.MAX_VALUE);
            region.setMaxHeight(Double.MAX_VALUE);
        }
    }

    private Node extractContentFromMenuLayout(HBox hBox) {
        if (hBox.getChildren().size() < 2 || !looksLikeEmbeddedMenu(hBox.getChildren().getFirst())) {
            return hBox;
        }
        Node content = hBox.getChildren().get(1);
        hBox.getChildren().remove(content);
        return content;
    }

    private Node extractContentFromMenuLayout(BorderPane borderPane) {
        Node left = borderPane.getLeft();
        Node center = borderPane.getCenter();
        if (center == null || !looksLikeEmbeddedMenu(left)) {
            return borderPane;
        }
        borderPane.setCenter(null);
        return center;
    }

    private boolean looksLikeEmbeddedMenu(Node node) {
        if (!(node instanceof Region region)) {
            return false;
        }
        return region.getPrefWidth() >= 220.0 && region.getPrefWidth() <= 340.0;
    }
}


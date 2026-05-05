package br.com.sigla.interfacegrafica.aplicativo;

import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.ResourceBundle;

@Component
public class FluxoAplicacao {

    private static final double MAIN_WIDTH_RATIO = 0.96;
    private static final double MAIN_HEIGHT_RATIO = 0.94;
    private static final double FLOATING_MAX_WIDTH_RATIO = 0.62;
    private static final double FLOATING_MAX_HEIGHT_RATIO = 0.84;
    private static final double LOGIN_MAX_WIDTH_RATIO = 0.55;
    private static final double LOGIN_MAX_HEIGHT_RATIO = 0.55;

    private final ConfigurableApplicationContext context;
    private final SessaoLocalAplicacao sessaoLocalAplicacao;
    private final Map<VisaoAplicacao, Stage> floatingStages = new EnumMap<>(VisaoAplicacao.class);
    private Stage stage;

    public FluxoAplicacao(
            ConfigurableApplicationContext context,
            SessaoLocalAplicacao sessaoLocalAplicacao
    ) {
        this.context = context;
        this.sessaoLocalAplicacao = sessaoLocalAplicacao;
    }

    public void attachStage(Stage stage) {
        this.stage = stage;
    }

    public void showLogin() {
        sessaoLocalAplicacao.logout();
        closeFloatingStages();
        showView(VisaoAplicacao.LOGIN);
    }

    public void showShell() {
        showView(VisaoAplicacao.SHELL);
    }

    public void showView(VisaoAplicacao view) {
        if (view != VisaoAplicacao.LOGIN && !sessaoLocalAplicacao.isAuthenticated()) {
            throw new IllegalStateException("Authentication required before opening the application");
        }
        if (view.isSobreposta()) {
            showFloatingView(view);
            return;
        }
        setRoot(loadRoot(view));
        configurePrimaryStage(view);
    }

    private Parent loadRoot(VisaoAplicacao view) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("i18n.mensagens");
            FXMLLoader loader = new FXMLLoader(getClass().getResource(view.fxmlPath()), bundle);
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            adaptRootToAvailableSpace(root);
            return root;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load view " + view, exception);
        }
    }

    private void setRoot(Parent root) {
        if (stage == null) {
            throw new IllegalStateException("Primary stage not attached");
        }

        if (stage.getScene() == null) {
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/base.css").toExternalForm());
            stage.setScene(scene);
            return;
        }

        stage.getScene().setRoot(root);
    }

    private void showFloatingView(VisaoAplicacao view) {
        Stage existingStage = floatingStages.get(view);
        if (existingStage != null && existingStage.isShowing()) {
            existingStage.toFront();
            existingStage.requestFocus();
            return;
        }

        Parent root = loadRoot(view);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/base.css").toExternalForm());

        Stage floatingStage = new Stage();
        floatingStage.initOwner(stage);
        floatingStage.initModality(Modality.NONE);
        floatingStage.initStyle(StageStyle.UTILITY);
        floatingStage.setResizable(true);
        floatingStage.setTitle("S.I.G.L.A - " + view.tituloJanela());
        floatingStage.setScene(scene);
        floatingStage.setOnHidden(event -> floatingStages.remove(view));

        resizeFloatingStage(floatingStage, root);

        floatingStages.put(view, floatingStage);
        floatingStage.show();
        floatingStage.toFront();
    }

    private void configurePrimaryStage(VisaoAplicacao view) {
        if (stage == null || stage.getScene() == null) {
            return;
        }

        Rectangle2D screenBounds = resolveBounds(stage);
        stage.setResizable(true);
        stage.setTitle(view == VisaoAplicacao.LOGIN ? "S.I.G.L.A - Login" : "S.I.G.L.A");

        if (view == VisaoAplicacao.LOGIN) {
            resizeStageToContent(stage, stage.getScene().getRoot(), screenBounds, LOGIN_MAX_WIDTH_RATIO, LOGIN_MAX_HEIGHT_RATIO);
        } else {
            double width = clamp(screenBounds.getWidth() * MAIN_WIDTH_RATIO, 960, screenBounds.getWidth());
            double height = clamp(screenBounds.getHeight() * MAIN_HEIGHT_RATIO, 640, screenBounds.getHeight());
            stage.setWidth(width);
            stage.setHeight(height);
        }

        centerStage(stage, screenBounds);
    }

    private void resizeFloatingStage(Stage floatingStage, Parent root) {
        Rectangle2D screenBounds = resolveBounds(stage);
        resizeStageToContent(floatingStage, root, screenBounds, FLOATING_MAX_WIDTH_RATIO, FLOATING_MAX_HEIGHT_RATIO);

        long visibleFloatingStages = floatingStages.values().stream()
                .filter(Stage::isShowing)
                .count();

        double x = clamp(stage.getX() + 48 + (visibleFloatingStages * 24), screenBounds.getMinX(), screenBounds.getMaxX() - floatingStage.getWidth());
        double y = clamp(stage.getY() + 48 + (visibleFloatingStages * 24), screenBounds.getMinY(), screenBounds.getMaxY() - floatingStage.getHeight());

        floatingStage.setX(x);
        floatingStage.setY(y);
    }

    private void resizeStageToContent(Stage targetStage, Parent root, Rectangle2D screenBounds, double maxWidthRatio, double maxHeightRatio) {
        double preferredWidth = resolvePreferredSize(root.prefWidth(-1), 720);
        double preferredHeight = resolvePreferredSize(root.prefHeight(-1), 480);
        double width = clamp(preferredWidth, 420, screenBounds.getWidth() * maxWidthRatio);
        double height = clamp(preferredHeight, 260, screenBounds.getHeight() * maxHeightRatio);
        targetStage.setWidth(width);
        targetStage.setHeight(height);
    }

    private void adaptRootToAvailableSpace(Parent root) {
        if (!(root instanceof AnchorPane anchorPane) || anchorPane.getChildren().size() != 1) {
            return;
        }

        Node content = anchorPane.getChildren().get(0);
        AnchorPane.setTopAnchor(content, 0.0);
        AnchorPane.setRightAnchor(content, 0.0);
        AnchorPane.setBottomAnchor(content, 0.0);
        AnchorPane.setLeftAnchor(content, 0.0);
    }

    private Rectangle2D resolveBounds(Stage referenceStage) {
        if (referenceStage != null) {
            double width = referenceStage.getWidth() > 0 ? referenceStage.getWidth() : 1;
            double height = referenceStage.getHeight() > 0 ? referenceStage.getHeight() : 1;
            var matchingScreens = Screen.getScreensForRectangle(referenceStage.getX(), referenceStage.getY(), width, height);
            if (!matchingScreens.isEmpty()) {
                return matchingScreens.getFirst().getVisualBounds();
            }
        }
        return Screen.getPrimary().getVisualBounds();
    }

    private void centerStage(Stage targetStage, Rectangle2D bounds) {
        targetStage.setX(bounds.getMinX() + ((bounds.getWidth() - targetStage.getWidth()) / 2));
        targetStage.setY(bounds.getMinY() + ((bounds.getHeight() - targetStage.getHeight()) / 2));
    }

    private double resolvePreferredSize(double preferredSize, double fallbackValue) {
        return preferredSize > 0 ? preferredSize : fallbackValue;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    private void closeFloatingStages() {
        for (Stage floatingStage : new ArrayList<>(floatingStages.values())) {
            floatingStage.close();
        }
        floatingStages.clear();
    }
}

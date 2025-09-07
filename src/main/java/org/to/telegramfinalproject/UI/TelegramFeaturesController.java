package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class TelegramFeaturesController {

    @FXML private Pane overlayBackground;
    @FXML private ScrollPane featureScroll;
    @FXML private StackPane rootOverlay;
    @FXML private BorderPane contentCard;

    @FXML
    public void initialize() {
        // Clicking background closes overlay
        overlayBackground.setOnMouseClicked( e -> {
            MainController.getInstance().closeOverlay(rootOverlay);
        });

        // Smooth scroll feel
        featureScroll.getStylesheets().add(getClass().getResource("/org/to/telegramfinalproject/CSS/scrollpane.css").toExternalForm());
        featureScroll.setPannable(true);
        featureScroll.setFitToWidth(true);
        featureScroll.setFitToHeight(false);
        featureScroll.getContent().setOnScroll(event -> {
            double deltaY = event.getDeltaY() * 0.003;
            featureScroll.setVvalue(featureScroll.getVvalue() - deltaY);
        });

        // Register scene for ThemeManager → stylesheet swap will handle colors/icons
        Platform.runLater(() -> {
            if (contentCard.getScene() != null) {
                ThemeManager.getInstance().registerScene(contentCard.getScene());
            }
        });
    }
}

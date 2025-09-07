package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class TelegramQnAController {

    @FXML private StackPane rootOverlay;
    @FXML private Pane overlayBackground;
    @FXML private ScrollPane qnaScroll;
    @FXML private BorderPane contentCard;

    @FXML
    private void initialize() {
        // Clicking outside closes overlay
        overlayBackground.setOnMouseClicked(e ->
                MainController.getInstance().closeOverlay(rootOverlay)
        );

        // Smooth scroll feel
        qnaScroll.getStylesheets().add(getClass().getResource("/org/to/telegramfinalproject/CSS/scrollpane.css").toExternalForm());
        qnaScroll.setPannable(true);
        qnaScroll.setFitToWidth(true);
        qnaScroll.setFitToHeight(false);
        qnaScroll.getContent().setOnScroll(event -> {
            double deltaY = event.getDeltaY() * 0.003;
            qnaScroll.setVvalue(qnaScroll.getVvalue() - deltaY);
        });

        // Register scene for ThemeManager → stylesheet swap will handle colors/icons
        Platform.runLater(() -> {
            if (contentCard.getScene() != null) {
                ThemeManager.getInstance().registerScene(contentCard.getScene());
            }
        });
    }
}
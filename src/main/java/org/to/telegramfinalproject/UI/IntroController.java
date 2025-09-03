package org.to.telegramfinalproject.UI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IntroController {
    @FXML private StackPane imagePane;
    @FXML private Label caption;
    @FXML private HBox dotContainer;
    @FXML private Button startButton;

    private List<Image> images = new ArrayList<>();
    private List<String> captions = new ArrayList<>();
    private int currentIndex = 0;

    private final int width = 200;
    private final int height = 200;

    @FXML
    public void initialize() {
        loadSlides();
        setupDots();
        showSlide(currentIndex);

        startAutoSlide();
    }

    private void loadSlides() {
        images.add(new Image(getClass().getResource("/org/to/telegramfinalproject/Images/telegram_icon.png").toExternalForm()));
        captions.add("🚀 The world's fastest messaging app.\nIt is free and secure.");

        images.add(new Image(getClass().getResource("/org/to/telegramfinalproject/Images/fast.png").toExternalForm()));
        captions.add("🚀 Telegram delivers messages \nfastest than any other application.");

        images.add(new Image(getClass().getResource("/org/to/telegramfinalproject/Images/free.png").toExternalForm()));
        captions.add("🎁 Telegram is free forever. No ads.\nNo subscription fees.");

        images.add(new Image(getClass().getResource("/org/to/telegramfinalproject/Images/secure.png").toExternalForm()));
        captions.add("🔒 Telegram keeps your messages \nsafe from hacker attacks.");
    }

    private void setupDots() {
        dotContainer.getChildren().clear();
        for (int i = 0; i < images.size(); i++) {
            Circle dot = new Circle(5, Color.LIGHTGRAY);
            dotContainer.getChildren().add(dot);
        }
    }

    private void updateDots() {
        for (int i = 0; i < dotContainer.getChildren().size(); i++) {
            Circle dot = (Circle) dotContainer.getChildren().get(i);
            if (i == currentIndex) {
                dot.setFill(getSlideColor(i));
            } else {
                dot.setFill(Color.LIGHTGRAY);
            }
        }
    }

    private Color getSlideColor(int index) {
        return switch (index) {
            case 0 -> Color.DODGERBLUE;  // Blue for Telegram logo
            case 1 -> Color.FIREBRICK;   // Red slide
            case 2 -> Color.GOLDENROD;   // Yellow slide
            case 3 -> Color.SEAGREEN;    // Green slide
            default -> Color.GRAY;
        };
    }

    private void showSlide(int index) {
        imagePane.getChildren().clear();

        ImageView imageView = new ImageView(images.get(index));
        imageView.setFitWidth(180);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(true);

        // Clip the image into a circle
        Circle clip = new Circle(90, 90, 80); // centerX, centerY, radius
        imageView.setClip(clip);

        imagePane.getChildren().add(imageView);
        caption.setText(captions.get(index));
        updateDots();
    }


    private void startAutoSlide() {
        Thread slider = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {}

                javafx.application.Platform.runLater(() -> {
                    currentIndex = (currentIndex + 1) % images.size();
                    showSlide(currentIndex);
                });
            }
        });
        slider.setDaemon(true);
        slider.start();
    }

    @FXML
    private void handleStartMessaging(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/to/telegramfinalproject/Fxml/login_view.fxml"));
            Scene loginScene = new Scene(loader.load());

            // Get the current stage and its dimensions
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();

            // Set the new scene and apply the previous size
            stage.setScene(loginScene);
            stage.setWidth(currentWidth);
            stage.setHeight(currentHeight);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

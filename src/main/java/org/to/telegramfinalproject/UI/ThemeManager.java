package org.to.telegramfinalproject.UI;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;

import java.util.ArrayList;
import java.util.List;

public class ThemeManager {
    private static final ThemeManager instance = new ThemeManager();

    private final BooleanProperty darkMode = new SimpleBooleanProperty(false);
    private final List<Scene> registeredScenes = new ArrayList<>();

    private ThemeManager() {
        // Whenever darkMode changes, update all registered scenes
        darkMode.addListener((obs, oldVal, newVal) -> applyThemeToAll());
    }

    public static ThemeManager getInstance() {
        return instance;
    }

    public BooleanProperty darkModeProperty() {
        return darkMode;
    }

    public boolean isDarkMode() {
        return darkMode.get();
    }

    public void setDarkMode(boolean dark) {
        darkMode.set(dark);
    }

    public void registerScene(Scene scene) {
        if (!registeredScenes.contains(scene)) {
            registeredScenes.add(scene);
            applyTheme(scene); // Apply current theme immediately
        }
    }

    public void applyThemeToAll() {
        for (Scene scene : registeredScenes) {
            applyTheme(scene);
        }
    }

    private void applyTheme(Scene scene) {
        scene.getStylesheets().clear();
        if (isDarkMode()) {
            scene.getStylesheets().add(getClass().getResource("/org/to/telegramfinalproject/CSS/dark_theme.css").toExternalForm());
        } else {
            scene.getStylesheets().add(getClass().getResource("/org/to/telegramfinalproject/CSS/light_theme.css").toExternalForm());
        }
    }
}
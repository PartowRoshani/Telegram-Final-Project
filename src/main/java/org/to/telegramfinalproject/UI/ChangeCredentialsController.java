package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ActionHandler;
import org.to.telegramfinalproject.Client.Session;

public class ChangeCredentialsController {

    @FXML private VBox credentialsCard;
    @FXML private Pane overlayBackground;

    @FXML private TextField usernameField;
    @FXML private Label usernameLabel;

    @FXML private Label passwordLabel;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private Button togglePasswordBtn;

    @FXML private Label confirmPasswordLabel;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField visibleConfirmPasswordField;
    @FXML private Button toggleConfirmBtn;

    @FXML private Button cancelButton;
    @FXML private Button saveButton;
    @FXML private Button closeButton;

    private boolean passwordVisible = false;
    private boolean confirmVisible = false;

    // ↓↓↓ متدی که از CheckPasswordController مقدار می‌گیرد
    private String currentPassword;
    public void setCurrentPassword(String p) { this.currentPassword = p; }

    @FXML
    private void initialize() {
        // Toggle password visibility
        togglePasswordBtn.setOnAction(e -> togglePasswordVisibility());
        toggleConfirmBtn.setOnAction(e -> toggleConfirmVisibility());

        // «ذخیره و بستن» مثل ادیت پروفایل
        saveButton.setOnAction(e -> saveAndClose());
        closeButton.setOnAction(e -> saveAndClose());
        cancelButton.setOnAction(e -> saveAndClose());
        overlayBackground.setOnMouseClicked(e -> saveAndClose());

        // Reset error when typing
        usernameField.textProperty().addListener((obs, o, n) -> removeError(usernameField, usernameLabel));

        passwordField.textProperty().addListener((obs, o, n) -> {
            visiblePasswordField.setText(n); // keep synced
            removeError(passwordField, passwordLabel);
            removeError(visiblePasswordField, passwordLabel);
        });
        visiblePasswordField.textProperty().addListener((obs, o, n) -> {
            passwordField.setText(n);
            removeError(passwordField, passwordLabel);
            removeError(visiblePasswordField, passwordLabel);
        });

        confirmPasswordField.textProperty().addListener((obs, o, n) -> {
            visibleConfirmPasswordField.setText(n);
            removeError(confirmPasswordField, confirmPasswordLabel);
            removeError(visibleConfirmPasswordField, confirmPasswordLabel);
        });
        visibleConfirmPasswordField.textProperty().addListener((obs, o, n) -> {
            confirmPasswordField.setText(n);
            removeError(confirmPasswordField, confirmPasswordLabel);
            removeError(visibleConfirmPasswordField, confirmPasswordLabel);
        });

        Platform.runLater(() -> usernameField.requestFocus());
    }

    // ← از CheckPasswordController بلافاصله بعد از load صدا بزن
    public void prefillFromSession() {
        var u = Session.currentUser;
        if (u == null) return;
        String handle = u.optString("user_id",
                u.optString("username",
                        u.optString("display_id","")));
        usernameField.setText(handle);
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        visiblePasswordField.setText(passwordField.getText());
        visiblePasswordField.setVisible(passwordVisible);
        visiblePasswordField.setManaged(passwordVisible);

        passwordField.setVisible(!passwordVisible);
        passwordField.setManaged(!passwordVisible);

        togglePasswordBtn.setText(passwordVisible ? "👁" : "👁");
    }

    private void toggleConfirmVisibility() {
        confirmVisible = !confirmVisible;
        visibleConfirmPasswordField.setText(confirmPasswordField.getText());
        visibleConfirmPasswordField.setVisible(confirmVisible);
        visibleConfirmPasswordField.setManaged(confirmVisible);

        confirmPasswordField.setVisible(!confirmVisible);
        confirmPasswordField.setManaged(!confirmVisible);

        toggleConfirmBtn.setText(confirmVisible ? "👁" : "👁");
    }

    // ارسال تغییرات (در صورت پر بودن/تغییر) و بستن
    private void saveAndClose() {
        boolean anyError = false;
        boolean sentSomething = false;

        // 1) یوزرنیم
        var cu = Session.currentUser;
        String oldUsername = cu != null ? cu.optString("user_id",
                cu.optString("username",
                        cu.optString("display_id",""))) : "";

        String newUsername = usernameField.getText().trim();
        if (!newUsername.isEmpty() && !newUsername.equals(oldUsername)) {
            sentSomething = true;

            JSONObject req = new JSONObject()
                    .put("action", "update_username")
                    .put("current_password", currentPassword) // با اینکه سرور فعلاً چک نمی‌کند، می‌فرستیم
                    .put("new_username", newUsername);

            JSONObject resp = ActionHandler.sendWithResponse(req);
            if (resp == null || !"success".equalsIgnoreCase(resp.optString("status"))) {
                anyError = true;
                showError("Changing username failed",
                        resp != null ? resp.optString("message","Unknown error") : "No response");
            } else {
                if (cu != null) cu.put("user_id", newUsername);
            }
        }

        // 2) پسورد
        String newPwd = (passwordVisible ? visiblePasswordField.getText() : passwordField.getText()).trim();
        String confirm = (confirmVisible ? visibleConfirmPasswordField.getText() : confirmPasswordField.getText()).trim();

        if (!newPwd.isEmpty()) {
            if (confirm.isEmpty() || !confirm.equals(newPwd)) {
                addError(confirmPasswordField, confirmPasswordLabel);
                addError(visibleConfirmPasswordField, confirmPasswordLabel);
                return; // نذار بسته شه تا کاربر درست کنه
            }

            sentSomething = true;

            JSONObject req = new JSONObject()
                    .put("action", "update_password")
                    .put("current_password", currentPassword)
                    .put("new_password", newPwd);

            JSONObject resp = ActionHandler.sendWithResponse(req);
            if (resp == null || !"success".equalsIgnoreCase(resp.optString("status"))) {
                anyError = true;
                showError("Changing password failed",
                        resp != null ? resp.optString("message","Unknown error") : "No response");
            }
        }

        // اگر چیزی نفرستادیم یا همه‌چیز OK بود → ببند و رفرش Settings
        if (!sentSomething || !anyError) {
            var sc = SettingsController.getInstance();
            if (sc != null) sc.populateFromSession();
            MainController.getInstance().closeOverlay(credentialsCard.getParent());
        }
    }

    private void addError(TextField field, Label label) {
        if (!field.getStyleClass().contains("error-input")) {
            field.getStyleClass().add("error-input");
        }
        if (!label.getStyleClass().contains("error-label")) {
            label.getStyleClass().add("error-label");
        }
    }

    private void removeError(TextField field, Label label) {
        field.getStyleClass().remove("error-input");
        label.getStyleClass().remove("error-label");
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        if (a.getDialogPane().getScene()!=null) {
            ThemeManager.getInstance().registerScene(a.getDialogPane().getScene());
        }
        a.showAndWait();
    }
}

package com.mindmap.view.fxml;

import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.User;
import com.mindmap.service.AuthService;
import com.mindmap.util.AppContext;
import com.mindmap.util.SessionManager;
import com.mindmap.util.exceptions.AuthException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FXML controller for register.fxml.
 */
public class RegisterFxmlController {

    private static final Logger log = LoggerFactory.getLogger(RegisterFxmlController.class);

    private final AuthService authService = ServiceRegistry.authService();

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField displayNameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label errorLabel;

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            if (usernameField != null) usernameField.requestFocus();
        });
    }

    @FXML
    private void handleRegister() {
        showError(null);

        try {
            User user = authService.register(
                    usernameField.getText(),
                    emailField.getText(),
                    passwordField.getText(),
                    confirmField.getText(),
                    displayNameField.getText()
            );

            SessionManager.saveSession(user.getId(), user.getUsername());
            AppContext.getInstance().setCurrentUser(user);
            log.info("FXML registration success for {}", user.getUsername());
            MainFxmlLauncher.showMainShell();

        } catch (AuthException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected registration error", e);
            showError("Unexpected error. Please try again.");
        }
    }

    @FXML
    private void handleGoToLogin() {
        MainFxmlLauncher.showLogin();
    }

    private void showError(String message) {
        if (message == null || message.isBlank()) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        } else {
            errorLabel.setText("⚠  " + message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }
}
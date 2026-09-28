package com.mindmap.view.fxml;

import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.User;
import com.mindmap.service.AuthService;
import com.mindmap.util.AppContext;
import com.mindmap.util.RootNavigator;
import com.mindmap.util.SessionManager;
import com.mindmap.util.exceptions.AuthException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FXML controller for login.fxml.
 * Same behavior as the programmatic LoginView — writes to the same services.
 */
public class LoginFxmlController {

    private static final Logger log = LoggerFactory.getLogger(LoginFxmlController.class);

    private final AuthService authService = ServiceRegistry.authService();

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            if (usernameField != null) usernameField.requestFocus();
        });
    }

    @FXML
    private void handleLogin() {
        showError(null);

        String username = usernameField.getText();
        String password = passwordField.getText();

        try {
            User user = authService.login(username, password);
            SessionManager.saveSession(user.getId(), user.getUsername());
            AppContext.getInstance().setCurrentUser(user);
            log.info("FXML login success — loading main shell");
            MainFxmlLauncher.showMainShell();

        } catch (AuthException e) {
            showError(e.getMessage());
            passwordField.clear();
            passwordField.requestFocus();
        } catch (Exception e) {
            log.error("Unexpected login error", e);
            showError("Unexpected error. Please try again.");
        }
    }

    @FXML
    private void handleGoToRegister() {
        MainFxmlLauncher.showRegister();
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
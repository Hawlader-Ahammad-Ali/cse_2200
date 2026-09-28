package com.mindmap.view.auth;

import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.User;
import com.mindmap.service.AuthService;
import com.mindmap.util.AppContext;
import com.mindmap.util.RootNavigator;
import com.mindmap.util.SessionManager;
import com.mindmap.util.exceptions.AuthException;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Login screen. Username-or-email + password.
 * On success, saves session, sets AppContext user, and swaps to MainLayout.
 */
public class LoginView extends _AuthBase {

    private static final Logger log = LoggerFactory.getLogger(LoginView.class);

    private final AuthService authService = ServiceRegistry.authService();

    private final TextField     usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label         errorLabel    = errorLabel();

    public LoginView() {
        usernameField.setPromptText("e.g. johndoe or john@example.com");
        passwordField.setPromptText("Your password");

        Button loginBtn = primaryButton("Sign In");
        Hyperlink registerLink = link("Create an account");

        loginBtn.setOnAction(e -> doLogin());
        passwordField.setOnAction(e -> doLogin());     // Enter submits
        usernameField.setOnAction(e -> passwordField.requestFocus());
        registerLink.setOnAction(e -> RootNavigator.showRegister());

        VBox card = buildCard("Welcome back", "Sign in to continue to MindMap");
        card.getChildren().addAll(
                field("Username or Email", usernameField),
                passwordField("Password", passwordField),
                errorLabel,
                loginBtn,
                linkRow("New here?", registerLink)
        );

        getChildren().add(card);

        Platform.runLater(usernameField::requestFocus);
    }

    private void doLogin() {
        showError(errorLabel, null);

        String username = usernameField.getText();
        String password = passwordField.getText();

        try {
            User user = authService.login(username, password);
            SessionManager.saveSession(user.getId(), user.getUsername());
            AppContext.getInstance().setCurrentUser(user);
            log.info("Login success — switching to main shell.");
            RootNavigator.showMainShell();

        } catch (AuthException e) {
            showError(errorLabel, e.getMessage());
            passwordField.clear();
            passwordField.requestFocus();
        } catch (Exception e) {
            log.error("Unexpected login error", e);
            showError(errorLabel, "Unexpected error. Please try again.");
        }
    }
}
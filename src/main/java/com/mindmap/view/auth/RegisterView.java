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
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registration screen. Creates an account, then auto-signs in.
 */
public class RegisterView extends _AuthBase {

    private static final Logger log = LoggerFactory.getLogger(RegisterView.class);

    private final AuthService authService = ServiceRegistry.authService();

    private final TextField     usernameField = new TextField();
    private final TextField     emailField    = new TextField();
    private final TextField     displayField  = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmField  = new PasswordField();
    private final Label         errorLabel    = errorLabel();

    public RegisterView() {
        usernameField.setPromptText("3–30 letters, numbers, underscores");
        emailField.setPromptText("you@example.com");
        displayField.setPromptText("Optional — shown in the app");
        passwordField.setPromptText("At least 6 characters");
        confirmField.setPromptText("Repeat your password");

        Button registerBtn = primaryButton("Create Account");
        Hyperlink loginLink = link("Sign in instead");

        registerBtn.setOnAction(e -> doRegister());
        confirmField.setOnAction(e -> doRegister());
        loginLink.setOnAction(e -> RootNavigator.showLogin());

        VBox card = buildCard("Create your account",
                "Start building your personal knowledge graph");
        card.getChildren().addAll(
                field("Username", usernameField),
                field("Email", emailField),
                field("Display name", displayField),
                passwordField("Password", passwordField),
                passwordField("Confirm password", confirmField),
                errorLabel,
                registerBtn,
                linkRow("Already have an account?", loginLink)
        );

        getChildren().add(card);
        Platform.runLater(usernameField::requestFocus);
    }

    private void doRegister() {
        showError(errorLabel, null);

        try {
            User user = authService.register(
                    usernameField.getText(),
                    emailField.getText(),
                    passwordField.getText(),
                    confirmField.getText(),
                    displayField.getText()
            );

            // Auto sign-in after registration
            SessionManager.saveSession(user.getId(), user.getUsername());
            AppContext.getInstance().setCurrentUser(user);
            log.info("Registration + auto-login success for {}", user.getUsername());
            RootNavigator.showMainShell();

        } catch (AuthException e) {
            showError(errorLabel, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected registration error", e);
            showError(errorLabel, "Unexpected error. Please try again.");
        }
    }
}
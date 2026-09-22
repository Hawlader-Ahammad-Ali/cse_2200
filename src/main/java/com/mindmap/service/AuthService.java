package com.mindmap.service;

import com.mindmap.dao.UserDAO;
import com.mindmap.dao.impl.UserDAOImpl;
import com.mindmap.model.User;
import com.mindmap.util.AppContext;
import com.mindmap.util.PasswordHasher;
import com.mindmap.util.SessionManager;
import com.mindmap.util.ValidationUtil;
import com.mindmap.util.exceptions.AuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Handles registration, login, session restore, and logout.
 *
 * <p>All SQL errors are wrapped in {@link AuthException} so the UI never
 * sees database details. The message on every {@code AuthException} is
 * safe to display to the user.</p>
 */
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAOImpl();
    }

    /** Test-friendly constructor. */
    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // -------------------------------------------------------------- register

    /**
     * Registers a new user. Returns the created user on success.
     * @throws AuthException if validation fails or the username/email is taken.
     */
    public User register(String username,
                         String email,
                         String password,
                         String confirmPassword,
                         String displayName) throws AuthException {

        String error = ValidationUtil.validateRegistration(username, email, password, confirmPassword);
        if (error != null) throw new AuthException(error);

        username = username.trim();
        email    = email.trim().toLowerCase();

        try {
            if (userDAO.usernameExists(username)) {
                throw new AuthException("That username is already taken.");
            }
            if (userDAO.emailExists(email)) {
                throw new AuthException("That email is already registered.");
            }

            String salt = PasswordHasher.generateSalt();
            String hash = PasswordHasher.hash(password, salt);

            User u = new User();
            u.setUsername(username);
            u.setEmail(email);
            u.setPasswordHash(hash);
            u.setSalt(salt);
            u.setDisplayName((displayName == null || displayName.isBlank())
                    ? username : displayName.trim());

            userDAO.insert(u);
            log.info("Registered new user: {} (id={})", u.getUsername(), u.getId());
            return u;

        } catch (SQLException e) {
            log.error("Registration failed", e);
            throw new AuthException("Could not create account. Please try again.", e);
        }
    }

    // -------------------------------------------------------------- login

    /**
     * Authenticates a user by username OR email + password.
     * @throws AuthException for invalid credentials or DB failure.
     */
    public User login(String usernameOrEmail, String password) throws AuthException {
        String error = ValidationUtil.validateLogin(usernameOrEmail, password);
        if (error != null) throw new AuthException(error);

        try {
            Optional<User> found = userDAO.findByUsernameOrEmail(usernameOrEmail.trim());
            if (found.isEmpty()) {
                // Do not reveal whether the account exists
                throw new AuthException("Invalid username or password.");
            }
            User user = found.get();
            if (!PasswordHasher.verify(password, user.getPasswordHash())) {
                throw new AuthException("Invalid username or password.");
            }
            userDAO.updateLastLogin(user.getId());
            log.info("Login succeeded for {}", user.getUsername());
            return user;

        } catch (SQLException e) {
            log.error("Login failed", e);
            throw new AuthException("Could not sign in. Please try again.", e);
        }
    }

    // -------------------------------------------------------------- session

    /**
     * Restores a session by user id (called on startup).
     * @return the user, or {@code null} if no such user exists.
     */
    public User restoreSession(int userId) throws AuthException {
        try {
            Optional<User> found = userDAO.findById(userId);
            if (found.isEmpty()) return null;
            User user = found.get();
            userDAO.updateLastLogin(user.getId());
            log.info("Session restored for {}", user.getUsername());
            return user;
        } catch (SQLException e) {
            log.error("Session restore failed", e);
            throw new AuthException("Could not restore session.", e);
        }
    }

    /** Clears persisted session and current user. */
    public void logout() {
        SessionManager.clearSession();
        AppContext.getInstance().logout();
        log.info("User logged out.");
    }
}
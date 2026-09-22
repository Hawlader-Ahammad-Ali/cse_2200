package com.mindmap.util;

import java.util.regex.Pattern;

/**
 * Input validation helpers. Every method either returns {@code true}
 * or a user-friendly error message. Never throws.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern USERNAME =
            Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    private static final int MIN_PASSWORD_LENGTH = 6;

    private ValidationUtil() { }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME.matcher(username.trim()).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    /**
     * Validates registration input.
     * @return {@code null} if all inputs are valid, otherwise the first error message.
     */
    public static String validateRegistration(String username,
                                              String email,
                                              String password,
                                              String confirmPassword) {
        if (username == null || username.isBlank()) {
            return "Username is required.";
        }
        if (!isValidUsername(username)) {
            return "Username must be 3–30 characters: letters, numbers, or underscores only.";
        }
        if (email == null || email.isBlank()) {
            return "Email is required.";
        }
        if (!isValidEmail(email)) {
            return "Please enter a valid email address.";
        }
        if (password == null || password.isEmpty()) {
            return "Password is required.";
        }
        if (!isValidPassword(password)) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.";
        }
        if (!password.equals(confirmPassword)) {
            return "Passwords do not match.";
        }
        return null;
    }

    /** Validates login input. */
    public static String validateLogin(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            return "Username or email is required.";
        }
        if (password == null || password.isEmpty()) {
            return "Password is required.";
        }
        return null;
    }
}
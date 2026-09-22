package com.mindmap.util.exceptions;

/**
 * Thrown by {@code AuthService} for user-facing authentication errors.
 * The message is always safe to display in the UI.
 */
public class AuthException extends Exception {

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
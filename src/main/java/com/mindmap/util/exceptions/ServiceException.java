package com.mindmap.util.exceptions;

/**
 * Thrown by service-layer classes for user-facing errors.
 * The message is always safe to display in the UI.
 */
public class ServiceException extends Exception {

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
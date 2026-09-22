package com.mindmap.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt wrapper. Cost factor 10 = ~50ms/hash on modern hardware.
 * Passwords are never stored in plain text anywhere.
 */
public final class PasswordHasher {

    private static final int LOG_ROUNDS = 10;

    private PasswordHasher() { }

    /** Returns a fresh BCrypt salt string (e.g. {@code $2a$10$...}). */
    public static String generateSalt() {
        return BCrypt.gensalt(LOG_ROUNDS);
    }

    /**
     * Hashes a plain password with the given salt.
     * The returned hash already embeds the salt — the salt column is
     * stored separately only for future algorithm migration.
     */
    public static String hash(String plainPassword, String salt) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty");
        }
        return BCrypt.hashpw(plainPassword, salt);
    }

    /** Constant-time verification. Returns false on malformed hash. */
    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (IllegalArgumentException e) {
            // Malformed hash — treat as verification failure.
            return false;
        }
    }
}
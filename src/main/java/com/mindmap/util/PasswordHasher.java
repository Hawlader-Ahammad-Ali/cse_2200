package com.mindmap.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt wrapper for password hashing.
 *
 * <p>Cost factor 10 = roughly 50ms per hash on modern hardware — slow enough
 * to make brute-force attacks expensive, fast enough for user login.</p>
 *
 * <p>Passwords are never stored in plain text anywhere in the application.
 * The hash returned by {@link #hash(String, String)} already embeds the salt,
 * so the separate {@code salt} column in the DB is preserved for future
 * algorithm migration only.</p>
 *
 * <p>Dependency: {@code org.mindrot:jbcrypt:0.4} (already in pom.xml).</p>
 */
public final class PasswordHasher {

    /** BCrypt cost factor. Range 4–31; 10 is the industry-recommended default. */
    private static final int LOG_ROUNDS = 10;

    private PasswordHasher() { /* utility class */ }

    /**
     * Generates a fresh BCrypt salt string, e.g. {@code $2a$10$...}.
     * Store the salt alongside the hash so a future migration to a
     * different algorithm can identify the original parameters.
     */
    public static String generateSalt() {
        return BCrypt.gensalt(LOG_ROUNDS);
    }

    /**
     * Hashes a plain password using the given salt.
     *
     * @param plainPassword the raw password (never stored)
     * @param salt          a salt string produced by {@link #generateSalt()}
     * @return the BCrypt hash, safe to store
     * @throws IllegalArgumentException if the password is null or empty
     */
    public static String hash(String plainPassword, String salt) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty");
        }
        if (salt == null || salt.isBlank()) {
            throw new IllegalArgumentException("Salt must not be empty");
        }
        return BCrypt.hashpw(plainPassword, salt);
    }

    /**
     * Verifies a plain password against a stored BCrypt hash.
     *
     * <p>Uses a constant-time comparison internally, so the caller does
     * not leak information via timing. Returns {@code false} for any
     * malformed input — never throws.</p>
     *
     * @param plainPassword the raw password typed by the user
     * @param storedHash    the hash previously stored in the DB
     * @return {@code true} if the password matches, {@code false} otherwise
     */
    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (IllegalArgumentException e) {
            // Malformed hash (wrong format, truncated, not BCrypt) →
            // treat as verification failure rather than crashing.
            return false;
        }
    }
}
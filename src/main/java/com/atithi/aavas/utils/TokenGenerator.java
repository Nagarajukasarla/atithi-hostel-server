package com.atithi.aavas.utils;

import java.security.SecureRandom;
import java.util.HexFormat;

public final class TokenGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 16; // 128 bits -> 32 hex characters

    private TokenGenerator() {
        // Prevent instantiation
    }

    /**
     * Generates a cryptographically secure, random, unguessable token as a 32-character lowercase hex string.
     * The token is not derived from any entity IDs or names.
     *
     * @return 32-character hexadecimal token string
     */
    public static String generateToken() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }
}

package com.atithi.aavas.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TokenGeneratorTest {

    @Test
    @DisplayName("Should generate a 32-character lowercase hex string")
    void testTokenFormatAndLength() {
        String token = TokenGenerator.generateToken();

        assertNotNull(token);
        assertEquals(32, token.length());
        assertTrue(token.matches("^[0-9a-f]{32}$"), "Token should be a 32-character lowercase hexadecimal string");
    }

    @Test
    @DisplayName("Should generate unique tokens across successive calls")
    void testTokenUniqueness() {
        int sampleSize = 1000;
        Set<String> tokens = new HashSet<>();

        for (int i = 0; i < sampleSize; i++) {
            String token = TokenGenerator.generateToken();
            assertEquals(32, token.length());
            assertTrue(tokens.add(token), "Duplicate token detected: " + token);
        }

        assertEquals(sampleSize, tokens.size());
    }
}

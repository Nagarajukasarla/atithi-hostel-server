package com.atithi.aavas.security;

import com.atithi.aavas.enums.UserRole;
import com.atithi.aavas.enums.UserStatus;
import com.atithi.aavas.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    private KeyLoader keyLoader;
    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        keyLoader = new KeyLoader();
        keyLoader.init();

        jwtService = new JwtService(keyLoader);
        ReflectionTestUtils.setField(jwtService, "expirationSeconds", 86400L);

        testUser = User.builder()
                .id(42L)
                .name("Student Resident")
                .email("student@aavas.com")
                .passwd("hashed_pw")
                .phone("9123456780")
                .hostel(com.atithi.aavas.models.Hostel.builder().id(99L).build())
                .role(UserRole.RESIDENT)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should generate valid JWT token and extract claims correctly")
    void testGenerateAndExtractClaims() {
        String token = jwtService.generateToken(testUser);

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));

        assertEquals("student@aavas.com", jwtService.extractEmail(token));
        assertEquals(42L, jwtService.extractUserId(token));
        assertEquals(99L, jwtService.extractHostelId(token));
        assertEquals("RESIDENT", jwtService.extractRole(token));
    }

    @Test
    @DisplayName("Should return false for malformed or tampered token")
    void testInvalidToken() {
        assertFalse(jwtService.isTokenValid("invalid.jwt.token"));
        assertFalse(jwtService.isTokenValid(""));
        assertFalse(jwtService.isTokenValid(null));
    }
}

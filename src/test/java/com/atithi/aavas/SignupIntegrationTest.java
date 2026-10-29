package com.atithi.aavas;

import com.atithi.aavas.models.Hostel;
import com.atithi.aavas.repositories.HostelRepository;
import com.atithi.aavas.repositories.UserRepository;
import com.atithi.aavas.requests.SignupRequest;
import com.atithi.aavas.responses.AuthResponse;
import com.atithi.aavas.services.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class SignupIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private HostelRepository hostelRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Signup should generate and persist unique token in PostgreSQL database")
    void testSignupPersistsHostelWithUniqueToken() {
        SignupRequest request = new SignupRequest(
                "Orchid Coliving",
                "Manager Alice",
                "alice.manager@orchid.com",
                "Secret@1234",
                "9876543210",
                "Main Street 10",
                "Apt 2B",
                "Bengaluru",
                "Karnataka",
                "560001"
        );

        AuthResponse response = authService.signup(request);

        assertNotNull(response);
        assertNotNull(response.hostel());
        String generatedToken = response.hostel().token();

        assertNotNull(generatedToken, "Hostel token must not be null");
        assertEquals(32, generatedToken.length(), "Token length must be 32 characters");
        assertTrue(generatedToken.matches("^[0-9a-f]{32}$"), "Token must be a lowercase hex string");

        // Verify entity persisted in PostgreSQL
        Optional<Hostel> persistedHostelOpt = hostelRepository.findById(response.hostel().id());
        assertTrue(persistedHostelOpt.isPresent(), "Hostel should be found in database");
        Hostel persistedHostel = persistedHostelOpt.get();
        assertEquals(generatedToken, persistedHostel.getToken(), "Stored token must match response token");

        // Verify findByToken works
        Optional<Hostel> hostelByTokenOpt = hostelRepository.findByToken(generatedToken);
        assertTrue(hostelByTokenOpt.isPresent(), "Hostel must be retrievable by token");
        assertEquals(response.hostel().id(), hostelByTokenOpt.get().getId());
    }
}

package com.atithi.aavas.services;

import com.atithi.aavas.enums.UserRole;
import com.atithi.aavas.enums.UserStatus;
import com.atithi.aavas.implementations.AuthServiceImpl;
import com.atithi.aavas.models.User;
import com.atithi.aavas.repositories.HostelRepository;
import com.atithi.aavas.repositories.UserRepository;
import com.atithi.aavas.requests.LoginRequest;
import com.atithi.aavas.requests.SignupRequest;
import com.atithi.aavas.responses.AuthResponse;
import com.atithi.aavas.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthServiceImplTest {

    @Mock
    private HostelRepository hostelRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private AutoCloseable mocks;
    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private User user;
    private com.atithi.aavas.models.Hostel hostel;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        hostel = com.atithi.aavas.models.Hostel.builder()
                .id(10L)
                .name("Sunrise Hostel")
                .phone("9876543210")
                .addressLane1("Lane 1")
                .city("Hyderabad")
                .state("Telangana")
                .zipcode("500001")
                .status(com.atithi.aavas.enums.HostelStatus.ACTIVE)
                .token("8f4a7c9e2d3e4b119a0f7c2b5d4e6f1a")
                .build();

        signupRequest = new SignupRequest(
                "Sunrise Hostel",
                "Hostel Warden",
                "warden@aavas.com",
                "Password@123",
                "9876543210",
                "Lane 1",
                "Lane 2",
                "Hyderabad",
                "Telangana",
                "500001"
        );

        loginRequest = new LoginRequest("warden@aavas.com", "Password@123");

        user = User.builder()
                .id(1L)
                .hostel(hostel)
                .name("Hostel Warden")
                .email("warden@aavas.com")
                .passwd("encoded_password")
                .phone("9876543210")
                .role(UserRole.OWNER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    @DisplayName("Should signup successfully when email is unique")
    void testSignupSuccess() {
        when(userRepository.existsByEmail("warden@aavas.com")).thenReturn(false);
        when(hostelRepository.save(any(com.atithi.aavas.models.Hostel.class))).thenReturn(hostel);
        when(passwordEncoder.encode("Password@123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt.token.here");

        AuthResponse response = authService.signup(signupRequest);

        assertNotNull(response);
        assertEquals("Signup successful", response.message());
        assertEquals("jwt.token.here", response.token());
        assertEquals("warden@aavas.com", response.user().email());
        assertEquals(UserRole.OWNER, response.user().role());
        assertNotNull(response.hostel());
        assertEquals("Sunrise Hostel", response.hostel().name());
        assertEquals("8f4a7c9e2d3e4b119a0f7c2b5d4e6f1a", response.hostel().token());

        ArgumentCaptor<com.atithi.aavas.models.Hostel> hostelCaptor = ArgumentCaptor.forClass(com.atithi.aavas.models.Hostel.class);
        verify(hostelRepository).save(hostelCaptor.capture());
        com.atithi.aavas.models.Hostel capturedHostel = hostelCaptor.getValue();
        assertNotNull(capturedHostel.getToken());
        assertEquals(32, capturedHostel.getToken().length());
        assertTrue(capturedHostel.getToken().matches("^[0-9a-f]{32}$"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should retry token generation if collision occurs during signup")
    void testSignupTokenCollisionRetry() {
        when(userRepository.existsByEmail("warden@aavas.com")).thenReturn(false);
        // First token check returns true (collision), second returns false (unique)
        when(hostelRepository.existsByToken(anyString())).thenReturn(true, false);
        when(hostelRepository.save(any(com.atithi.aavas.models.Hostel.class))).thenReturn(hostel);
        when(passwordEncoder.encode("Password@123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt.token.here");

        AuthResponse response = authService.signup(signupRequest);

        assertNotNull(response);
        verify(hostelRepository, times(2)).existsByToken(anyString());
        verify(hostelRepository).save(any(com.atithi.aavas.models.Hostel.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when email already exists during signup")
    void testSignupDuplicateEmail() {
        when(userRepository.existsByEmail("warden@aavas.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.signup(signupRequest)
        );

        assertTrue(ex.getMessage().contains("User already exists with email"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLoginSuccess() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail("warden@aavas.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt.token.here");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("Login successful", response.message());
        assertEquals("jwt.token.here", response.token());
        assertEquals("warden@aavas.com", response.user().email());
    }

    @Test
    @DisplayName("Should throw exception when authentication fails on login")
    void testLoginBadCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }
}

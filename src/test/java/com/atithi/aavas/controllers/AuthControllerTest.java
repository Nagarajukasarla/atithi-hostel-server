package com.atithi.aavas.controllers;

import com.atithi.aavas.enums.UserRole;
import com.atithi.aavas.enums.UserStatus;
import com.atithi.aavas.models.User;
import com.atithi.aavas.models.UserPrincipal;
import com.atithi.aavas.requests.LoginRequest;
import com.atithi.aavas.requests.SignupRequest;
import com.atithi.aavas.responses.ApiResponse;
import com.atithi.aavas.responses.AuthResponse;
import com.atithi.aavas.responses.UserDto;
import com.atithi.aavas.security.CookieService;
import com.atithi.aavas.services.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private CookieService cookieService;

    @InjectMocks
    private AuthController authController;

    private AutoCloseable mocks;
    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        com.atithi.aavas.models.Hostel sampleHostel = com.atithi.aavas.models.Hostel.builder()
                .id(1L)
                .name("Grand Hostel")
                .phone("9876543210")
                .addressLane1("Line 1")
                .city("Bengaluru")
                .state("Karnataka")
                .zipcode("560001")
                .status(com.atithi.aavas.enums.HostelStatus.ACTIVE)
                .token("8f4a7c9e2d3e4b119a0f7c2b5d4e6f1a")
                .build();

        signupRequest = new SignupRequest(
                "Grand Hostel",
                "Hostel Owner",
                "owner@atithiaavas.com",
                "Password@123",
                "9876543210",
                "Line 1",
                "Line 2",
                "Bengaluru",
                "Karnataka",
                "560001"
        );

        loginRequest = new LoginRequest("owner@atithiaavas.com", "Password@123");

        sampleUser = User.builder()
                .id(1L)
                .hostel(sampleHostel)
                .name("Hostel Owner")
                .email("owner@atithiaavas.com")
                .passwd("encoded_password")
                .phone("9876543210")
                .role(UserRole.OWNER)
                .status(UserStatus.ACTIVE)
                .build();

        authResponse = new AuthResponse(
                "Signup successful",
                "mock.jwt.token",
                UserDto.fromEntity(sampleUser),
                com.atithi.aavas.responses.HostelDto.fromEntity(sampleHostel)
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    @DisplayName("Signup endpoint should return 201 Created and set HTTP-only cookie")
    void testSignupEndpoint() {
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ResponseCookie cookie = ResponseCookie.from("jwt", "mock.jwt.token").httpOnly(true).path("/").build();

        when(authService.signup(any())).thenReturn(authResponse);
        when(cookieService.createJwtCookie("mock.jwt.token")).thenReturn(cookie);

        ResponseEntity<AuthResponse> responseEntity = authController.signup(signupRequest, servletResponse);

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals("mock.jwt.token", responseEntity.getBody().token());
        assertEquals("owner@atithiaavas.com", responseEntity.getBody().user().email());
        assertNotNull(responseEntity.getBody().hostel());
        assertEquals("Grand Hostel", responseEntity.getBody().hostel().name());
        assertEquals("8f4a7c9e2d3e4b119a0f7c2b5d4e6f1a", responseEntity.getBody().hostel().token());
        assertTrue(servletResponse.getHeaderNames().contains("Set-Cookie"));
        assertTrue(servletResponse.getHeader("Set-Cookie").contains("jwt=mock.jwt.token"));
    }

    @Test
    @DisplayName("Login endpoint should return 200 OK and set HTTP-only cookie")
    void testLoginEndpoint() {
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ResponseCookie cookie = ResponseCookie.from("jwt", "mock.jwt.token").httpOnly(true).path("/").build();

        AuthResponse loginResponse = new AuthResponse("Login successful", "mock.jwt.token", UserDto.fromEntity(sampleUser), com.atithi.aavas.responses.HostelDto.fromEntity(sampleUser.getHostel()));
        when(authService.login(any())).thenReturn(loginResponse);
        when(cookieService.createJwtCookie("mock.jwt.token")).thenReturn(cookie);

        ResponseEntity<AuthResponse> responseEntity = authController.login(loginRequest, servletResponse);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals("mock.jwt.token", responseEntity.getBody().token());
        assertTrue(servletResponse.getHeaderNames().contains("Set-Cookie"));
    }

    @Test
    @DisplayName("Logout endpoint should clear cookie and return success message")
    void testLogoutEndpoint() {
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ResponseCookie cookie = ResponseCookie.from("jwt", "").httpOnly(true).maxAge(0).build();

        when(cookieService.clearJwtCookie()).thenReturn(cookie);

        ResponseEntity<ApiResponse<Void>> responseEntity = authController.logout(servletResponse);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getBody().success());
        assertEquals("Logged out successfully", responseEntity.getBody().message());
    }

    @Test
    @DisplayName("GetCurrentUser endpoint should return user details for authenticated principal")
    void testGetCurrentUserEndpoint() {
        UserPrincipal principal = new UserPrincipal(sampleUser);
        ResponseEntity<ApiResponse<UserDto>> responseEntity = authController.getCurrentUser(principal);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getBody().success());
        assertEquals("owner@atithiaavas.com", responseEntity.getBody().data().email());
        assertEquals("Hostel Owner", responseEntity.getBody().data().name());
    }

    @Test
    @DisplayName("GetCurrentUser endpoint should return 401 when principal is null")
    void testGetCurrentUserUnauthenticated() {
        ResponseEntity<ApiResponse<UserDto>> responseEntity = authController.getCurrentUser(null);

        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertFalse(responseEntity.getBody().success());
        assertEquals("Not authenticated", responseEntity.getBody().message());
    }
}

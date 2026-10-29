package com.atithi.aavas.publicregistration;

import com.atithi.aavas.enums.HostelStatus;
import com.atithi.aavas.enums.UserRole;
import com.atithi.aavas.enums.UserStatus;
import com.atithi.aavas.models.Hostel;
import com.atithi.aavas.models.User;
import com.atithi.aavas.models.UserPrincipal;
import com.atithi.aavas.publicregistration.controllers.QrController;
import com.atithi.aavas.publicregistration.services.QrService;
import com.atithi.aavas.repositories.HostelRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class QrControllerTest {

    @Mock
    private QrService qrService;

    @Mock
    private HostelRepository hostelRepository;

    @InjectMocks
    private QrController qrController;

    private AutoCloseable mocks;
    private Hostel hostel;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(qrController, "publicBaseUrl", "http://localhost:5173");

        hostel = Hostel.builder()
                .id(10L)
                .name("Atithi Luxury PG")
                .status(HostelStatus.ACTIVE)
                .token("testtoken1234567890abcdef1234567")
                .build();

        User user = User.builder()
                .id(1L)
                .hostel(hostel)
                .email("owner@atithi.com")
                .role(UserRole.OWNER)
                .status(UserStatus.ACTIVE)
                .build();

        userPrincipal = new UserPrincipal(user);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    @DisplayName("Should return 200 and image/svg+xml when user is authenticated")
    void testGetHostelQrSuccess() {
        when(hostelRepository.findById(10L)).thenReturn(Optional.of(hostel));
        String mockSvg = "<svg>mock qr</svg>";
        when(qrService.generateSvg(anyString())).thenReturn(mockSvg);

        ResponseEntity<byte[]> response = qrController.getHostelQr(userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(MediaType.parseMediaType("image/svg+xml"), response.getHeaders().getContentType());
        assertEquals(mockSvg, new String(response.getBody(), StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Should return 401 when principal is null")
    void testGetHostelQrUnauthenticated() {
        ResponseEntity<byte[]> response = qrController.getHostelQr(null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}

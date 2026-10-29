package com.atithi.aavas.controllers;

import com.atithi.aavas.enums.HostelStatus;
import com.atithi.aavas.enums.UserRole;
import com.atithi.aavas.enums.UserStatus;
import com.atithi.aavas.models.Hostel;
import com.atithi.aavas.models.User;
import com.atithi.aavas.models.UserPrincipal;
import com.atithi.aavas.requests.UpdateHostelRequest;
import com.atithi.aavas.responses.ApiResponse;
import com.atithi.aavas.responses.HostelDto;
import com.atithi.aavas.services.HostelService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class HostelControllerTest {

    @Mock
    private HostelService hostelService;

    @InjectMocks
    private HostelController hostelController;

    private AutoCloseable mocks;
    private Hostel sampleHostel;
    private User sampleUser;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        sampleHostel = Hostel.builder()
                .id(1L)
                .name("Sunrise Luxury Hostel")
                .phone("9876543210")
                .addressLane1("Plot 42, Silicon Valley Road")
                .addressLane2("Madhapur")
                .city("Hyderabad")
                .state("Telangana")
                .zipcode("500081")
                .status(HostelStatus.ACTIVE)
                .token("8f4a7c9e2d3e4b119a0f7c2b5d4e6f1a")
                .build();

        sampleUser = User.builder()
                .id(100L)
                .hostel(sampleHostel)
                .name("Admin User")
                .email("admin@sunrise.com")
                .role(UserRole.OWNER)
                .status(UserStatus.ACTIVE)
                .build();

        principal = new UserPrincipal(sampleUser);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    @DisplayName("Should fetch current user's hostel details")
    void testGetCurrentHostel() {
        when(hostelService.getHostel(1L)).thenReturn(HostelDto.fromEntity(sampleHostel));

        ResponseEntity<ApiResponse<HostelDto>> response = hostelController.getCurrentHostel(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("Sunrise Luxury Hostel", response.getBody().data().name());
        assertEquals("500081", response.getBody().data().zipcode());
        assertEquals("8f4a7c9e2d3e4b119a0f7c2b5d4e6f1a", response.getBody().data().token());
    }

    @Test
    @DisplayName("Should update current user's hostel details")
    void testUpdateCurrentHostel() {
        UpdateHostelRequest req = new UpdateHostelRequest(
                "Sunrise Grand PG",
                "9123456780",
                "New Address Line 1",
                "Near Metro",
                "Bengaluru",
                "Karnataka",
                "560001",
                4
        );

        Hostel updatedHostel = Hostel.builder()
                .id(1L)
                .name("Sunrise Grand PG")
                .phone("9123456780")
                .addressLane1("New Address Line 1")
                .addressLane2("Near Metro")
                .city("Bengaluru")
                .state("Karnataka")
                .zipcode("560001")
                .status(HostelStatus.ACTIVE)
                .token("8f4a7c9e2d3e4b119a0f7c2b5d4e6f1a")
                .build();

        when(hostelService.updateHostel(eq(1L), any(UpdateHostelRequest.class)))
                .thenReturn(HostelDto.fromEntity(updatedHostel));

        ResponseEntity<ApiResponse<HostelDto>> response = hostelController.updateCurrentHostel(principal, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("Sunrise Grand PG", response.getBody().data().name());
        assertEquals("Bengaluru", response.getBody().data().city());
    }

    @Test
    @DisplayName("Should return 401 when principal is null")
    void testUnauthenticated() {
        ResponseEntity<ApiResponse<HostelDto>> response = hostelController.getCurrentHostel(null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}

package com.atithi.aavas.publicregistration;

import com.atithi.aavas.enums.CustomerStatus;
import com.atithi.aavas.enums.HostelStatus;
import com.atithi.aavas.models.Hostel;
import com.atithi.aavas.publicregistration.controllers.PublicRegistrationController;
import com.atithi.aavas.publicregistration.dtos.PublicRegistrationInfoDto;
import com.atithi.aavas.publicregistration.dtos.PublicRegistrationRequest;
import com.atithi.aavas.publicregistration.implementations.PublicRegistrationServiceImpl;
import com.atithi.aavas.repositories.CustomerRepository;
import com.atithi.aavas.repositories.HostelRepository;
import com.atithi.aavas.responses.ApiResponse;
import com.atithi.aavas.responses.CustomerResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PublicRegistrationControllerTest {

    @Mock
    private HostelRepository hostelRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private PublicRegistrationServiceImpl publicRegistrationService;

    private PublicRegistrationController publicRegistrationController;

    private AutoCloseable mocks;
    private Hostel sampleHostel;
    private PublicRegistrationRequest sampleRequest;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        publicRegistrationController = new PublicRegistrationController(publicRegistrationService);

        sampleHostel = Hostel.builder()
                .id(1L)
                .name("Atithi Comforts PG")
                .status(HostelStatus.ACTIVE)
                .token("validtoken1234567890abcdef123456")
                .build();

        sampleRequest = new PublicRegistrationRequest(
                "Rahul",
                "Kumar",
                "Male",
                "9876543210",
                "123456789012",
                "Software Engineer",
                "9123456780",
                "Suresh Kumar",
                "Hyderabad, Telangana"
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    @DisplayName("GET registration info should return only hostelName and active status")
    void testGetRegistrationInfoSuccess() {
        when(hostelRepository.findByToken("validtoken1234567890abcdef123456")).thenReturn(Optional.of(sampleHostel));

        ResponseEntity<PublicRegistrationInfoDto> response = publicRegistrationController.getRegistrationInfo("validtoken1234567890abcdef123456");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Atithi Comforts PG", response.getBody().hostelName());
        assertTrue(response.getBody().active());
    }

    @Test
    @DisplayName("GET registration info should throw NOT_FOUND for invalid token")
    void testGetRegistrationInfoInvalidToken() {
        when(hostelRepository.findByToken("invalidtoken")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                publicRegistrationController.getRegistrationInfo("invalidtoken")
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("POST registration should create customer with hostel_token populated")
    void testRegisterGuestSuccess() {
        when(hostelRepository.findByToken("validtoken1234567890abcdef123456")).thenReturn(Optional.of(sampleHostel));
        when(customerRepository.existsByAadhaarNumber("123456789012")).thenReturn(false);
        when(customerRepository.existsByMobileNumberAndHostelToken("9876543210", "validtoken1234567890abcdef123456")).thenReturn(false);

        com.atithi.aavas.models.Customer saved = com.atithi.aavas.models.Customer.builder()
                .id(100L)
                .firstName("Rahul")
                .lastName("Kumar")
                .fullName("Rahul Kumar")
                .gender("Male")
                .mobileNumber("9876543210")
                .aadhaarNumber("123456789012")
                .occupation("Software Engineer")
                .emergencyContactName("Suresh Kumar")
                .emergencyContactPhone("9123456780")
                .permanentAddress("Hyderabad, Telangana")
                .hostelToken("validtoken1234567890abcdef123456")
                .status(CustomerStatus.ACTIVE)
                .build();

        when(customerRepository.save(any(com.atithi.aavas.models.Customer.class))).thenReturn(saved);

        ResponseEntity<ApiResponse<CustomerResponse>> response = publicRegistrationController.registerGuest("validtoken1234567890abcdef123456", sampleRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("Rahul Kumar", response.getBody().data().fullName());
        assertEquals("validtoken1234567890abcdef123456", response.getBody().data().hostelToken());
    }

    @Test
    @DisplayName("POST registration should throw CONFLICT when Aadhaar is already registered")
    void testRegisterGuestDuplicateAadhaar() {
        when(hostelRepository.findByToken("validtoken1234567890abcdef123456")).thenReturn(Optional.of(sampleHostel));
        when(customerRepository.existsByAadhaarNumber("123456789012")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                publicRegistrationController.registerGuest("validtoken1234567890abcdef123456", sampleRequest)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Aadhaar"));
    }
}

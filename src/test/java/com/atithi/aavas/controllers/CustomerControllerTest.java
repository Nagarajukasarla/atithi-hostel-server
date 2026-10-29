package com.atithi.aavas.controllers;

import com.atithi.aavas.enums.CustomerStatus;
import com.atithi.aavas.requests.CustomerRequest;
import com.atithi.aavas.responses.ApiResponse;
import com.atithi.aavas.responses.CustomerResponse;
import com.atithi.aavas.services.CustomerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class CustomerControllerTest {

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController customerController;

    private AutoCloseable mocks;
    private CustomerResponse sampleResponse;
    private CustomerRequest sampleRequest;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        sampleRequest = new CustomerRequest(
                "Rajesh",
                "Kumar",
                "Male",
                LocalDate.of(1995, 3, 20),
                "9876543210",
                "9876543211",
                "rajesh.kumar@example.com",
                "123456789012",
                "Software Engineer",
                "Ramesh Kumar",
                "9876543220",
                "12, Gandhi Nagar, Bengaluru"
        );

        sampleResponse = new CustomerResponse(
                1L,
                "Rajesh",
                "Kumar",
                "Rajesh Kumar",
                "Male",
                LocalDate.of(1995, 3, 20),
                "9876543210",
                "9876543211",
                "rajesh.kumar@example.com",
                "123456789012",
                "Software Engineer",
                "Ramesh Kumar",
                "9876543220",
                "12, Gandhi Nagar, Bengaluru",
                CustomerStatus.ACTIVE,
                null,
                null
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    @DisplayName("GET /api/v1/customers/aadhaar/{aadhaarNumber} should return 200 OK and customer profile")
    void getCustomerByAadhaar_success() {
        when(customerService.getCustomerByAadhaar("123456789012")).thenReturn(sampleResponse);

        ResponseEntity<ApiResponse<CustomerResponse>> response = customerController.getCustomerByAadhaar("123456789012");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("123456789012", response.getBody().data().aadhaarNumber());
        assertEquals("Rajesh Kumar", response.getBody().data().fullName());
        verify(customerService, times(1)).getCustomerByAadhaar("123456789012");
    }

    @Test
    @DisplayName("POST /api/v1/customers should return 201 CREATED and saved customer profile")
    void saveCustomer_success() {
        when(customerService.saveOrUpdateCustomer(any(CustomerRequest.class))).thenReturn(sampleResponse);

        ResponseEntity<ApiResponse<CustomerResponse>> response = customerController.saveCustomer(sampleRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("Rajesh Kumar", response.getBody().data().fullName());
        verify(customerService, times(1)).saveOrUpdateCustomer(any(CustomerRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/customers should return 200 OK and list of all customers")
    void getAllCustomers_success() {
        when(customerService.getAllCustomers()).thenReturn(List.of(sampleResponse));

        ResponseEntity<ApiResponse<List<CustomerResponse>>> response = customerController.getAllCustomers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals(1, response.getBody().data().size());
        verify(customerService, times(1)).getAllCustomers();
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id} should return 200 OK and customer profile")
    void getCustomerById_success() {
        when(customerService.getCustomerById(1L)).thenReturn(sampleResponse);

        ResponseEntity<ApiResponse<CustomerResponse>> response = customerController.getCustomerById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals(1L, response.getBody().data().id());
        verify(customerService, times(1)).getCustomerById(1L);
    }

    @Test
    @DisplayName("PUT /api/v1/customers/{id} should return 200 OK and updated customer profile")
    void updateCustomer_success() {
        when(customerService.updateCustomer(eq(1L), any(CustomerRequest.class))).thenReturn(sampleResponse);

        ResponseEntity<ApiResponse<CustomerResponse>> response = customerController.updateCustomer(1L, sampleRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals(1L, response.getBody().data().id());
        verify(customerService, times(1)).updateCustomer(eq(1L), any(CustomerRequest.class));
    }
}

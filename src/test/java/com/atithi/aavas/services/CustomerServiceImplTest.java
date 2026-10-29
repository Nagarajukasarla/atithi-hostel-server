package com.atithi.aavas.services;

import com.atithi.aavas.enums.CustomerStatus;
import com.atithi.aavas.implementations.CustomerServiceImpl;
import com.atithi.aavas.models.Customer;
import com.atithi.aavas.repositories.CustomerRepository;
import com.atithi.aavas.requests.CustomerRequest;
import com.atithi.aavas.responses.CustomerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private CustomerRequest createSampleRequest(String aadhaar) {
        return new CustomerRequest(
                "Rajesh",
                "Kumar",
                "Male",
                LocalDate.of(1995, 3, 20),
                "9876543210",
                "9876543211",
                "rajesh.kumar@example.com",
                aadhaar,
                "Software Engineer",
                "Ramesh Kumar",
                "9876543220",
                "12, Gandhi Nagar, Bengaluru"
        );
    }

    @Test
    @DisplayName("Should create a new customer when Aadhaar does not exist")
    void saveOrUpdateCustomer_newCustomer_success() {
        CustomerRequest request = createSampleRequest("123456789012");
        when(customerRepository.findByAadhaarNumber("123456789012")).thenReturn(Optional.empty());

        Customer savedCustomer = Customer.builder()
                .id(1L)
                .firstName("Rajesh")
                .lastName("Kumar")
                .fullName("Rajesh Kumar")
                .gender("Male")
                .dateOfBirth(LocalDate.of(1995, 3, 20))
                .mobileNumber("9876543210")
                .aadhaarNumber("123456789012")
                .occupation("Software Engineer")
                .emergencyContactName("Ramesh Kumar")
                .emergencyContactPhone("9876543220")
                .permanentAddress("12, Gandhi Nagar, Bengaluru")
                .status(CustomerStatus.ACTIVE)
                .build();

        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        CustomerResponse response = customerService.saveOrUpdateCustomer(request);

        assertNotNull(response);
        assertEquals("Rajesh Kumar", response.fullName());
        assertEquals("123456789012", response.aadhaarNumber());
        assertEquals("9876543210", response.mobileNumber());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should update existing customer when Aadhaar exists")
    void saveOrUpdateCustomer_existingCustomer_updates() {
        CustomerRequest request = createSampleRequest("123456789012");

        Customer existingCustomer = Customer.builder()
                .id(1L)
                .firstName("Old")
                .lastName("Name")
                .fullName("Old Name")
                .gender("Male")
                .mobileNumber("9999999999")
                .aadhaarNumber("123456789012")
                .occupation("Student")
                .emergencyContactName("Old Contact")
                .emergencyContactPhone("8888888888")
                .permanentAddress("Old Address")
                .status(CustomerStatus.ACTIVE)
                .build();

        when(customerRepository.findByAadhaarNumber("123456789012")).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.saveOrUpdateCustomer(request);

        assertNotNull(response);
        assertEquals("Rajesh", response.firstName());
        assertEquals("Kumar", response.lastName());
        assertEquals("Software Engineer", response.occupation());
        verify(customerRepository, times(1)).save(existingCustomer);
    }

    @Test
    @DisplayName("Should return customer when Aadhaar is found")
    void getCustomerByAadhaar_found() {
        Customer customer = Customer.builder()
                .id(1L)
                .firstName("Rajesh")
                .lastName("Kumar")
                .fullName("Rajesh Kumar")
                .aadhaarNumber("123456789012")
                .mobileNumber("9876543210")
                .gender("Male")
                .occupation("Engineer")
                .emergencyContactName("Contact")
                .emergencyContactPhone("9876543220")
                .permanentAddress("Address")
                .build();

        when(customerRepository.findByAadhaarNumber("123456789012")).thenReturn(Optional.of(customer));

        CustomerResponse response = customerService.getCustomerByAadhaar("123456789012");

        assertNotNull(response);
        assertEquals("123456789012", response.aadhaarNumber());
        assertEquals("Rajesh Kumar", response.fullName());
    }

    @Test
    @DisplayName("Should throw exception when Aadhaar is not found")
    void getCustomerByAadhaar_notFound_throwsException() {
        when(customerRepository.findByAadhaarNumber("999999999999")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> customerService.getCustomerByAadhaar("999999999999"));
    }

    @Test
    @DisplayName("Should return list of all customers")
    void getAllCustomers_returnsList() {
        Customer c1 = Customer.builder().id(1L).firstName("A").lastName("B").fullName("A B").build();
        Customer c2 = Customer.builder().id(2L).firstName("C").lastName("D").fullName("C D").build();

        when(customerRepository.findAll()).thenReturn(List.of(c1, c2));

        List<CustomerResponse> responses = customerService.getAllCustomers();

        assertEquals(2, responses.size());
    }
}

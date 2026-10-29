package com.atithi.aavas.publicregistration.implementations;

import com.atithi.aavas.enums.CustomerStatus;
import com.atithi.aavas.enums.HostelStatus;
import com.atithi.aavas.models.Customer;
import com.atithi.aavas.models.Hostel;
import com.atithi.aavas.publicregistration.dtos.PublicRegistrationInfoDto;
import com.atithi.aavas.publicregistration.dtos.PublicRegistrationRequest;
import com.atithi.aavas.publicregistration.services.PublicRegistrationService;
import com.atithi.aavas.repositories.CustomerRepository;
import com.atithi.aavas.repositories.HostelRepository;
import com.atithi.aavas.responses.CustomerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicRegistrationServiceImpl implements PublicRegistrationService {

    private final HostelRepository hostelRepository;
    private final CustomerRepository customerRepository;

    @Override
    public PublicRegistrationInfoDto getRegistrationInfo(String token) {
        Hostel hostel = validateAndGetActiveHostel(token);
        return new PublicRegistrationInfoDto(hostel.getName(), true);
    }

    @Override
    @Transactional
    public CustomerResponse registerGuest(String token, PublicRegistrationRequest request) {
        Hostel hostel = validateAndGetActiveHostel(token);

        String cleanAadhaar = request.aadhaarNumber().trim();
        String cleanMobile = request.mobile().trim();

        // 1. Duplicate Aadhaar Check
        if (customerRepository.existsByAadhaarNumber(cleanAadhaar)) {
            log.warn("Public registration rejected: Aadhaar {} already registered", cleanAadhaar);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A customer with Aadhaar number " + cleanAadhaar + " is already registered.");
        }

        // 2. Duplicate Mobile Check for this Hostel
        if (customerRepository.existsByMobileNumberAndHostelToken(cleanMobile, token)) {
            log.warn("Public registration rejected: Mobile {} already registered for hostel token {}", cleanMobile, token);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A guest with mobile number " + cleanMobile + " is already registered for this property.");
        }

        // 3. Create Customer with hostel_token populated
        Customer customer = Customer.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .gender(request.gender().trim())
                .mobileNumber(cleanMobile)
                .aadhaarNumber(cleanAadhaar)
                .occupation(request.occupation().trim())
                .emergencyContactName(request.emergencyContactName().trim())
                .emergencyContactPhone(request.emergencyContact() != null ? request.emergencyContact().trim() : "")
                .permanentAddress(request.permanentAddress().trim())
                .hostelToken(token)
                .status(CustomerStatus.ACTIVE)
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Successfully registered guest id={} for hostel='{}'", saved.getId(), hostel.getName());

        return CustomerResponse.fromEntity(saved);
    }

    private Hostel validateAndGetActiveHostel(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration token is required");
        }

        Hostel hostel = hostelRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid or expired registration link"));

        if (hostel.getStatus() != HostelStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration is currently unavailable");
        }

        return hostel;
    }
}

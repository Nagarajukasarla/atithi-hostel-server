package com.atithi.aavas.responses;

import com.atithi.aavas.enums.CustomerStatus;
import com.atithi.aavas.models.Customer;

import java.time.Instant;
import java.time.LocalDate;

public record CustomerResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String gender,
        LocalDate dateOfBirth,
        String mobileNumber,
        String altMobileNumber,
        String email,
        String aadhaarNumber,
        String occupation,
        String emergencyContactName,
        String emergencyContactPhone,
        String permanentAddress,
        CustomerStatus status,
        String hostelToken,
        Instant createdAt,
        Instant updatedAt
) {
    public CustomerResponse(
            Long id,
            String firstName,
            String lastName,
            String fullName,
            String gender,
            LocalDate dateOfBirth,
            String mobileNumber,
            String altMobileNumber,
            String email,
            String aadhaarNumber,
            String occupation,
            String emergencyContactName,
            String emergencyContactPhone,
            String permanentAddress,
            CustomerStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, firstName, lastName, fullName, gender, dateOfBirth, mobileNumber, altMobileNumber, email,
                aadhaarNumber, occupation, emergencyContactName, emergencyContactPhone, permanentAddress,
                status, null, createdAt, updatedAt);
    }

    public static CustomerResponse fromEntity(Customer customer) {
        if (customer == null) {
            return null;
        }
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getFullName(),
                customer.getGender(),
                customer.getDateOfBirth(),
                customer.getMobileNumber(),
                customer.getAltMobileNumber(),
                customer.getEmail(),
                customer.getAadhaarNumber(),
                customer.getOccupation(),
                customer.getEmergencyContactName(),
                customer.getEmergencyContactPhone(),
                customer.getPermanentAddress(),
                customer.getStatus(),
                customer.getHostelToken(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}

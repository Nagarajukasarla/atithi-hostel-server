package com.atithi.aavas.publicregistration.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PublicRegistrationRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @NotBlank(message = "Gender is required")
        String gender,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be a 10-digit number")
        String mobile,

        @NotBlank(message = "Aadhaar number is required")
        @Pattern(regexp = "^\\d{12}$", message = "Aadhaar number must be a 12-digit number")
        String aadhaarNumber,

        @NotBlank(message = "Occupation is required")
        String occupation,

        @Pattern(regexp = "^$|^\\d{10}$", message = "Emergency contact must be a 10-digit number")
        String emergencyContact,

        @NotBlank(message = "Emergency contact name is required")
        @Size(max = 150, message = "Emergency contact name must not exceed 150 characters")
        String emergencyContactName,

        @NotBlank(message = "Permanent address is required")
        String permanentAddress
) {}

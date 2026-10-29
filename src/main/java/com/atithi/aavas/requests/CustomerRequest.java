package com.atithi.aavas.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record CustomerRequest(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Gender is required")
        String gender,

        LocalDate dateOfBirth,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be a 10-digit number")
        String mobileNumber,

        String altMobileNumber,

        @Email(message = "Invalid email address format")
        String email,

        @Pattern(regexp = "^$|^\\d{12}$", message = "Aadhaar number must be a 12-digit number")
        String aadhaarNumber,

        String occupation,

        String emergencyContactName,

        @Pattern(regexp = "^$|^\\d{10}$", message = "Emergency contact phone must be a 10-digit number")
        String emergencyContactPhone,

        String permanentAddress
) {}

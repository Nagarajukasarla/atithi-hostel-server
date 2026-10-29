package com.atithi.aavas.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "Hostel name is required")
        String hostelName,

        @NotBlank(message = "Owner name is required")
        String ownerName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        String phone,
        String addressLane1,
        String addressLane2,
        String city,
        String state,
        String zipcode
) {}

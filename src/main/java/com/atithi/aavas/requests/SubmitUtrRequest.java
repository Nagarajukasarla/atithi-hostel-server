package com.atithi.aavas.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record SubmitUtrRequest(
        @NotBlank(message = "Plan name is required")
        String planName,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "12-digit UTR reference number is required")
        @Pattern(regexp = "^[0-9A-Za-z]{12}$", message = "UTR reference must be exactly 12 alphanumeric characters")
        String utrNumber,

        String notes
) {}

package com.atithi.aavas.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RoomTariffRequest(

        @NotNull(message = "Monthly rent is required")
        @DecimalMin(value = "0.00", message = "Monthly rent must be zero or positive")
        BigDecimal monthlyRent,

        @DecimalMin(value = "0.00", message = "Daily rate must be zero or positive")
        BigDecimal dailyRate,

        @NotNull(message = "Security deposit is required")
        @DecimalMin(value = "0.00", message = "Security deposit must be zero or positive")
        BigDecimal securityDepositRequired,

        @NotNull(message = "Effective from date is required")
        LocalDate effectiveFrom,

        /** Null means this tariff is open-ended (no end date yet). */
        LocalDate effectiveTo
) {}

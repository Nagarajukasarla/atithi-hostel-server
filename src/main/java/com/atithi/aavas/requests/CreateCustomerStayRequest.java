package com.atithi.aavas.requests;

import com.atithi.aavas.enums.BillingMode;
import com.atithi.aavas.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request payload for POST /api/v1/customer-stays.
 * Single atomic call creating allocation, financial terms, charges, and initial payment + allocations.
 */
public record CreateCustomerStayRequest(

        @NotNull(message = "Customer ID is required")
        Long customerId,

        Long hostelId,

        @NotNull(message = "Room ID is required")
        Long roomId,

        String bedNumber,

        @NotNull(message = "Check-in date is required")
        LocalDate checkInDate,

        LocalDate checkOutDate,

        @NotNull(message = "Financial terms are required")
        @Valid
        FinancialTermsRequest financialTerms,

        @Valid
        InitialPaymentRequest initialPayment
) {
    public CreateCustomerStayRequest(
            Long customerId,
            Long hostelId,
            Long roomId,
            String bedNumber,
            LocalDate checkInDate,
            FinancialTermsRequest financialTerms
    ) {
        this(customerId, hostelId, roomId, bedNumber, checkInDate, null, financialTerms, null);
    }

    /**
     * Nested DTO for the customer-specific financial agreement.
     */
    public record FinancialTermsRequest(

            Long roomTariffId,

            BillingMode billingMode,

            @DecimalMin(value = "0.00", message = "Monthly rent must be zero or positive")
            BigDecimal monthlyRent,

            @DecimalMin(value = "0.00", message = "Daily rate must be zero or positive")
            BigDecimal dailyRate,

            @NotNull(message = "Security deposit is required")
            @DecimalMin(value = "0.00", message = "Security deposit must be zero or positive")
            BigDecimal securityDepositRequired,

            LocalDate billingAnchorDate
    ) {}

    /**
     * Nested DTO for optional initial payment made at registration.
     */
    public record InitialPaymentRequest(

            @DecimalMin(value = "0.00", message = "Payment amount must be zero or positive")
            BigDecimal amount,

            PaymentMethod paymentMode,

            String notes
    ) {}
}

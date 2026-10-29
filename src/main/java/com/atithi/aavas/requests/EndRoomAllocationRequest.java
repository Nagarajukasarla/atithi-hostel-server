package com.atithi.aavas.requests;

import com.atithi.aavas.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EndRoomAllocationRequest(
        @NotNull(message = "Checkout date is required")
        LocalDate checkOutDate,

        BigDecimal depositDeduction,

        String depositDeductionReason,

        PaymentRequest payment,

        RefundRequest refund
) {
    public record PaymentRequest(
            @NotNull BigDecimal amount,
            @NotNull PaymentMethod paymentMethod,
            String transactionReference,
            String notes
    ) {}

    public record RefundRequest(
            @NotNull BigDecimal amount,
            @NotNull PaymentMethod paymentMethod,
            String transactionReference,
            String notes
    ) {}
}

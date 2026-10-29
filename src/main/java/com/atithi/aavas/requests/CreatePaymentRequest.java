package com.atithi.aavas.requests;

import com.atithi.aavas.enums.PaymentMethod;
import com.atithi.aavas.enums.PaymentStatus;
import com.atithi.aavas.enums.PaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePaymentRequest(
        @NotNull(message = "Customer ID is required")
        Long customerId,

        Long allocationId,

        @NotNull(message = "Payment type is required")
        PaymentType paymentType,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        PaymentStatus paymentStatus,

        String transactionReference,

        String receiptNumber,

        LocalDate paymentDate,

        String notes
) {}

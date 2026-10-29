package com.atithi.aavas.responses;

import com.atithi.aavas.enums.PaymentMethod;
import com.atithi.aavas.enums.PaymentStatus;
import com.atithi.aavas.enums.PaymentType;
import com.atithi.aavas.models.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PaymentResponse(
        Long id,
        Long hostelId,
        Long customerId,
        String customerName,
        Long allocationId,
        String roomNumber,
        String bedNumber,
        PaymentType paymentType,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        String transactionReference,
        String receiptNumber,
        LocalDate paymentDate,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse fromEntity(Payment payment) {
        if (payment == null) {
            return null;
        }

        String customerName = payment.getCustomer() != null ? payment.getCustomer().getFullName() : null;
        String roomNumber = null;
        String bedNumber = null;

        if (payment.getAllocation() != null) {
            bedNumber = payment.getAllocation().getBedNumber();
            if (payment.getAllocation().getRoom() != null) {
                roomNumber = payment.getAllocation().getRoom().getRoomNumber();
            }
        }

        return new PaymentResponse(
                payment.getId(),
                payment.getHostelId(),
                payment.getCustomerId(),
                customerName,
                payment.getAllocationId(),
                roomNumber,
                bedNumber,
                payment.getPaymentType(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                payment.getTransactionReference(),
                payment.getReceiptNumber(),
                payment.getPaymentDate(),
                payment.getNotes(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}

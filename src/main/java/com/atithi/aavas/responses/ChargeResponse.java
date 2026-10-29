package com.atithi.aavas.responses;

import com.atithi.aavas.enums.ChargeStatus;
import com.atithi.aavas.enums.ChargeType;
import com.atithi.aavas.models.Charge;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ChargeResponse(
        Long id,
        Long hostelId,
        Long customerId,
        Long allocationId,
        ChargeType chargeType,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDate periodStart,
        LocalDate periodEnd,
        String notes,
        ChargeStatus status,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        Instant createdAt,
        Instant updatedAt
) {
    public static ChargeResponse fromEntity(Charge charge) {
        return fromEntity(charge, BigDecimal.ZERO);
    }

    public static ChargeResponse fromEntity(Charge charge, BigDecimal paidAmount) {
        if (charge == null) {
            return null;
        }
        BigDecimal paid = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        BigDecimal remaining = charge.getAmount() != null
                ? charge.getAmount().subtract(paid).max(BigDecimal.ZERO)
                : BigDecimal.ZERO;

        return new ChargeResponse(
                charge.getId(),
                charge.getHostel() != null ? charge.getHostel().getId() : null,
                charge.getCustomer() != null ? charge.getCustomer().getId() : null,
                charge.getAllocation() != null ? charge.getAllocation().getId() : null,
                charge.getChargeType(),
                charge.getAmount(),
                charge.getDueDate(),
                charge.getPeriodStart(),
                charge.getPeriodEnd(),
                charge.getNotes(),
                charge.getStatus(),
                paid,
                remaining,
                charge.getCreatedAt(),
                charge.getUpdatedAt()
        );
    }
}

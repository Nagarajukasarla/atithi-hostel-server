package com.atithi.aavas.responses;

import com.atithi.aavas.enums.ChargeType;
import com.atithi.aavas.models.PaymentAllocation;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentAllocationResponse(
        Long id,
        Long paymentId,
        Long chargeId,
        ChargeType chargeType,
        BigDecimal allocatedAmount,
        Instant createdAt
) {
    public static PaymentAllocationResponse fromEntity(PaymentAllocation allocation) {
        if (allocation == null) return null;
        return new PaymentAllocationResponse(
                allocation.getId(),
                allocation.getPayment() != null ? allocation.getPayment().getId() : null,
                allocation.getCharge() != null ? allocation.getCharge().getId() : null,
                allocation.getCharge() != null ? allocation.getCharge().getChargeType() : null,
                allocation.getAllocatedAmount(),
                allocation.getCreatedAt()
        );
    }
}

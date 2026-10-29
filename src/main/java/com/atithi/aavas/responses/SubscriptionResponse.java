package com.atithi.aavas.responses;

import com.atithi.aavas.enums.SubscriptionStatus;
import com.atithi.aavas.models.SubscriptionRequest;

import java.math.BigDecimal;
import java.time.Instant;

public record SubscriptionResponse(
        Long id,
        Long hostelId,
        String planName,
        BigDecimal amount,
        String utrNumber,
        SubscriptionStatus status,
        String notes,
        Instant createdAt
) {
    public static SubscriptionResponse fromEntity(SubscriptionRequest request) {
        if (request == null) {
            return null;
        }
        return new SubscriptionResponse(
                request.getId(),
                request.getHostel() != null ? request.getHostel().getId() : null,
                request.getPlanName(),
                request.getAmount(),
                request.getUtrNumber(),
                request.getStatus(),
                request.getNotes(),
                request.getCreatedAt()
        );
    }
}

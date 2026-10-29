package com.atithi.aavas.responses;

import com.atithi.aavas.enums.BillingMode;
import com.atithi.aavas.models.AllocationFinancialTerms;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AllocationFinancialTermsResponse(
        Long id,
        Long hostelId,
        Long allocationId,
        Long roomTariffId,
        BillingMode billingMode,
        BigDecimal monthlyRent,
        BigDecimal dailyRate,
        BigDecimal securityDepositRequired,
        LocalDate billingAnchorDate,
        Instant createdAt,
        Instant updatedAt
) {
    public static AllocationFinancialTermsResponse fromEntity(AllocationFinancialTerms terms) {
        if (terms == null) return null;
        return new AllocationFinancialTermsResponse(
                terms.getId(),
                terms.getHostelId(),
                terms.getAllocationId(),
                terms.getRoomTariffId(),
                terms.getBillingMode(),
                terms.getMonthlyRent(),
                terms.getDailyRate(),
                terms.getSecurityDepositRequired(),
                terms.getBillingAnchorDate(),
                terms.getCreatedAt(),
                terms.getUpdatedAt()
        );
    }
}

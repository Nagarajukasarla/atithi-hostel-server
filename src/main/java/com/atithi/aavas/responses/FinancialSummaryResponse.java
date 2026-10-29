package com.atithi.aavas.responses;

import com.atithi.aavas.enums.BillingMode;
import java.math.BigDecimal;

public record FinancialSummaryResponse(
        Long customerId,
        String customerName,
        Long activeAllocationId,
        String roomNumber,
        BillingMode billingMode,
        BigDecimal monthlyRent,
        BigDecimal dailyRate,
        BigDecimal securityDepositRequired,
        BigDecimal securityDepositPaid,
        BigDecimal totalCharges,
        BigDecimal totalPaid,
        BigDecimal outstandingBalance
) {
    public FinancialSummaryResponse(
            Long customerId,
            String customerName,
            Long activeAllocationId,
            String roomNumber,
            BigDecimal monthlyRent,
            BigDecimal securityDepositRequired,
            BigDecimal securityDepositPaid,
            BigDecimal totalCharges,
            BigDecimal totalPaid,
            BigDecimal outstandingBalance
    ) {
        this(customerId, customerName, activeAllocationId, roomNumber, BillingMode.MONTHLY,
                monthlyRent, null, securityDepositRequired, securityDepositPaid,
                totalCharges, totalPaid, outstandingBalance);
    }
}

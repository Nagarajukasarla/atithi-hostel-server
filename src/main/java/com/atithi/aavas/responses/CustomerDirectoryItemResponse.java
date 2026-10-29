package com.atithi.aavas.responses;

import com.atithi.aavas.enums.AllocationStatus;
import com.atithi.aavas.enums.BillingMode;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for the Customer Directory table view:
 * Displays active resident room info, agreed terms, paid rent (current cycle),
 * security deposit, security deposit paid, outstanding balance, and latest payment date.
 */
public record CustomerDirectoryItemResponse(
        Long customerId,
        String name,
        String mobileNumber,
        String roomNumber,
        String bedNumber,
        BillingMode billingMode,
        BigDecimal monthlyRent,
        BigDecimal dailyRate,
        BigDecimal paidRent,
        BigDecimal securityDeposit,
        BigDecimal securityDepositPaid,
        LocalDate joinDate,
        LocalDate checkOutDate,
        Long allocationId,
        AllocationStatus allocationStatus,
        BigDecimal outstandingBalance,
        LocalDate latestPaymentDate
) {
    public CustomerDirectoryItemResponse(
            Long customerId,
            String name,
            String mobileNumber,
            String roomNumber,
            String bedNumber,
            BigDecimal monthlyRent,
            BigDecimal paidRent,
            BigDecimal securityDeposit,
            BigDecimal securityDepositPaid,
            LocalDate joinDate,
            Long allocationId,
            BigDecimal outstandingBalance,
            LocalDate latestPaymentDate
    ) {
        this(customerId, name, mobileNumber, roomNumber, bedNumber, BillingMode.MONTHLY,
                monthlyRent, null, paidRent, securityDeposit, securityDepositPaid,
                joinDate, null, allocationId, AllocationStatus.ACTIVE, outstandingBalance, latestPaymentDate);
    }
}

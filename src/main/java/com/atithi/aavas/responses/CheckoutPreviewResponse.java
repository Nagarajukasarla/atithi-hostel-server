package com.atithi.aavas.responses;

import com.atithi.aavas.enums.BillingMode;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CheckoutPreviewResponse(
        Long allocationId,
        Long customerId,
        String customerName,
        String customerPhone,
        Long roomId,
        String roomNumber,
        String bedNumber,
        BillingMode billingMode,
        LocalDate checkInDate,
        LocalDate checkOutDate,

        // Daily-specific fields (null if MONTHLY)
        Integer stayDurationDays,
        BigDecimal dailyRate,

        // Monthly-specific fields (null if DAILY)
        BigDecimal monthlyRent,
        LocalDate currentCycleStart,
        LocalDate currentCycleEnd,
        Integer daysInCycle,
        Integer daysStayedInCycle,
        BigDecimal dailyProRatedRate,
        BigDecimal proRatedRentAmount,
        BigDecimal rentPaidForCurrentCycle,
        BigDecimal unpaidRentDues,

        // Security Deposit fields
        BigDecimal securityDepositRequired,
        BigDecimal securityDepositPaid,

        // Totals
        BigDecimal totalCharges,
        BigDecimal totalPaid,
        BigDecimal outstandingBalance,

        // Net settlement amount: > 0 means resident must pay, < 0 means refund due to resident, == 0 means clear
        BigDecimal netSettlementAmount,

        // Speculative future charges that will be cancelled/deleted
        int speculativeChargesToCancelCount
) {}

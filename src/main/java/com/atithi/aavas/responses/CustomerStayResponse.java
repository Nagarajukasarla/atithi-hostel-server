package com.atithi.aavas.responses;

import com.atithi.aavas.enums.BillingMode;
import com.atithi.aavas.models.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response returned after POST /api/v1/customer-stays.
 * Completed in Plan 3 to include charges, initial payment, payment allocations, and authoritative dues.
 */
public record CustomerStayResponse(
        Long allocationId,
        Long customerId,
        String customerName,
        Long roomId,
        String roomNumber,
        String bedNumber,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Long financialTermsId,
        Long roomTariffId,
        BillingMode billingMode,
        BigDecimal monthlyRent,
        BigDecimal dailyRate,
        BigDecimal securityDepositRequired,
        LocalDate billingAnchorDate,
        BigDecimal totalInitialCharges,
        BigDecimal totalCharges,
        BigDecimal totalPaid,
        BigDecimal outstandingBalance,
        List<ChargeResponse> charges,
        PaymentResponse initialPayment,
        List<PaymentAllocationResponse> paymentAllocations
) {
    public static CustomerStayResponse fromEntities(
            RoomAllocation allocation,
            AllocationFinancialTerms terms,
            List<Charge> charges,
            Payment payment,
            List<PaymentAllocation> allocations,
            BigDecimal outstandingBalance
    ) {
        BigDecimal rent = terms.getMonthlyRent() != null ? terms.getMonthlyRent() : BigDecimal.ZERO;
        BigDecimal daily = terms.getDailyRate() != null ? terms.getDailyRate() : BigDecimal.ZERO;
        BigDecimal deposit = terms.getSecurityDepositRequired() != null ? terms.getSecurityDepositRequired() : BigDecimal.ZERO;
        BigDecimal initialCharges = (terms.getBillingMode() == BillingMode.DAILY ? daily : rent).add(deposit);

        List<ChargeResponse> chargeResponses = charges != null
                ? charges.stream().map(ChargeResponse::fromEntity).toList()
                : List.of();

        BigDecimal sumCharges = charges != null
                ? charges.stream().map(Charge::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                : initialCharges;

        List<PaymentAllocationResponse> allocationResponses = allocations != null
                ? allocations.stream().map(PaymentAllocationResponse::fromEntity).toList()
                : List.of();

        BigDecimal totalPaid = payment != null && payment.getAmount() != null
                ? payment.getAmount()
                : BigDecimal.ZERO;

        BigDecimal balance = outstandingBalance != null
                ? outstandingBalance
                : sumCharges.subtract(totalPaid).max(BigDecimal.ZERO);

        return new CustomerStayResponse(
                allocation.getId(),
                allocation.getCustomerId(),
                allocation.getCustomer() != null ? allocation.getCustomer().getFullName() : null,
                allocation.getRoomId(),
                allocation.getRoom() != null ? allocation.getRoom().getRoomNumber() : null,
                allocation.getBedNumber(),
                allocation.getCheckInDate(),
                allocation.getCheckOutDate(),
                terms.getId(),
                terms.getRoomTariffId(),
                terms.getBillingMode(),
                terms.getMonthlyRent(),
                terms.getDailyRate(),
                terms.getSecurityDepositRequired(),
                terms.getBillingAnchorDate(),
                initialCharges,
                sumCharges,
                totalPaid,
                balance,
                chargeResponses,
                payment != null ? PaymentResponse.fromEntity(payment) : null,
                allocationResponses
        );
    }

    public static CustomerStayResponse fromEntities(
            RoomAllocation allocation,
            AllocationFinancialTerms terms,
            List<Charge> charges,
            BigDecimal outstandingBalance
    ) {
        return fromEntities(allocation, terms, charges, null, List.of(), outstandingBalance);
    }
}

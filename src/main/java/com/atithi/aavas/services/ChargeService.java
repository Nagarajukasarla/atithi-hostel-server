package com.atithi.aavas.services;

import com.atithi.aavas.models.Charge;
import com.atithi.aavas.models.Customer;
import com.atithi.aavas.models.Hostel;
import com.atithi.aavas.models.RoomAllocation;
import com.atithi.aavas.responses.ChargeResponse;
import com.atithi.aavas.responses.CustomerFinancialDetailsResponse;
import com.atithi.aavas.responses.FinancialSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ChargeService {

    /**
     * Creates a SECURITY_DEPOSIT charge for customer + hostel if not already present.
     * Enforces the duplicate guard: only one deposit charge per customer + hostel.
     */
    Charge createSecurityDepositCharge(
            Hostel hostel,
            Customer customer,
            RoomAllocation allocation,
            BigDecimal amount,
            LocalDate dueDate
    );

    /**
     * Creates a MONTHLY_RENT charge for an allocation + billing period if not already present.
     * Enforces the duplicate guard: only one rent charge per allocation + billing period.
     */
    Charge createMonthlyRentCharge(
            Hostel hostel,
            Customer customer,
            RoomAllocation allocation,
            BigDecimal amount,
            LocalDate dueDate,
            LocalDate periodStart,
            LocalDate periodEnd
    );

    /**
     * Creates a DAILY_RENT charge for an allocation + specific date if not already present.
     * Enforces the duplicate guard: only one daily rent charge per allocation + day.
     */
    Charge createDailyRentCharge(
            Hostel hostel,
            Customer customer,
            RoomAllocation allocation,
            BigDecimal amount,
            LocalDate date
    );

    /**
     * Gets all charges for a customer in the hostel, newest due first.
     */
    List<ChargeResponse> getCustomerCharges(Long hostelId, Long customerId);

    /**
     * Gets outstanding unpaid charges for a customer in the hostel, oldest due first.
     */
    List<ChargeResponse> getOutstandingCharges(Long hostelId, Long customerId);

    /**
     * Calculates the authoritative outstanding balance for a customer.
     */
    BigDecimal calculateOutstandingBalance(Long hostelId, Long customerId);

    /**
     * Builds the financial summary for a customer.
     */
    FinancialSummaryResponse getCustomerFinancialSummary(Long hostelId, Long customerId);

    /**
     * Builds the complete financial details view for a customer.
     */
    CustomerFinancialDetailsResponse getCustomerFinancialDetails(Long hostelId, Long customerId);
}

package com.atithi.aavas.services;

import com.atithi.aavas.requests.CreateCustomerStayRequest;
import com.atithi.aavas.responses.CustomerStayResponse;

public interface CustomerStayService {

    /**
     * Creates a complete customer stay in a single transaction:
     * Plan 1: validates customer + room + tariff, creates RoomAllocation + AllocationFinancialTerms.
     * Plan 2: also creates Charges (SECURITY_DEPOSIT + MONTHLY_RENT).
     * Plan 3: also creates Payment + PaymentAllocations if amountPaid > 0.
     */
    CustomerStayResponse createCustomerStay(Long hostelId, CreateCustomerStayRequest request);
}

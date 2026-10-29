package com.atithi.aavas.responses;

import java.util.List;

/**
 * Complete financial overview for the Customer Financial Page:
 * Combines customer details, active room allocation, financial terms snapshot,
 * high-level summary, charges ledger, payments history, and allocations.
 */
public record CustomerFinancialDetailsResponse(
        CustomerResponse customer,
        RoomAllocationResponse activeAllocation,
        AllocationFinancialTermsResponse financialTerms,
        FinancialSummaryResponse summary,
        List<ChargeResponse> charges,
        List<PaymentResponse> payments,
        List<PaymentAllocationResponse> paymentAllocations
) {}

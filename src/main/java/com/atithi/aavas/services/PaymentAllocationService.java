package com.atithi.aavas.services;

import com.atithi.aavas.models.Customer;
import com.atithi.aavas.models.Hostel;
import com.atithi.aavas.models.Payment;
import com.atithi.aavas.models.PaymentAllocation;
import com.atithi.aavas.responses.PaymentAllocationResponse;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentAllocationService {

    /**
     * Allocates a payment against the customer's outstanding charges using the oldest-due-first policy.
     * Updates Charge statuses to PAID or PARTIAL.
     */
    List<PaymentAllocation> allocatePaymentAgainstOldestCharges(
            Hostel hostel,
            Customer customer,
            Payment payment
    );

    List<PaymentAllocationResponse> getPaymentAllocationsByPayment(Long paymentId);

    List<PaymentAllocationResponse> getPaymentAllocationsByCharge(Long chargeId);

    List<PaymentAllocationResponse> getPaymentAllocationsByCustomer(Long hostelId, Long customerId);

    BigDecimal getTotalAllocatedForCustomer(Long hostelId, Long customerId);

    BigDecimal getSecurityDepositPaidForCustomer(Long hostelId, Long customerId);
}

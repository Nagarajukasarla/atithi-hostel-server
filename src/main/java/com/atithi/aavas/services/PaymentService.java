package com.atithi.aavas.services;

import com.atithi.aavas.requests.CreatePaymentRequest;
import com.atithi.aavas.responses.PaymentResponse;

import java.util.List;

public interface PaymentService {

    PaymentResponse createPayment(Long hostelId, CreatePaymentRequest request);

    List<PaymentResponse> getPayments(Long hostelId);

    PaymentResponse getPaymentById(Long hostelId, Long paymentId);

    List<PaymentResponse> getPaymentsByCustomer(Long hostelId, Long customerId);

    List<PaymentResponse> getPaymentsByAllocation(Long hostelId, Long allocationId);
}

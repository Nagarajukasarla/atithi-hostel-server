package com.atithi.aavas.controllers;

import com.atithi.aavas.models.UserPrincipal;
import com.atithi.aavas.responses.*;
import com.atithi.aavas.services.ChargeService;
import com.atithi.aavas.services.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints for customer financial ledger, summary, charges, and payments:
 *   GET /api/v1/customers/{customerId}/charges
 *   GET /api/v1/customers/{customerId}/financial-summary
 *   GET /api/v1/customers/{customerId}/financial-details
 *   GET /api/v1/customers/{customerId}/payments
 */
@RestController
@RequestMapping("/api/v1/customers/{customerId}")
@RequiredArgsConstructor
@Slf4j
public class CustomerFinancialController {

    private final ChargeService chargeService;
    private final PaymentService paymentService;

    @GetMapping("/charges")
    public ResponseEntity<ApiResponse<List<ChargeResponse>>> getCustomerCharges(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long customerId
    ) {
        Long hostelId = requireHostelId(userPrincipal);
        if (hostelId == null) return unauthorized();

        List<ChargeResponse> charges = chargeService.getCustomerCharges(hostelId, customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer charges fetched successfully", charges));
    }

    @GetMapping("/financial-summary")
    public ResponseEntity<ApiResponse<FinancialSummaryResponse>> getFinancialSummary(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long customerId
    ) {
        Long hostelId = requireHostelId(userPrincipal);
        if (hostelId == null) return unauthorized();

        FinancialSummaryResponse summary = chargeService.getCustomerFinancialSummary(hostelId, customerId);
        return ResponseEntity.ok(ApiResponse.success("Financial summary fetched successfully", summary));
    }

    @GetMapping("/financial-details")
    public ResponseEntity<ApiResponse<CustomerFinancialDetailsResponse>> getFinancialDetails(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long customerId
    ) {
        Long hostelId = requireHostelId(userPrincipal);
        if (hostelId == null) return unauthorized();

        CustomerFinancialDetailsResponse details = chargeService.getCustomerFinancialDetails(hostelId, customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer financial details fetched successfully", details));
    }

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getCustomerPayments(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long customerId
    ) {
        Long hostelId = requireHostelId(userPrincipal);
        if (hostelId == null) return unauthorized();

        List<PaymentResponse> payments = paymentService.getPaymentsByCustomer(hostelId, customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer payments fetched successfully", payments));
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private Long requireHostelId(UserPrincipal userPrincipal) {
        return userPrincipal != null ? userPrincipal.getHostelId() : null;
    }

    private <T> ResponseEntity<ApiResponse<T>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Hostel not found or not authenticated"));
    }
}

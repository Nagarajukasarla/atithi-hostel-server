package com.atithi.aavas.controllers;

import com.atithi.aavas.models.UserPrincipal;
import com.atithi.aavas.requests.CreatePaymentRequest;
import com.atithi.aavas.responses.ApiResponse;
import com.atithi.aavas.responses.PaymentResponse;
import com.atithi.aavas.services.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Hostel not identified"));
        }
        PaymentResponse response = paymentService.createPayment(userPrincipal.getHostelId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment recorded successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPayments(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Hostel not identified"));
        }
        List<PaymentResponse> list = paymentService.getPayments(userPrincipal.getHostelId());
        return ResponseEntity.ok(ApiResponse.success("Payments fetched successfully", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Hostel not identified"));
        }
        PaymentResponse response = paymentService.getPaymentById(userPrincipal.getHostelId(), id);
        return ResponseEntity.ok(ApiResponse.success("Payment fetched successfully", response));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByCustomer(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long customerId
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Hostel not identified"));
        }
        List<PaymentResponse> list = paymentService.getPaymentsByCustomer(userPrincipal.getHostelId(), customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer payments fetched successfully", list));
    }
}

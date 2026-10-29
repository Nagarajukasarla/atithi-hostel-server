package com.atithi.aavas.controllers;

import com.atithi.aavas.models.UserPrincipal;
import com.atithi.aavas.requests.CreateCustomerStayRequest;
import com.atithi.aavas.responses.ApiResponse;
import com.atithi.aavas.responses.CustomerStayResponse;
import com.atithi.aavas.services.CustomerStayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * POST /api/v1/customer-stays
 *
 * Single endpoint that atomically creates a room allocation + financial terms
 * (and in future plans, charges + payment) in one backend transaction.
 * The frontend must NOT call room-allocation or payment APIs separately after Plan 3.
 */
@RestController
@RequestMapping("/api/v1/customer-stays")
@RequiredArgsConstructor
public class CustomerStayController {

    private final CustomerStayService customerStayService;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerStayResponse>> createCustomerStay(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateCustomerStayRequest request
    ) {
        Long hostelId = requireHostelId(userPrincipal);
        if (hostelId == null) return unauthorized();

        CustomerStayResponse response = customerStayService.createCustomerStay(hostelId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer stay created successfully", response));
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

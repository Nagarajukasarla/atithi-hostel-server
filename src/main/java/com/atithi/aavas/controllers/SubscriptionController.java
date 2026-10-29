package com.atithi.aavas.controllers;

import com.atithi.aavas.models.UserPrincipal;
import com.atithi.aavas.requests.SubmitUtrRequest;
import com.atithi.aavas.responses.ApiResponse;
import com.atithi.aavas.responses.SubscriptionResponse;
import com.atithi.aavas.services.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/submit-utr")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> submitUtr(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody SubmitUtrRequest request
    ) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User not authenticated"));
        }

        SubscriptionResponse response = subscriptionService.submitUtr(userPrincipal, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment reference submitted successfully. Verification in progress.", response));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User not authenticated"));
        }

        SubscriptionResponse response = subscriptionService.getLatestStatus(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success("Subscription status fetched successfully", response));
    }
}

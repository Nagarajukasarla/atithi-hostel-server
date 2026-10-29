package com.atithi.aavas.controllers;

import com.atithi.aavas.models.UserPrincipal;
import com.atithi.aavas.requests.CustomerRequest;
import com.atithi.aavas.responses.ApiResponse;
import com.atithi.aavas.responses.CustomerDirectoryItemResponse;
import com.atithi.aavas.responses.CustomerResponse;
import com.atithi.aavas.services.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/aadhaar/{aadhaarNumber}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerByAadhaar(
            @PathVariable String aadhaarNumber
    ) {
        CustomerResponse customer = customerService.getCustomerByAadhaar(aadhaarNumber);
        return ResponseEntity.ok(ApiResponse.success("Customer profile found", customer));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> saveCustomer(
            @Valid @RequestBody CustomerRequest request
    ) {
        CustomerResponse saved = customerService.saveOrUpdateCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer saved successfully", saved));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomers() {
        List<CustomerResponse> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(ApiResponse.success("Customers fetched successfully", customers));
    }

    @GetMapping("/directory")
    public ResponseEntity<ApiResponse<List<CustomerDirectoryItemResponse>>> getCustomerDirectory(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Hostel not identified"));
        }
        List<CustomerDirectoryItemResponse> list = customerService.getCustomerDirectory(userPrincipal.getHostelId());
        return ResponseEntity.ok(ApiResponse.success("Customer directory fetched successfully", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(
            @PathVariable Long id
    ) {
        CustomerResponse customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success("Customer fetched successfully", customer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request
    ) {
        CustomerResponse updated = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", updated));
    }
}

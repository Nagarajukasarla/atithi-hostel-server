package com.atithi.aavas.controllers;

import com.atithi.aavas.models.UserPrincipal;
import com.atithi.aavas.requests.CreateRoomAllocationRequest;
import com.atithi.aavas.requests.EndRoomAllocationRequest;
import com.atithi.aavas.responses.ApiResponse;
import com.atithi.aavas.responses.CheckoutPreviewResponse;
import com.atithi.aavas.responses.RoomAllocationResponse;
import com.atithi.aavas.services.RoomAllocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/room-allocation")
@RequiredArgsConstructor
@Slf4j
public class RoomAllocationController {

    private final RoomAllocationService roomAllocationService;

    @PostMapping
    public ResponseEntity<ApiResponse<RoomAllocationResponse>> createAllocation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateRoomAllocationRequest request
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Hostel not identified"));
        }
        RoomAllocationResponse response = roomAllocationService.createAllocation(userPrincipal.getHostelId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Room allocation completed successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomAllocationResponse>>> getAllocations(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Hostel not identified"));
        }
        List<RoomAllocationResponse> list = roomAllocationService.getAllocations(userPrincipal.getHostelId());
        return ResponseEntity.ok(ApiResponse.success("Room allocations fetched successfully", list));
    }

    @GetMapping("/customer/{customerId}/active")
    public ResponseEntity<ApiResponse<RoomAllocationResponse>> getActiveAllocationForCustomer(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long customerId
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Hostel not identified"));
        }
        RoomAllocationResponse allocation = roomAllocationService.getActiveAllocationForCustomer(userPrincipal.getHostelId(), customerId);
        return ResponseEntity.ok(ApiResponse.success("Active allocation fetched", allocation));
    }

    @GetMapping("/room/{roomId}/active")
    public ResponseEntity<ApiResponse<List<RoomAllocationResponse>>> getActiveAllocationsForRoom(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long roomId
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Hostel not identified"));
        }
        List<RoomAllocationResponse> allocations = roomAllocationService.getActiveAllocationsForRoom(userPrincipal.getHostelId(), roomId);
        return ResponseEntity.ok(ApiResponse.success("Active allocations for room fetched", allocations));
    }

    @GetMapping("/{id}/checkout-preview")
    public ResponseEntity<ApiResponse<CheckoutPreviewResponse>> getCheckoutPreview(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Hostel not identified"));
        }
        CheckoutPreviewResponse response = roomAllocationService.getCheckoutPreview(
                userPrincipal.getHostelId(), id, checkOutDate
        );
        return ResponseEntity.ok(ApiResponse.success("Checkout preview fetched successfully", response));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<ApiResponse<RoomAllocationResponse>> endAllocation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestBody(required = false) EndRoomAllocationRequest request
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Hostel not identified"));
        }
        RoomAllocationResponse response = roomAllocationService.endAllocation(userPrincipal.getHostelId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Room allocation ended successfully", response));
    }
}

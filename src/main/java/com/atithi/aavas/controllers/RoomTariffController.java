package com.atithi.aavas.controllers;

import com.atithi.aavas.models.UserPrincipal;
import com.atithi.aavas.requests.RoomTariffRequest;
import com.atithi.aavas.responses.ApiResponse;
import com.atithi.aavas.responses.RoomTariffResponse;
import com.atithi.aavas.services.RoomTariffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints:
 * GET /api/v1/room-types/{roomTypeId}/tariffs/current — fetch current active
 * tariff
 * GET /api/v1/room-types/{roomTypeId}/tariffs/history — full tariff history
 * POST /api/v1/room-types/{roomTypeId}/tariffs — create a new tariff
 */
@RestController
@RequestMapping("/api/v1/room-tariffs")
@RequiredArgsConstructor
public class RoomTariffController {

    private final RoomTariffService roomTariffService;

    /**
     * GET /api/v1/room-tariffs
     * Returns all room tariffs for the authenticated hostel.
     * Optional param: currentOnly (default true) returns only currently active tariffs.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomTariffResponse>>> getAllTariffs(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(value = "currentOnly", defaultValue = "true") boolean currentOnly) {
        Long hostelId = requireHostelId(userPrincipal);
        if (hostelId == null)
            return unauthorized();
        List<RoomTariffResponse> response = currentOnly
                ? roomTariffService.getAllCurrentTariffs(hostelId)
                : roomTariffService.getAllTariffs(hostelId);
        return ResponseEntity.ok(ApiResponse.success("Room tariffs fetched successfully", response));
    }

    /**
     * GET /api/v1/room-tariffs/room-type/{roomTypeId}/current
     */
    @GetMapping("/room-type/{roomTypeId}/current")
    public ResponseEntity<ApiResponse<RoomTariffResponse>> getCurrentTariff(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long roomTypeId) {
        Long hostelId = requireHostelId(userPrincipal);
        if (hostelId == null)
            return unauthorized();
        RoomTariffResponse response = roomTariffService.getCurrentTariff(hostelId, roomTypeId);
        return ResponseEntity.ok(ApiResponse.success(
                response != null ? "Current tariff fetched successfully" : "No active tariff found for this room type",
                response));
    }

    /**
     * GET /api/v1/room-tariffs/room-type/{roomTypeId}/history
     */
    @GetMapping("/room-type/{roomTypeId}/history")
    public ResponseEntity<ApiResponse<List<RoomTariffResponse>>> getTariffHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long roomTypeId) {
        Long hostelId = requireHostelId(userPrincipal);
        if (hostelId == null)
            return unauthorized();
        List<RoomTariffResponse> response = roomTariffService.getTariffHistory(hostelId, roomTypeId);
        return ResponseEntity.ok(ApiResponse.success("Tariff history fetched successfully", response));
    }

    /**
     * POST /api/v1/room-tariffs/room-type/{roomTypeId}
     */
    @PostMapping("/room-type/{roomTypeId}")
    public ResponseEntity<ApiResponse<RoomTariffResponse>> createTariff(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long roomTypeId,
            @RequestParam(value = "closeExisting", defaultValue = "false") boolean closeExisting,
            @Valid @RequestBody RoomTariffRequest request) {
        Long hostelId = requireHostelId(userPrincipal);
        if (hostelId == null)
            return unauthorized();
        RoomTariffResponse response = roomTariffService.createTariff(hostelId, roomTypeId, request, closeExisting);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Room tariff created successfully", response));
    }

    /**
     * PUT /api/v1/room-tariffs/{tariffId}
     */
    @PutMapping("/{tariffId}")
    public ResponseEntity<ApiResponse<RoomTariffResponse>> updateTariff(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long tariffId,
            @Valid @RequestBody RoomTariffRequest request) {
        Long hostelId = requireHostelId(userPrincipal);
        if (hostelId == null)
            return unauthorized();
        RoomTariffResponse response = roomTariffService.updateTariff(hostelId, tariffId, request);
        return ResponseEntity.ok(ApiResponse.success("Room tariff updated successfully", response));
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

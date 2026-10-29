package com.atithi.aavas.controllers;

import com.atithi.aavas.models.UserPrincipal;
import com.atithi.aavas.requests.RoomTypeRequest;
import com.atithi.aavas.responses.ApiResponse;
import com.atithi.aavas.responses.RoomTypeResponse;
import com.atithi.aavas.services.RoomTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/room-types")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomTypeResponse>>> getRoomTypes(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long hostelId = requireHostelId(userPrincipal);
        if (hostelId == null) {
            return unauthorized();
        }
        return ResponseEntity.ok(ApiResponse.success("Room types fetched successfully", roomTypeService.getRoomTypes(hostelId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomTypeResponse>> createRoomType(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody RoomTypeRequest request
    ) {
        Long hostelId = requireHostelId(userPrincipal);
        if (hostelId == null) {
            return unauthorized();
        }
        RoomTypeResponse response = roomTypeService.createRoomType(hostelId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Room type created successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> updateRoomType(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody RoomTypeRequest request
    ) {
        Long hostelId = requireHostelId(userPrincipal);
        if (hostelId == null) {
            return unauthorized();
        }
        RoomTypeResponse response = roomTypeService.updateRoomType(hostelId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Room type updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoomType(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        Long hostelId = requireHostelId(userPrincipal);
        if (hostelId == null) {
            return unauthorized();
        }
        roomTypeService.deleteRoomType(hostelId, id);
        return ResponseEntity.ok(ApiResponse.success("Room type deleted successfully", null));
    }

    private Long requireHostelId(UserPrincipal userPrincipal) {
        return userPrincipal != null ? userPrincipal.getHostelId() : null;
    }

    private <T> ResponseEntity<ApiResponse<T>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Hostel not found or not authenticated"));
    }
}

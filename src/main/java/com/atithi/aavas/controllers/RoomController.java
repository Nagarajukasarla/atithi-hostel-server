package com.atithi.aavas.controllers;

import com.atithi.aavas.models.UserPrincipal;
import com.atithi.aavas.requests.RoomRequest;
import com.atithi.aavas.responses.ApiResponse;
import com.atithi.aavas.responses.RoomResponse;
import com.atithi.aavas.services.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRooms(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String floor
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Hostel not found or not authenticated"));
        }
        List<RoomResponse> rooms = roomService.getRooms(userPrincipal.getHostelId(), floor);
        return ResponseEntity.ok(ApiResponse.success("Rooms fetched successfully", rooms));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Hostel not found or not authenticated"));
        }
        RoomResponse room = roomService.getRoomById(userPrincipal.getHostelId(), id);
        return ResponseEntity.ok(ApiResponse.success("Room fetched successfully", room));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody RoomRequest request
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Hostel not found or not authenticated"));
        }
        RoomResponse saved = roomService.createRoom(userPrincipal.getHostelId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Room created successfully", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody RoomRequest request
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Hostel not found or not authenticated"));
        }
        RoomResponse updated = roomService.updateRoom(userPrincipal.getHostelId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Room updated successfully", updated));
    }

    @PatchMapping("/{id}/toggle-clean")
    public ResponseEntity<ApiResponse<RoomResponse>> toggleCleanStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Hostel not found or not authenticated"));
        }
        RoomResponse updated = roomService.toggleCleanStatus(userPrincipal.getHostelId(), id);
        return ResponseEntity.ok(ApiResponse.success("Room cleanliness status updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Hostel not found or not authenticated"));
        }
        roomService.deleteRoom(userPrincipal.getHostelId(), id);
        return ResponseEntity.ok(ApiResponse.success("Room deleted successfully", null));
    }
}

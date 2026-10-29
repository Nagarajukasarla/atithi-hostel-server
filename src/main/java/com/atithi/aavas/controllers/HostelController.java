package com.atithi.aavas.controllers;

import com.atithi.aavas.models.UserPrincipal;
import com.atithi.aavas.requests.UpdateHostelRequest;
import com.atithi.aavas.responses.ApiResponse;
import com.atithi.aavas.responses.HostelDto;
import com.atithi.aavas.services.HostelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hostels")
@RequiredArgsConstructor
public class HostelController {

    private final HostelService hostelService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<HostelDto>> getCurrentHostel(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Hostel not found or not authenticated"));
        }

        HostelDto hostel = hostelService.getHostel(userPrincipal.getHostelId());
        return ResponseEntity.ok(ApiResponse.success("Hostel details fetched successfully", hostel));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<HostelDto>> updateCurrentHostel(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateHostelRequest request
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Hostel not found or not authenticated"));
        }

        HostelDto updated = hostelService.updateHostel(userPrincipal.getHostelId(), request);
        return ResponseEntity.ok(ApiResponse.success("Hostel details updated successfully", updated));
    }
}

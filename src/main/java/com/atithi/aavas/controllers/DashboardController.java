package com.atithi.aavas.controllers;

import com.atithi.aavas.models.UserPrincipal;
import com.atithi.aavas.responses.ApiResponse;
import com.atithi.aavas.responses.DashboardStatisticsResponse;
import com.atithi.aavas.services.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<DashboardStatisticsResponse>> getDashboardStatistics(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Hostel not identified"));
        }
        DashboardStatisticsResponse statistics = dashboardService.getDashboardStatistics(userPrincipal.getHostelId());
        return ResponseEntity.ok(ApiResponse.success("Dashboard statistics fetched successfully", statistics));
    }
}

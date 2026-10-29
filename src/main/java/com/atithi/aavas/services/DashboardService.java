package com.atithi.aavas.services;

import com.atithi.aavas.responses.DashboardStatisticsResponse;

public interface DashboardService {

    DashboardStatisticsResponse getDashboardStatistics(Long hostelId);
}

package com.atithi.aavas.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatisticsResponse {

    private OccupancyStats occupancy;
    private RevenueStats revenue;
    private ArrivalsStats arrivals;
    private DeparturesStats departures;
    private List<SharingTypeOccupancyDto> sharingTypes;
    private List<MonthlyRevenueTrendDto> revenueTrend;
    private List<UpcomingPaymentDto> upcomingPayments;

    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OccupancyStats {
        private Integer totalBeds;
        private Integer occupiedBeds;
        private Integer vacantBeds;
        private Integer occupancyPercentage;
    }

    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueStats {
        private BigDecimal thisMonthTotalRevenue;   // overall payments received this month across all stays
        private BigDecimal thisMonthMonthlyRevenue; // payments received this month for monthly stays
        private BigDecimal thisMonthDailyRevenue;   // payments received this month for daily stays
        private BigDecimal thisMonthDailyBilledDue; // net unpaid dues for daily stays
        private BigDecimal totalMonthlyRevenue;     // alias pointing to thisMonthTotalRevenue
        private BigDecimal totalMonthlyBilled;      // charges billed this month across property
        private BigDecimal totalDueAmount;          // total pending dues across property
        private BigDecimal todayRevenue;            // payments received today
    }

    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArrivalsStats {
        private Long monthArrivalsCount;
        private Long todayArrivalsCount;
        private Long activeInHouseGuests;
        private Long activeDailyGuests;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeparturesStats {
        private Long monthDeparturesCount;
        private Long todayDeparturesCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SharingTypeOccupancyDto {
        private Long roomTypeId;
        private String sharingTypeName;
        private Integer sharingCount;
        private Integer totalBeds;
        private Integer occupiedBeds;
        private Integer occupancyPercentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenueTrendDto {
        private String month;       // e.g. "Oct 2026"
        private String yearMonth;   // e.g. "2026-10"
        private BigDecimal revenue;
    }

    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpcomingPaymentDto {
        private Long chargeId;
        private Long customerId;
        private String customerName;
        private String roomNumber;
        private String bedNumber;
        private LocalDate dueDate;
        private BigDecimal amount;
        private String status; // "Overdue" | "Upcoming" | "Due Today"
        private Integer pendingDuesCount;
        private String billingMode; // "DAILY" | "MONTHLY"
    }
}

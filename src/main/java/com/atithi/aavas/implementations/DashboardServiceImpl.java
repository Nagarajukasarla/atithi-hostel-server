package com.atithi.aavas.implementations;

import com.atithi.aavas.enums.AllocationStatus;
import com.atithi.aavas.enums.BillingMode;
import com.atithi.aavas.enums.ChargeStatus;
import com.atithi.aavas.enums.ChargeType;
import com.atithi.aavas.enums.PaymentStatus;
import com.atithi.aavas.models.Charge;
import com.atithi.aavas.models.RoomAllocation;
import com.atithi.aavas.repositories.*;
import com.atithi.aavas.responses.DashboardStatisticsResponse;
import com.atithi.aavas.responses.DashboardStatisticsResponse.*;
import com.atithi.aavas.services.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final PaymentRepository paymentRepository;
    private final ChargeRepository chargeRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final AllocationFinancialTermsRepository allocationFinancialTermsRepository;

    @Override
    public DashboardStatisticsResponse getDashboardStatistics(Long hostelId) {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        // 1. Occupancy & Room Types via JPQL
        OccupancyStats occupancyStats = buildOccupancyStats(hostelId);
        List<SharingTypeOccupancyDto> sharingTypes = buildSharingTypeOccupancy(hostelId);

        // 2. Arrivals & Departures via JPQL count queries
        ArrivalsStats arrivalsStats = buildArrivalsStats(hostelId, today, monthStart, monthEnd);
        DeparturesStats departuresStats = buildDeparturesStats(hostelId, today, monthStart, monthEnd);

        // 3. Revenue & Monthly trend via JPQL aggregations
        Map<Integer, BigDecimal> monthlySums = aggregateCurrentYearRevenue(hostelId, today.getYear());
        BigDecimal totalDueAmount = calculateTotalDueAmount(hostelId);
        RevenueStats revenueStats = buildRevenueStats(hostelId, today, currentMonth, monthStart, monthEnd, monthlySums, totalDueAmount);
        List<MonthlyRevenueTrendDto> revenueTrend = buildRevenueTrend(today.getYear(), monthlySums);

        // 4. Upcoming dues grouped by customer for payments within 10 days
        List<UpcomingPaymentDto> upcomingPayments = findUpcomingPayments(hostelId, today);

        return DashboardStatisticsResponse.builder()
                .occupancy(occupancyStats)
                .revenue(revenueStats)
                .arrivals(arrivalsStats)
                .departures(departuresStats)
                .sharingTypes(sharingTypes)
                .revenueTrend(revenueTrend)
                .upcomingPayments(upcomingPayments)
                .build();
    }

    // ── Occupancy ──────────────────────────────────────────────────────────────

    private OccupancyStats buildOccupancyStats(Long hostelId) {
        List<Object[]> rows = roomRepository.sumOccupancyByHostelId(hostelId);
        int totalBeds = 0;
        int occupiedBeds = 0;

        if (!rows.isEmpty() && rows.get(0) != null) {
            Object[] row = rows.get(0);
            totalBeds = row[0] != null ? ((Number) row[0]).intValue() : 0;
            occupiedBeds = row[1] != null ? ((Number) row[1]).intValue() : 0;
        }

        int vacantBeds = Math.max(0, totalBeds - occupiedBeds);
        int occupancyPercentage = totalBeds > 0 ? (int) Math.round((occupiedBeds * 100.0) / totalBeds) : 0;

        return OccupancyStats.builder()
                .totalBeds(totalBeds)
                .occupiedBeds(occupiedBeds)
                .vacantBeds(vacantBeds)
                .occupancyPercentage(occupancyPercentage)
                .build();
    }

    private List<SharingTypeOccupancyDto> buildSharingTypeOccupancy(Long hostelId) {
        List<Object[]> rows = roomTypeRepository.findSharingTypeOccupancySummary(hostelId);
        List<SharingTypeOccupancyDto> dtos = new ArrayList<>(rows.size());

        for (Object[] r : rows) {
            Long rtId = (Long) r[0];
            String name = (String) r[1];
            Integer sharingCount = (Integer) r[2];
            int totalBeds = r[3] != null ? ((Number) r[3]).intValue() : 0;
            int occupiedBeds = r[4] != null ? ((Number) r[4]).intValue() : 0;
            int percent = totalBeds > 0 ? (int) Math.round((occupiedBeds * 100.0) / totalBeds) : 0;

            dtos.add(SharingTypeOccupancyDto.builder()
                    .roomTypeId(rtId)
                    .sharingTypeName(name)
                    .sharingCount(sharingCount)
                    .totalBeds(totalBeds)
                    .occupiedBeds(occupiedBeds)
                    .occupancyPercentage(percent)
                    .build());
        }
        return dtos;
    }

    // ── Arrivals & Departures ──────────────────────────────────────────────────

    private ArrivalsStats buildArrivalsStats(Long hostelId, LocalDate today, LocalDate monthStart, LocalDate monthEnd) {
        long monthArrivals = roomAllocationRepository.countByHostel_IdAndCheckInDateBetween(hostelId, monthStart, monthEnd);
        long todayArrivals = roomAllocationRepository.countByHostel_IdAndCheckInDate(hostelId, today);
        long activeInHouse = roomAllocationRepository.countByHostel_IdAndStatus(hostelId, AllocationStatus.ACTIVE);
        long activeDaily = roomAllocationRepository.countByHostelAndStatusAndBillingMode(
                hostelId, AllocationStatus.ACTIVE, BillingMode.DAILY
        );

        return ArrivalsStats.builder()
                .monthArrivalsCount(monthArrivals)
                .todayArrivalsCount(todayArrivals)
                .activeInHouseGuests(activeInHouse)
                .activeDailyGuests(activeDaily)
                .build();
    }

    private DeparturesStats buildDeparturesStats(Long hostelId, LocalDate today, LocalDate monthStart, LocalDate monthEnd) {
        long monthDepartures = roomAllocationRepository.countByHostel_IdAndCheckOutDateBetween(hostelId, monthStart, monthEnd);
        long todayDepartures = roomAllocationRepository.countByHostel_IdAndCheckOutDate(hostelId, today);

        return DeparturesStats.builder()
                .monthDeparturesCount(monthDepartures)
                .todayDeparturesCount(todayDepartures)
                .build();
    }

    // ── Revenue ────────────────────────────────────────────────────────────────

    private Map<Integer, BigDecimal> aggregateCurrentYearRevenue(Long hostelId, int currentYear) {
        LocalDate yearStart = LocalDate.of(currentYear, 1, 1);
        LocalDate yearEnd = LocalDate.of(currentYear, 12, 31);

        List<Object[]> rows = paymentRepository.sumMonthlyRevenueForYear(
                hostelId, PaymentStatus.SUCCESS, yearStart, yearEnd
        );

        Map<Integer, BigDecimal> monthlySums = new HashMap<>();
        for (Object[] r : rows) {
            if (r[0] != null && r[1] != null) {
                int month = ((Number) r[0]).intValue();
                BigDecimal sum = (BigDecimal) r[1];
                monthlySums.put(month, sum);
            }
        }
        return monthlySums;
    }

    private BigDecimal calculateTotalDueAmount(Long hostelId) {
        List<ChargeStatus> openStatuses = List.of(ChargeStatus.OPEN, ChargeStatus.PARTIAL);
        BigDecimal totalBilled = chargeRepository.sumAmountByHostelAndStatusIn(hostelId, openStatuses);
        BigDecimal totalAllocated = paymentAllocationRepository.sumAllocatedAmountByHostelAndChargeStatusIn(hostelId, openStatuses);
        BigDecimal dues = (totalBilled != null ? totalBilled : BigDecimal.ZERO)
                .subtract(totalAllocated != null ? totalAllocated : BigDecimal.ZERO);
        return dues.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : dues;
    }

    private BigDecimal calculateDailyDueAmount(Long hostelId) {
        List<ChargeStatus> openStatuses = List.of(ChargeStatus.OPEN, ChargeStatus.PARTIAL);
        BigDecimal totalBilledDaily = chargeRepository.sumAmountByHostelAndStatusInAndBillingMode(
                hostelId, openStatuses, BillingMode.DAILY
        );
        BigDecimal totalAllocatedDaily = paymentAllocationRepository.sumAllocatedAmountByHostelAndChargeStatusInAndBillingMode(
                hostelId, openStatuses, BillingMode.DAILY
        );
        BigDecimal dues = (totalBilledDaily != null ? totalBilledDaily : BigDecimal.ZERO)
                .subtract(totalAllocatedDaily != null ? totalAllocatedDaily : BigDecimal.ZERO);
        return dues.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : dues;
    }

    private RevenueStats buildRevenueStats(
            Long hostelId, LocalDate today, YearMonth currentMonth,
            LocalDate monthStart, LocalDate monthEnd,
            Map<Integer, BigDecimal> monthlySums, BigDecimal totalDueAmount
    ) {
        BigDecimal thisMonthTotalRevenue = monthlySums.getOrDefault(currentMonth.getMonthValue(), BigDecimal.ZERO);
        BigDecimal thisMonthDailyRevenue = paymentRepository.sumAmountByHostelAndStatusAndBillingModeAndPaymentDateBetween(
                hostelId, PaymentStatus.SUCCESS, BillingMode.DAILY, monthStart, monthEnd
        );
        BigDecimal thisMonthMonthlyRevenue = paymentRepository.sumAmountByHostelAndStatusAndBillingModeAndPaymentDateBetween(
                hostelId, PaymentStatus.SUCCESS, BillingMode.MONTHLY, monthStart, monthEnd
        );
        BigDecimal thisMonthDailyBilledDue = calculateDailyDueAmount(hostelId);
        BigDecimal todayRevenue = paymentRepository.sumAmountByHostelAndStatusAndPaymentDate(
                hostelId, PaymentStatus.SUCCESS, today
        );
        BigDecimal totalMonthlyBilled = chargeRepository.sumBilledBetweenDates(hostelId, monthStart, monthEnd);

        return RevenueStats.builder()
                .thisMonthTotalRevenue(thisMonthTotalRevenue != null ? thisMonthTotalRevenue : BigDecimal.ZERO)
                .thisMonthMonthlyRevenue(thisMonthMonthlyRevenue != null ? thisMonthMonthlyRevenue : BigDecimal.ZERO)
                .thisMonthDailyRevenue(thisMonthDailyRevenue != null ? thisMonthDailyRevenue : BigDecimal.ZERO)
                .thisMonthDailyBilledDue(thisMonthDailyBilledDue != null ? thisMonthDailyBilledDue : BigDecimal.ZERO)
                .totalMonthlyRevenue(thisMonthTotalRevenue != null ? thisMonthTotalRevenue : BigDecimal.ZERO)
                .totalMonthlyBilled(totalMonthlyBilled != null ? totalMonthlyBilled : BigDecimal.ZERO)
                .totalDueAmount(totalDueAmount != null ? totalDueAmount : BigDecimal.ZERO)
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .build();
    }

    private List<MonthlyRevenueTrendDto> buildRevenueTrend(int currentYear, Map<Integer, BigDecimal> monthlySums) {
        List<MonthlyRevenueTrendDto> trend = new ArrayList<>(12);
        DateTimeFormatter monthNameFmt = DateTimeFormatter.ofPattern("MMM");

        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(currentYear, m);
            trend.add(MonthlyRevenueTrendDto.builder()
                    .month(ym.format(monthNameFmt))
                    .yearMonth(ym.toString())
                    .revenue(monthlySums.getOrDefault(m, BigDecimal.ZERO))
                    .build());
        }
        return trend;
    }

    // ── Upcoming Dues ──────────────────────────────────────────────────────────

    /**
     * Finds open/partial charges due within the next 10 days (or overdue),
     * fetches their allocations via JPQL, and groups them by customer so each customer
     * appears at most once in upcoming payments.
     */
    private List<UpcomingPaymentDto> findUpcomingPayments(Long hostelId, LocalDate today) {
        LocalDate tenDaysAhead = today.plusDays(10);
        List<Charge> upcomingCharges = chargeRepository.findByHostel_IdAndStatusInAndDueDateLessThanEqualOrderByDueDateAsc(
                hostelId, List.of(ChargeStatus.OPEN, ChargeStatus.PARTIAL), tenDaysAhead
        );

        if (upcomingCharges.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> chargeIds = upcomingCharges.stream().map(Charge::getId).toList();
        Map<Long, BigDecimal> allocatedMap = paymentAllocationRepository.sumAllocationsGroupedByChargeIds(chargeIds)
                .stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[0],
                        r -> (BigDecimal) r[1],
                        (a, b) -> a
                ));

        Set<Long> allocationIds = upcomingCharges.stream()
                .map(Charge::getAllocation)
                .filter(Objects::nonNull)
                .map(RoomAllocation::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> billingModeMap = allocationIds.isEmpty() ? Collections.emptyMap() :
                allocationFinancialTermsRepository.findBillingModesByAllocationIds(allocationIds)
                        .stream()
                        .collect(Collectors.toMap(
                                r -> (Long) r[0],
                                r -> r[1] != null ? r[1].toString() : "MONTHLY",
                                (a, b) -> a
                        ));

        Map<Long, CustomerDueAgg> customerDueMap = new LinkedHashMap<>();

        for (Charge c : upcomingCharges) {
            BigDecimal paid = allocatedMap.getOrDefault(c.getId(), BigDecimal.ZERO);
            BigDecimal remaining = (c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO).subtract(paid);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Long custId = c.getCustomer() != null ? c.getCustomer().getId() : null;
            if (custId == null) continue;

            Long allocId = c.getAllocation() != null ? c.getAllocation().getId() : null;
            String chargeBillingMode = "MONTHLY";
            if (c.getChargeType() == ChargeType.DAILY_RENT) {
                chargeBillingMode = "DAILY";
            } else if (allocId != null && billingModeMap.containsKey(allocId)) {
                chargeBillingMode = billingModeMap.get(allocId);
            }

            final String detectedBillingMode = chargeBillingMode;

            CustomerDueAgg agg = customerDueMap.computeIfAbsent(custId, id -> {
                CustomerDueAgg newAgg = new CustomerDueAgg();
                newAgg.customerId = id;
                newAgg.customerName = c.getCustomer().getFullName();
                if (c.getAllocation() != null) {
                    if (c.getAllocation().getRoom() != null) {
                        newAgg.roomNumber = c.getAllocation().getRoom().getRoomNumber();
                    }
                    newAgg.bedNumber = c.getAllocation().getBedNumber();
                }
                newAgg.representativeChargeId = c.getId();
                newAgg.billingMode = detectedBillingMode;
                return newAgg;
            });

            if ("DAILY".equals(detectedBillingMode)) {
                agg.billingMode = "DAILY";
            }

            agg.totalAmount = agg.totalAmount.add(remaining);
            agg.count++;

            if (agg.earliestDueDate == null || (c.getDueDate() != null && c.getDueDate().isBefore(agg.earliestDueDate))) {
                agg.earliestDueDate = c.getDueDate();
            }
            if (c.getDueDate() != null) {
                if (c.getDueDate().isBefore(today)) {
                    agg.overdueCount++;
                } else if (c.getDueDate().isEqual(today)) {
                    agg.dueTodayCount++;
                }
            }
        }

        return buildUpcomingPaymentDtos(customerDueMap);
    }

    private List<UpcomingPaymentDto> buildUpcomingPaymentDtos(Map<Long, CustomerDueAgg> customerDueMap) {
        List<UpcomingPaymentDto> upcomingPayments = new ArrayList<>();

        for (CustomerDueAgg agg : customerDueMap.values()) {
            String status;
            if (agg.overdueCount > 0) {
                status = "Overdue";
            } else if (agg.dueTodayCount > 0) {
                status = "Due Today";
            } else {
                status = "Upcoming";
            }

            upcomingPayments.add(UpcomingPaymentDto.builder()
                    .chargeId(agg.representativeChargeId)
                    .customerId(agg.customerId)
                    .customerName(agg.customerName != null ? agg.customerName : "Resident")
                    .roomNumber(agg.roomNumber)
                    .bedNumber(agg.bedNumber)
                    .dueDate(agg.earliestDueDate)
                    .amount(agg.totalAmount)
                    .status(status)
                    .pendingDuesCount(agg.count)
                    .billingMode(agg.billingMode != null ? agg.billingMode : "MONTHLY")
                    .build());
        }

        // Sort: Overdue first, then Due Today, then Upcoming — within each group by earliest dueDate
        upcomingPayments.sort((a, b) -> {
            int scoreA = "Overdue".equals(a.getStatus()) ? 0 : "Due Today".equals(a.getStatus()) ? 1 : 2;
            int scoreB = "Overdue".equals(b.getStatus()) ? 0 : "Due Today".equals(b.getStatus()) ? 1 : 2;
            if (scoreA != scoreB) return Integer.compare(scoreA, scoreB);
            if (a.getDueDate() == null && b.getDueDate() == null) return 0;
            if (a.getDueDate() == null) return 1;
            if (b.getDueDate() == null) return -1;
            return a.getDueDate().compareTo(b.getDueDate());
        });

        return upcomingPayments;
    }

    // ── Internal helper classes ────────────────────────────────────────────────

    private static class CustomerDueAgg {
        Long customerId;
        String customerName;
        String roomNumber;
        String bedNumber;
        LocalDate earliestDueDate;
        BigDecimal totalAmount = BigDecimal.ZERO;
        int overdueCount = 0;
        int dueTodayCount = 0;
        int count = 0;
        Long representativeChargeId;
        String billingMode = "MONTHLY";
    }
}

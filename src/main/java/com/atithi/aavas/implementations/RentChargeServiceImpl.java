package com.atithi.aavas.implementations;

import com.atithi.aavas.enums.ChargeStatus;
import com.atithi.aavas.enums.ChargeType;
import com.atithi.aavas.models.AllocationFinancialTerms;
import com.atithi.aavas.models.Charge;
import com.atithi.aavas.repositories.AllocationFinancialTermsRepository;
import com.atithi.aavas.repositories.ChargeRepository;
import com.atithi.aavas.services.MonthlyRentChargeService;
import com.atithi.aavas.services.RentChargeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scans active allocations and creates MONTHLY_RENT or DAILY_RENT charges
 * for any billing periods that have become due up to today.
 *
 * <p>Implements both {@link RentChargeService} and {@link MonthlyRentChargeService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RentChargeServiceImpl implements RentChargeService, MonthlyRentChargeService {

    private final AllocationFinancialTermsRepository financialTermsRepository;
    private final ChargeRepository chargeRepository;

    @Override
    @Transactional
    public void generateDueCharges() {
        LocalDate today = LocalDate.now();
        log.info("Rent charge generation job started. today={}", today);

        int monthlyChargesCreated = generateMonthlyCharges(today);
        int dailyChargesCreated = generateDailyCharges(today);

        log.info("Rent charge generation job completed. monthlyChargesCreated={}, dailyChargesCreated={}",
                monthlyChargesCreated, dailyChargesCreated);
    }

    /**
     * Generates due MONTHLY_RENT charges for active monthly allocations.
     */
    private int generateMonthlyCharges(LocalDate today) {
        List<AllocationFinancialTerms> activeTerms = financialTermsRepository.findActiveMonthlyAllocations();
        log.info("Found {} active monthly allocation financial terms to evaluate.", activeTerms.size());

        int count = 0;
        for (AllocationFinancialTerms terms : activeTerms) {
            try {
                count += generateMonthlyChargesForAllocation(terms, today);
            } catch (Exception e) {
                log.error("Error generating monthly rent charge for allocation {}: {}",
                        terms.getAllocationId(), e.getMessage(), e);
            }
        }
        return count;
    }

    private int generateMonthlyChargesForAllocation(AllocationFinancialTerms terms, LocalDate today) {
        LocalDate anchor = terms.getBillingAnchorDate();
        if (anchor == null) {
            anchor = terms.getAllocation().getCheckInDate();
        }
        int chargesCreated = 0;
        int periodIndex = 1;

        while (true) {
            LocalDate periodStart = anchor.plusMonths(periodIndex);
            LocalDate periodEnd = anchor.plusMonths(periodIndex + 1).minusDays(1);

            if (periodStart.isAfter(today)) {
                break;
            }

            boolean exists = chargeRepository.existsByAllocation_IdAndChargeTypeAndPeriodStartAndPeriodEnd(
                    terms.getAllocationId(),
                    ChargeType.MONTHLY_RENT,
                    periodStart,
                    periodEnd
            );

            if (!exists) {
                Charge charge = Charge.builder()
                        .hostel(terms.getHostel())
                        .customer(terms.getAllocation().getCustomer())
                        .allocation(terms.getAllocation())
                        .chargeType(ChargeType.MONTHLY_RENT)
                        .amount(terms.getMonthlyRent())
                        .dueDate(periodStart)
                        .periodStart(periodStart)
                        .periodEnd(periodEnd)
                        .status(ChargeStatus.OPEN)
                        .notes("Monthly rent for period " + periodStart + " to " + periodEnd)
                        .build();

                chargeRepository.save(charge);
                chargesCreated++;

                log.info("Created MONTHLY_RENT charge for allocation {} | period {} → {} | amount {}",
                        terms.getAllocationId(), periodStart, periodEnd, terms.getMonthlyRent());
            }

            periodIndex++;
        }

        return chargesCreated;
    }

    /**
     * Generates due DAILY_RENT charges for active daily allocations.
     * Each day is created as an individual charge (1 charge per day) to allow day-level payment tracking.
     */
    private int generateDailyCharges(LocalDate today) {
        List<AllocationFinancialTerms> activeDailyTerms = financialTermsRepository.findActiveDailyAllocations();
        log.info("Found {} active daily allocation financial terms to evaluate.", activeDailyTerms.size());

        int count = 0;
        for (AllocationFinancialTerms terms : activeDailyTerms) {
            try {
                count += generateDailyChargesForAllocation(terms, today);
            } catch (Exception e) {
                log.error("Error generating daily rent charge for allocation {}: {}",
                        terms.getAllocationId(), e.getMessage(), e);
            }
        }
        return count;
    }

    private int generateDailyChargesForAllocation(AllocationFinancialTerms terms, LocalDate today) {
        if (terms.getDailyRate() == null || terms.getDailyRate().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return 0;
        }

        LocalDate startDate = terms.getAllocation().getCheckInDate();
        if (startDate == null) {
            startDate = terms.getBillingAnchorDate();
        }
        LocalDate toDate = terms.getAllocation().getCheckOutDate();

        int chargesCreated = 0;
        LocalDate currentDay = startDate;

        while (!currentDay.isAfter(today)) {
            // Respect planned checkout limit if to_date is defined
            if (toDate != null && currentDay.isAfter(toDate)) {
                break;
            }

            boolean exists = chargeRepository.existsByAllocation_IdAndChargeTypeAndPeriodStart(
                    terms.getAllocationId(),
                    ChargeType.DAILY_RENT,
                    currentDay
            );

            if (!exists) {
                Charge charge = Charge.builder()
                        .hostel(terms.getHostel())
                        .customer(terms.getAllocation().getCustomer())
                        .allocation(terms.getAllocation())
                        .chargeType(ChargeType.DAILY_RENT)
                        .amount(terms.getDailyRate())
                        .dueDate(currentDay)
                        .periodStart(currentDay)
                        .periodEnd(currentDay)
                        .status(ChargeStatus.OPEN)
                        .notes("Daily rent for " + currentDay)
                        .build();

                chargeRepository.save(charge);
                chargesCreated++;

                log.info("Created DAILY_RENT charge for allocation {} | date {} | amount {}",
                        terms.getAllocationId(), currentDay, terms.getDailyRate());
            }

            currentDay = currentDay.plusDays(1);
        }

        return chargesCreated;
    }
}

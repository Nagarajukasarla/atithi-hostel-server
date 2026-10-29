package com.atithi.aavas.scheduler;

import com.atithi.aavas.services.RentChargeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that runs once every 24 hours to generate MONTHLY_RENT
 * and DAILY_RENT charges for active customer allocations.
 *
 * <p>The actual logic is delegated entirely to {@link RentChargeService}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RentChargeGenerationJob {

    private final RentChargeService rentChargeService;

    /**
     * Fires once every 24 hours (86 400 000 ms).
     */
    @Scheduled(fixedRate = 24 * 60 * 60 * 1000)
    public void generateRentCharges() {
        log.info("Triggered scheduled rent charge generation (monthly & daily).");
        rentChargeService.generateDueCharges();
    }
}

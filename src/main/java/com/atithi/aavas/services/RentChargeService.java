package com.atithi.aavas.services;

/**
 * Generates due rent charges (both MONTHLY and DAILY) for active allocations.
 * Designed to be invoked by the scheduled RentChargeGenerationJob.
 */
public interface RentChargeService {

    /**
     * Scans active allocation financial terms, generates due monthly rent charges
     * and daily rent charges up to current date with duplicate protection.
     */
    void generateDueCharges();
}

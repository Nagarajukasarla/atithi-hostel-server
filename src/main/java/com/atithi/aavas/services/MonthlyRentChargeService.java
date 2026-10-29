package com.atithi.aavas.services;

/**
 * Generates MONTHLY_RENT charges for active allocations whose
 * billing cycle has completed. Retained for backwards compatibility;
 * {@link RentChargeService} is the generalized interface.
 */
public interface MonthlyRentChargeService extends RentChargeService {
}

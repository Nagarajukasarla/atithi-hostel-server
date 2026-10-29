package com.atithi.aavas.responses;

import com.atithi.aavas.models.RoomTariff;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record RoomTariffResponse(
        Long id,
        Long hostelId,
        Long roomTypeId,
        String roomTypeName,
        BigDecimal monthlyRent,
        BigDecimal dailyRate,
        BigDecimal securityDepositRequired,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static RoomTariffResponse fromEntity(RoomTariff tariff) {
        if (tariff == null) return null;
        return new RoomTariffResponse(
                tariff.getId(),
                tariff.getHostelId(),
                tariff.getRoomTypeId(),
                tariff.getRoomType() != null ? tariff.getRoomType().getName() : null,
                tariff.getMonthlyRent(),
                tariff.getDailyRate(),
                tariff.getSecurityDepositRequired(),
                tariff.getEffectiveFrom(),
                tariff.getEffectiveTo(),
                tariff.isActive(),
                tariff.getCreatedAt(),
                tariff.getUpdatedAt()
        );
    }
}

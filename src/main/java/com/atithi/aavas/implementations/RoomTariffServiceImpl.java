package com.atithi.aavas.implementations;

import com.atithi.aavas.models.Hostel;
import com.atithi.aavas.models.RoomTariff;
import com.atithi.aavas.models.RoomType;
import com.atithi.aavas.repositories.HostelRepository;
import com.atithi.aavas.repositories.RoomTariffRepository;
import com.atithi.aavas.repositories.RoomTypeRepository;
import com.atithi.aavas.requests.RoomTariffRequest;
import com.atithi.aavas.responses.RoomTariffResponse;
import com.atithi.aavas.services.RoomTariffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomTariffServiceImpl implements RoomTariffService {

    private final RoomTariffRepository roomTariffRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final HostelRepository hostelRepository;

    @Override
    @Transactional(readOnly = true)
    public RoomTariffResponse getCurrentTariff(Long hostelId, Long roomTypeId) {
        return getCurrentTariffOnDate(hostelId, roomTypeId, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public RoomTariffResponse getCurrentTariffOnDate(Long hostelId, Long roomTypeId, LocalDate date) {
        validateRoomTypeOwnership(hostelId, roomTypeId);
        return roomTariffRepository.findCurrentTariff(hostelId, roomTypeId, date)
                .map(RoomTariffResponse::fromEntity)
                .orElse(null);
    }

    @Override
    @Transactional
    public RoomTariffResponse createTariff(Long hostelId, Long roomTypeId, RoomTariffRequest request) {
        return createTariff(hostelId, roomTypeId, request, false);
    }

    @Override
    @Transactional
    public RoomTariffResponse createTariff(Long hostelId, Long roomTypeId, RoomTariffRequest request, boolean closeExisting) {
        validateRoomTypeOwnership(hostelId, roomTypeId);

        LocalDate effectiveFrom = request.effectiveFrom();
        // Use a far-future date for open-ended tariffs when checking overlap
        LocalDate effectiveTo = request.effectiveTo() != null ? request.effectiveTo() : LocalDate.of(9999, 12, 31);

        if (effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("Effective-to date must be on or after effective-from date.");
        }

        if (closeExisting) {
            // Find any active tariffs for this room type that overlap with the new effective period and close them
            List<RoomTariff> history = roomTariffRepository.findHistory(hostelId, roomTypeId);
            for (RoomTariff existing : history) {
                if (existing.isActive() && (existing.getEffectiveTo() == null || !existing.getEffectiveTo().isBefore(effectiveFrom))) {
                    if (effectiveFrom.isAfter(existing.getEffectiveFrom())) {
                        existing.setEffectiveTo(effectiveFrom.minusDays(1));
                    } else {
                        existing.setActive(false);
                        existing.setEffectiveTo(effectiveFrom);
                    }
                    roomTariffRepository.save(existing);
                    log.info("Closed existing tariff ID {} for room type {} prior to creating new revision (effectiveTo={})",
                            existing.getId(), roomTypeId, existing.getEffectiveTo());
                }
            }
        }

        // Check for overlapping active tariffs (excluding non-existent ID = -1 means no exclusion)
        boolean overlaps = roomTariffRepository.existsOverlappingTariff(
                hostelId, roomTypeId, effectiveFrom, effectiveTo, -1L);
        if (overlaps) {
            throw new IllegalArgumentException(
                    "An active tariff already exists for this room type that overlaps the given date range. " +
                            "Close the existing tariff first before creating a new one.");
        }

        Hostel hostel = hostelRepository.findById(hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Hostel not found with ID: " + hostelId));
        RoomType roomType = roomTypeRepository.findByIdAndHostel_Id(roomTypeId, hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Room type not found with ID: " + roomTypeId));

        RoomTariff tariff = RoomTariff.builder()
                .hostel(hostel)
                .roomType(roomType)
                .monthlyRent(request.monthlyRent())
                .dailyRate(request.dailyRate())
                .securityDepositRequired(request.securityDepositRequired())
                .effectiveFrom(effectiveFrom)
                .effectiveTo(request.effectiveTo())
                .active(true)
                .build();

        RoomTariff saved = roomTariffRepository.save(tariff);
        log.info("Created room tariff for room type '{}' (hostel {}): rent={}, deposit={}, from={}",
                roomType.getName(), hostelId, saved.getMonthlyRent(),
                saved.getSecurityDepositRequired(), saved.getEffectiveFrom());
        return RoomTariffResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public RoomTariffResponse updateTariff(Long hostelId, Long tariffId, RoomTariffRequest request) {
        RoomTariff tariff = roomTariffRepository.findByIdAndHostel_Id(tariffId, hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Room tariff not found with ID: " + tariffId));

        LocalDate effectiveFrom = request.effectiveFrom();
        LocalDate effectiveTo = request.effectiveTo() != null ? request.effectiveTo() : LocalDate.of(9999, 12, 31);

        if (effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("Effective-to date must be on or after effective-from date.");
        }

        // Exclude the current tariff ID so it can self-update without collision
        boolean overlaps = roomTariffRepository.existsOverlappingTariff(
                hostelId, tariff.getRoomTypeId(), effectiveFrom, effectiveTo, tariffId);
        if (overlaps) {
            throw new IllegalArgumentException(
                    "The updated date range overlaps with another active tariff for this room type.");
        }

        tariff.setMonthlyRent(request.monthlyRent());
        tariff.setDailyRate(request.dailyRate());
        tariff.setSecurityDepositRequired(request.securityDepositRequired());
        tariff.setEffectiveFrom(effectiveFrom);
        tariff.setEffectiveTo(request.effectiveTo());

        RoomTariff saved = roomTariffRepository.save(tariff);
        log.info("Updated room tariff ID {} for room type '{}' (hostel {}): rent={}, dailyRate={}, deposit={}, from={}",
                tariff.getId(), tariff.getRoomType().getName(), hostelId,
                saved.getMonthlyRent(), saved.getDailyRate(), saved.getSecurityDepositRequired(), saved.getEffectiveFrom());

        return RoomTariffResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTariffResponse> getTariffHistory(Long hostelId, Long roomTypeId) {
        validateRoomTypeOwnership(hostelId, roomTypeId);
        return roomTariffRepository.findHistory(hostelId, roomTypeId).stream()
                .map(RoomTariffResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoomTariffResponse getTariffById(Long hostelId, Long tariffId) {
        RoomTariff tariff = roomTariffRepository.findByIdAndHostel_Id(tariffId, hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Tariff not found with ID: " + tariffId));
        return RoomTariffResponse.fromEntity(tariff);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTariffResponse> getAllCurrentTariffs(Long hostelId) {
        List<RoomTariff> tariffs = roomTariffRepository.findAllCurrentTariffs(hostelId, LocalDate.now());
        java.util.Map<Long, RoomTariffResponse> latestPerRoomType = new java.util.LinkedHashMap<>();
        for (RoomTariff tariff : tariffs) {
            latestPerRoomType.putIfAbsent(tariff.getRoomTypeId(), RoomTariffResponse.fromEntity(tariff));
        }
        return new java.util.ArrayList<>(latestPerRoomType.values());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTariffResponse> getAllTariffs(Long hostelId) {
        return roomTariffRepository.findAllByHostelId(hostelId).stream()
                .map(RoomTariffResponse::fromEntity)
                .toList();
    }

    // ── private helpers ─────────────────────────────────────────────────────

    private void validateRoomTypeOwnership(Long hostelId, Long roomTypeId) {
        if (!roomTypeRepository.existsByIdAndHostel_Id(roomTypeId, hostelId)) {
            throw new IllegalArgumentException("Room type not found with ID: " + roomTypeId + " in this hostel.");
        }
    }
}

package com.atithi.aavas.services;

import com.atithi.aavas.requests.RoomTariffRequest;
import com.atithi.aavas.responses.RoomTariffResponse;

import java.time.LocalDate;
import java.util.List;

public interface RoomTariffService {

    /** Returns the current active tariff for a hostel + room type valid today. */
    RoomTariffResponse getCurrentTariff(Long hostelId, Long roomTypeId);

    /** Returns the current active tariff for a hostel + room type valid on a specific date. */
    RoomTariffResponse getCurrentTariffOnDate(Long hostelId, Long roomTypeId, LocalDate date);

    /** Creates a new tariff. Validates no overlap with existing active tariffs. */
    RoomTariffResponse createTariff(Long hostelId, Long roomTypeId, RoomTariffRequest request);

    /** Creates a new tariff. If closeExisting is true, automatically closes any overlapping active tariff before creating the new one. */
    RoomTariffResponse createTariff(Long hostelId, Long roomTypeId, RoomTariffRequest request, boolean closeExisting);

    /** In-place updates an existing tariff. Validates that updated dates do not overlap with other tariffs. */
    RoomTariffResponse updateTariff(Long hostelId, Long tariffId, RoomTariffRequest request);

    /** Full tariff history for a room type, newest first. */
    List<RoomTariffResponse> getTariffHistory(Long hostelId, Long roomTypeId);

    /** Loads a specific tariff by ID, verifying it belongs to the hostel. */
    RoomTariffResponse getTariffById(Long hostelId, Long tariffId);

    /** Returns all current active tariffs for all room types in a hostel valid today. */
    List<RoomTariffResponse> getAllCurrentTariffs(Long hostelId);

    /** Returns all tariffs configured for a hostel. */
    List<RoomTariffResponse> getAllTariffs(Long hostelId);
}


package com.atithi.aavas.repositories;

import com.atithi.aavas.models.RoomTariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTariffRepository extends JpaRepository<RoomTariff, Long> {

    Optional<RoomTariff> findByIdAndHostel_Id(Long id, Long hostelId);

    /**
     * Finds the current active tariff for a hostel + room type valid on a specific date.
     * A tariff is valid when: effectiveFrom <= date AND (effectiveTo IS NULL OR effectiveTo >= date).
     */
    @Query("""
            SELECT t FROM RoomTariff t
            WHERE t.hostel.id = :hostelId
              AND t.roomType.id = :roomTypeId
              AND t.active = true
              AND t.effectiveFrom <= :date
              AND (t.effectiveTo IS NULL OR t.effectiveTo >= :date)
            ORDER BY t.effectiveFrom DESC
            """)
    Optional<RoomTariff> findCurrentTariff(
            @Param("hostelId") Long hostelId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("date") LocalDate date
    );

    /** Full tariff history for a room type, newest first. */
    @Query("""
            SELECT t FROM RoomTariff t
            WHERE t.hostel.id = :hostelId
              AND t.roomType.id = :roomTypeId
            ORDER BY t.effectiveFrom DESC
            """)
    List<RoomTariff> findHistory(
            @Param("hostelId") Long hostelId,
            @Param("roomTypeId") Long roomTypeId
    );

    /**
     * Checks if any ACTIVE tariff overlaps the given date range for the same hostel + room type.
     * Used to prevent overlapping active tariff periods.
     * Excludes a specific tariff ID to allow self-updates.
     */
    @Query("""
            SELECT COUNT(t) > 0 FROM RoomTariff t
            WHERE t.hostel.id = :hostelId
              AND t.roomType.id = :roomTypeId
              AND t.active = true
              AND t.id <> :excludeId
              AND t.effectiveFrom <= :effectiveTo
              AND (t.effectiveTo IS NULL OR t.effectiveTo >= :effectiveFrom)
            """)
    boolean existsOverlappingTariff(
            @Param("hostelId") Long hostelId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo,
            @Param("excludeId") Long excludeId
    );

    /**
     * Finds all current active tariffs for a hostel valid on a specific date.
     */
    @Query("""
            SELECT t FROM RoomTariff t
            WHERE t.hostel.id = :hostelId
              AND t.active = true
              AND t.effectiveFrom <= :date
              AND (t.effectiveTo IS NULL OR t.effectiveTo >= :date)
            ORDER BY t.effectiveFrom DESC
            """)
    List<RoomTariff> findAllCurrentTariffs(
            @Param("hostelId") Long hostelId,
            @Param("date") LocalDate date
    );

    /**
     * Finds all tariffs for a hostel, newest first.
     */
    @Query("""
            SELECT t FROM RoomTariff t
            WHERE t.hostel.id = :hostelId
            ORDER BY t.createdAt DESC
            """)
    List<RoomTariff> findAllByHostelId(@Param("hostelId") Long hostelId);
}


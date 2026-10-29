package com.atithi.aavas.repositories;

import com.atithi.aavas.enums.AllocationStatus;
import com.atithi.aavas.enums.BillingMode;
import com.atithi.aavas.models.RoomAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomAllocationRepository extends JpaRepository<RoomAllocation, Long> {

    long countByHostel_IdAndStatus(Long hostelId, AllocationStatus status);

    long countByHostel_IdAndCheckInDateBetween(Long hostelId, LocalDate startDate, LocalDate endDate);

    long countByHostel_IdAndCheckInDate(Long hostelId, LocalDate date);

    long countByHostel_IdAndCheckOutDateBetween(Long hostelId, LocalDate startDate, LocalDate endDate);

    long countByHostel_IdAndCheckOutDate(Long hostelId, LocalDate date);

    Optional<RoomAllocation> findByIdAndHostel_Id(Long id, Long hostelId);

    List<RoomAllocation> findByHostel_IdOrderByCreatedAtDesc(Long hostelId);

    List<RoomAllocation> findByHostel_IdAndStatus(Long hostelId, AllocationStatus status);

    List<RoomAllocation> findByHostel_IdAndCustomer_IdOrderByCreatedAtDesc(Long hostelId, Long customerId);

    Optional<RoomAllocation> findByHostel_IdAndCustomer_IdAndStatus(Long hostelId, Long customerId, AllocationStatus status);

    List<RoomAllocation> findByHostel_IdAndRoom_IdAndStatus(Long hostelId, Long roomId, AllocationStatus status);

    long countByHostel_IdAndRoom_IdAndStatus(Long hostelId, Long roomId, AllocationStatus status);

    boolean existsByHostel_IdAndCustomer_IdAndStatus(Long hostelId, Long customerId, AllocationStatus status);

    @Query("""
            SELECT COUNT(ra) > 0 FROM RoomAllocation ra
            WHERE ra.hostel.id = :hostelId
              AND ra.room.id = :roomId
              AND ra.bedNumber = :bedNumber
              AND ra.status = :status
            """)
    boolean existsActiveAllocationForRoomAndBed(
            @Param("hostelId") Long hostelId,
            @Param("roomId") Long roomId,
            @Param("bedNumber") String bedNumber,
            @Param("status") AllocationStatus status
    );

    @Query("""
            SELECT COUNT(ra) FROM RoomAllocation ra
            JOIN AllocationFinancialTerms aft ON aft.allocation = ra
            WHERE ra.hostel.id = :hostelId
              AND ra.status = :status
              AND aft.billingMode = :billingMode
            """)
    long countByHostelAndStatusAndBillingMode(
            @Param("hostelId") Long hostelId,
            @Param("status") AllocationStatus status,
            @Param("billingMode") BillingMode billingMode
    );
}

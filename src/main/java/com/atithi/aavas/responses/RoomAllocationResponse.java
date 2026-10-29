package com.atithi.aavas.responses;

import com.atithi.aavas.enums.AllocationStatus;
import com.atithi.aavas.models.RoomAllocation;

import java.time.Instant;
import java.time.LocalDate;

public record RoomAllocationResponse(
        Long id,
        Long hostelId,
        Long customerId,
        String customerName,
        String customerMobile,
        Long roomId,
        String roomNumber,
        String floor,
        String bedNumber,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        AllocationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static RoomAllocationResponse fromEntity(RoomAllocation allocation) {
        if (allocation == null) {
            return null;
        }
        return new RoomAllocationResponse(
                allocation.getId(),
                allocation.getHostelId(),
                allocation.getCustomerId(),
                allocation.getCustomer() != null ? allocation.getCustomer().getFullName() : null,
                allocation.getCustomer() != null ? allocation.getCustomer().getMobileNumber() : null,
                allocation.getRoomId(),
                allocation.getRoom() != null ? allocation.getRoom().getRoomNumber() : null,
                allocation.getRoom() != null ? allocation.getRoom().getFloor() : null,
                allocation.getBedNumber(),
                allocation.getCheckInDate(),
                allocation.getCheckOutDate(),
                allocation.getStatus(),
                allocation.getCreatedAt(),
                allocation.getUpdatedAt()
        );
    }
}

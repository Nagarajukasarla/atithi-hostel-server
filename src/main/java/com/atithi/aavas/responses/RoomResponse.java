package com.atithi.aavas.responses;

import com.atithi.aavas.enums.RoomState;
import com.atithi.aavas.enums.RoomStatus;
import com.atithi.aavas.models.Room;

import java.time.Instant;

public record RoomResponse(
        Long id,
        String roomNumber,
        String floor,
        Long roomTypeId,
        String roomTypeName,
        Integer sharingCount,
        Integer maxCapacity,
        Integer occupied,
        RoomState roomState,
        RoomStatus roomStatus,
        Instant lastCleaned,
        String notes,
        Long hostelId,
        Instant createdAt,
        Instant updatedAt
) {
    public static RoomResponse fromEntity(Room room) {
        if (room == null) {
            return null;
        }
        return new RoomResponse(
                room.getId(),
                room.getRoomNumber(),
                room.getFloor(),
                room.getRoomTypeId(),
                room.getRoomType() != null ? room.getRoomType().getName() : null,
                room.getSharingCount(),
                room.getMaxCapacity(),
                room.getOccupied() != null ? room.getOccupied() : 0,
                room.getRoomState(),
                room.getRoomStatus(),
                room.getLastCleaned(),
                room.getNotes(),
                room.getHostelId(),
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }
}

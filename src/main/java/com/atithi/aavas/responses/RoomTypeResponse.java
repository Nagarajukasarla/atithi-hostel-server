package com.atithi.aavas.responses;

import com.atithi.aavas.models.RoomType;

import java.time.Instant;

public record RoomTypeResponse(
        Long id,
        Long hostelId,
        String name,
        Integer sharingCount,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static RoomTypeResponse fromEntity(RoomType roomType) {
        if (roomType == null) {
            return null;
        }

        return new RoomTypeResponse(
                roomType.getId(),
                roomType.getHostelId(),
                roomType.getName(),
                roomType.getSharingCount(),
                roomType.getActive(),
                roomType.getCreatedAt(),
                roomType.getUpdatedAt()
        );
    }
}

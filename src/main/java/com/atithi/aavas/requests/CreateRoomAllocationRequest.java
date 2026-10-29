package com.atithi.aavas.requests;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateRoomAllocationRequest(
        @NotNull(message = "Customer ID is required")
        Long customerId,

        @NotNull(message = "Room ID is required")
        Long roomId,

        String bedNumber,

        @NotNull(message = "Check-in date is required")
        LocalDate checkInDate,

        LocalDate checkOutDate
) {
    public CreateRoomAllocationRequest(
            Long customerId,
            Long roomId,
            String bedNumber,
            LocalDate checkInDate
    ) {
        this(customerId, roomId, bedNumber, checkInDate, null);
    }
}

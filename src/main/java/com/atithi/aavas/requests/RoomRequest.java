package com.atithi.aavas.requests;

import com.atithi.aavas.enums.RoomState;
import com.atithi.aavas.enums.RoomStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RoomRequest(
        @NotBlank(message = "Room number is required")
        String roomNumber,

        @NotBlank(message = "Floor is required")
        String floor,

        @NotNull(message = "Room type is required")
        Long roomTypeId,

        @NotNull(message = "Maximum capacity is required")
        @Min(value = 1, message = "Maximum capacity must be at least 1")
        Integer maxCapacity,

        @Min(value = 0, message = "Occupied beds cannot be negative")
        Integer occupied,

        RoomState roomState,

        RoomStatus roomStatus,

        Instant lastCleaned,

        String notes
) {}

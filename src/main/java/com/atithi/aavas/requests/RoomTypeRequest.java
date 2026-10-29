package com.atithi.aavas.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomTypeRequest(
        @NotBlank(message = "Room type name is required")
        String name,

        @NotNull(message = "Sharing count is required")
        @Min(value = 1, message = "Sharing count must be at least 1")
        Integer sharingCount,

        @NotNull(message = "Active status is required")
        Boolean active
) {}

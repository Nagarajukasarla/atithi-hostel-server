package com.atithi.aavas.requests;

import jakarta.validation.constraints.NotBlank;

public record UpdateHostelRequest(
        @NotBlank(message = "Hostel name is required")
        String name,

        String phone,
        String addressLane1,
        String addressLane2,
        String city,
        String state,
        String zipcode,
        Integer totalFloors
) {}

package com.atithi.aavas.responses;

import com.atithi.aavas.enums.HostelStatus;
import com.atithi.aavas.models.Hostel;

public record HostelDto(
        Long id,
        String name,
        String phone,
        String addressLane1,
        String addressLane2,
        String city,
        String state,
        String zipcode,
        HostelStatus status,
        String token,
        Integer totalFloors
) {
    public static HostelDto fromEntity(Hostel hostel) {
        if (hostel == null) {
            return null;
        }
        return new HostelDto(
                hostel.getId(),
                hostel.getName(),
                hostel.getPhone(),
                hostel.getAddressLane1(),
                hostel.getAddressLane2(),
                hostel.getCity(),
                hostel.getState(),
                hostel.getZipcode(),
                hostel.getStatus(),
                hostel.getToken(),
                hostel.getTotalFloors()
        );
    }
}

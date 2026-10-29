package com.atithi.aavas.services;

import com.atithi.aavas.requests.RoomTypeRequest;
import com.atithi.aavas.responses.RoomTypeResponse;

import java.util.List;

public interface RoomTypeService {

    List<RoomTypeResponse> getRoomTypes(Long hostelId);

    RoomTypeResponse createRoomType(Long hostelId, RoomTypeRequest request);

    RoomTypeResponse updateRoomType(Long hostelId, Long roomTypeId, RoomTypeRequest request);

    void deleteRoomType(Long hostelId, Long roomTypeId);
}

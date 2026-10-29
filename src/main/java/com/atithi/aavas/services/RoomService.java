package com.atithi.aavas.services;

import com.atithi.aavas.requests.RoomRequest;
import com.atithi.aavas.responses.RoomResponse;

import java.util.List;

public interface RoomService {

    List<RoomResponse> getRooms(Long hostelId, String floor);

    RoomResponse getRoomById(Long hostelId, Long id);

    RoomResponse createRoom(Long hostelId, RoomRequest request);

    RoomResponse updateRoom(Long hostelId, Long id, RoomRequest request);

    RoomResponse toggleCleanStatus(Long hostelId, Long id);

    void deleteRoom(Long hostelId, Long id);
}

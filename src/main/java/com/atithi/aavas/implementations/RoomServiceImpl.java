package com.atithi.aavas.implementations;

import com.atithi.aavas.enums.RoomState;
import com.atithi.aavas.enums.RoomStatus;
import com.atithi.aavas.models.Hostel;
import com.atithi.aavas.models.Room;
import com.atithi.aavas.models.RoomType;
import com.atithi.aavas.repositories.HostelRepository;
import com.atithi.aavas.repositories.RoomRepository;
import com.atithi.aavas.repositories.RoomTypeRepository;
import com.atithi.aavas.requests.RoomRequest;
import com.atithi.aavas.responses.RoomResponse;
import com.atithi.aavas.services.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final HostelRepository hostelRepository;
    private final RoomTypeRepository roomTypeRepository;

    @Override
    public List<RoomResponse> getRooms(Long hostelId, String floor) {
        if (hostelId == null) {
            throw new IllegalArgumentException("Hostel ID is required");
        }

        List<Room> rooms;
        if (floor != null && !floor.isBlank() && !floor.equalsIgnoreCase("All")) {
            rooms = roomRepository.findByHostel_IdAndFloorOrderByRoomNumberAsc(hostelId, floor.trim());
        } else {
            rooms = roomRepository.findByHostel_IdOrderByRoomNumberAsc(hostelId);
        }

        return rooms.stream()
                .map(RoomResponse::fromEntity)
                .toList();
    }

    @Override
    public RoomResponse getRoomById(Long hostelId, Long id) {
        Room room = roomRepository.findByIdAndHostel_Id(id, hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + id));
        return RoomResponse.fromEntity(room);
    }

    @Override
    @Transactional
    public RoomResponse createRoom(Long hostelId, RoomRequest request) {
        if (hostelId == null) {
            throw new IllegalArgumentException("Hostel ID is required");
        }

        String roomNumber = request.roomNumber().trim();
        if (roomRepository.existsByHostel_IdAndRoomNumber(hostelId, roomNumber)) {
            throw new IllegalArgumentException("Room number '" + roomNumber + "' already exists in this hostel");
        }

        Hostel hostel = hostelRepository.findById(hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Hostel not found with ID: " + hostelId));

        RoomType roomType = roomTypeRepository.findByIdAndHostel_Id(request.roomTypeId(), hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Room type not found with ID: " + request.roomTypeId()));

        RoomState state = request.roomState() != null ? request.roomState() : RoomState.CLEAN;
        Integer occupied = request.occupied() != null ? request.occupied() : 0;

        RoomStatus status = request.roomStatus();
        if (status == null) {
            if (occupied == 0) status = RoomStatus.VACANT;
            else if (occupied >= request.maxCapacity()) status = RoomStatus.FILLED;
            else status = RoomStatus.PARTIALLY_FILLED;
        }

        Instant lastCleaned = request.lastCleaned();
        if (lastCleaned == null && state == RoomState.CLEAN) {
            lastCleaned = Instant.now();
        }

        Room room = Room.builder()
                .roomNumber(roomNumber)
                .floor(request.floor().trim())
                .roomType(roomType)
                .maxCapacity(request.maxCapacity())
                .occupied(occupied)
                .roomState(state)
                .roomStatus(status)
                .lastCleaned(lastCleaned)
                .notes(request.notes() != null && !request.notes().isBlank() ? request.notes().trim() : null)
                .hostel(hostel)
                .build();

        Room saved = roomRepository.save(room);
        log.info("Created room '{}' on floor '{}' (type: {}, occupied: {}) for hostel id {}",
                saved.getRoomNumber(), saved.getFloor(), roomType.getName(), saved.getOccupied(), hostelId);
        return RoomResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(Long hostelId, Long id, RoomRequest request) {
        Room room = roomRepository.findByIdAndHostel_Id(id, hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + id));

        String newRoomNumber = request.roomNumber().trim();
        if (roomRepository.existsByHostel_IdAndRoomNumberAndIdNot(hostelId, newRoomNumber, id)) {
            throw new IllegalArgumentException("Room number '" + newRoomNumber + "' already exists in this hostel");
        }

        RoomType roomType = roomTypeRepository.findByIdAndHostel_Id(request.roomTypeId(), hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Room type not found with ID: " + request.roomTypeId()));

        room.setRoomNumber(newRoomNumber);
        room.setFloor(request.floor().trim());
        room.setRoomType(roomType);
        room.setMaxCapacity(request.maxCapacity());

        if (request.occupied() != null) {
            room.setOccupied(request.occupied());
        }

        if (request.roomState() != null) {
            if (room.getRoomState() != RoomState.CLEAN && request.roomState() == RoomState.CLEAN && request.lastCleaned() == null) {
                room.setLastCleaned(Instant.now());
            }
            room.setRoomState(request.roomState());
        }

        if (request.roomStatus() != null) {
            room.setRoomStatus(request.roomStatus());
        } else if (request.occupied() != null) {
            if (room.getOccupied() == 0) room.setRoomStatus(RoomStatus.VACANT);
            else if (room.getOccupied() >= room.getMaxCapacity()) room.setRoomStatus(RoomStatus.FILLED);
            else room.setRoomStatus(RoomStatus.PARTIALLY_FILLED);
        }

        if (request.lastCleaned() != null) {
            room.setLastCleaned(request.lastCleaned());
        }

        room.setNotes(request.notes() != null && !request.notes().isBlank() ? request.notes().trim() : null);

        Room updated = roomRepository.save(room);
        log.info("Updated room '{}' (id: {}, type: {}, occupied: {}) for hostel id {}",
                updated.getRoomNumber(), id, roomType.getName(), updated.getOccupied(), hostelId);
        return RoomResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public RoomResponse toggleCleanStatus(Long hostelId, Long id) {
        Room room = roomRepository.findByIdAndHostel_Id(id, hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + id));

        if (room.getRoomState() == RoomState.CLEAN) {
            room.setRoomState(RoomState.DIRTY);
        } else {
            room.setRoomState(RoomState.CLEAN);
            room.setLastCleaned(Instant.now());
        }

        Room updated = roomRepository.save(room);
        log.info("Toggled cleanliness for room '{}' to {}", updated.getRoomNumber(), updated.getRoomState());
        return RoomResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteRoom(Long hostelId, Long id) {
        Room room = roomRepository.findByIdAndHostel_Id(id, hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + id));

        if (room.getRoomStatus() == RoomStatus.FILLED || (room.getOccupied() != null && room.getOccupied() > 0)) {
            throw new IllegalArgumentException("Cannot delete room '" + room.getRoomNumber() + "' as it is currently occupied");
        }

        roomRepository.delete(room);
        log.info("Deleted room '{}' (id: {}) for hostel id {}", room.getRoomNumber(), id, hostelId);
    }
}

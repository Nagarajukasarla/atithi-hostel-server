package com.atithi.aavas.implementations;

import com.atithi.aavas.models.Hostel;
import com.atithi.aavas.models.RoomType;
import com.atithi.aavas.repositories.HostelRepository;
import com.atithi.aavas.repositories.RoomRepository;
import com.atithi.aavas.repositories.RoomTypeRepository;
import com.atithi.aavas.requests.RoomTypeRequest;
import com.atithi.aavas.responses.RoomTypeResponse;
import com.atithi.aavas.services.RoomTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomTypeServiceImpl implements RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final HostelRepository hostelRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeResponse> getRoomTypes(Long hostelId) {
        return roomTypeRepository.findByHostel_IdOrderBySharingCountAscNameAsc(hostelId).stream()
                .map(RoomTypeResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public RoomTypeResponse createRoomType(Long hostelId, RoomTypeRequest request) {
        String name = request.name().trim();
        if (roomTypeRepository.existsByHostel_IdAndNameIgnoreCase(hostelId, name)) {
            throw new IllegalArgumentException("A room type named '" + name + "' already exists");
        }

        Hostel hostel = hostelRepository.findById(hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Hostel not found with ID: " + hostelId));

        RoomType saved = roomTypeRepository.save(RoomType.builder()
                .hostel(hostel)
                .name(name)
                .sharingCount(request.sharingCount())
                .active(request.active())
                .build());

        log.info("Created room type '{}' for hostel {}", saved.getName(), hostelId);
        return RoomTypeResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public RoomTypeResponse updateRoomType(Long hostelId, Long roomTypeId, RoomTypeRequest request) {
        RoomType roomType = roomTypeRepository.findByIdAndHostel_Id(roomTypeId, hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Room type not found with ID: " + roomTypeId));

        String name = request.name().trim();
        if (roomTypeRepository.existsByHostel_IdAndNameIgnoreCaseAndIdNot(hostelId, name, roomTypeId)) {
            throw new IllegalArgumentException("A room type named '" + name + "' already exists");
        }

        roomType.setName(name);
        roomType.setSharingCount(request.sharingCount());
        roomType.setActive(request.active());

        RoomType saved = roomTypeRepository.save(roomType);
        log.info("Updated room type {} for hostel {}", roomTypeId, hostelId);
        return RoomTypeResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deleteRoomType(Long hostelId, Long roomTypeId) {
        RoomType roomType = roomTypeRepository.findByIdAndHostel_Id(roomTypeId, hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Room type not found with ID: " + roomTypeId));

        if (roomRepository.existsByRoomType_Id(roomTypeId)) {
            throw new IllegalStateException("Room type '" + roomType.getName() + "' is assigned to one or more rooms and cannot be deleted");
        }

        roomTypeRepository.delete(roomType);
        log.info("Deleted room type {} for hostel {}", roomTypeId, hostelId);
    }
}

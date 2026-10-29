package com.atithi.aavas.implementations;

import com.atithi.aavas.models.Hostel;
import com.atithi.aavas.repositories.HostelRepository;
import com.atithi.aavas.requests.UpdateHostelRequest;
import com.atithi.aavas.responses.HostelDto;
import com.atithi.aavas.services.HostelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HostelServiceImpl implements HostelService {

    private final HostelRepository hostelRepository;

    @Override
    public HostelDto getHostel(Long hostelId) {
        Hostel hostel = hostelRepository.findById(hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Hostel not found with id: " + hostelId));
        return HostelDto.fromEntity(hostel);
    }

    @Override
    @Transactional
    public HostelDto updateHostel(Long hostelId, UpdateHostelRequest request) {
        Hostel hostel = hostelRepository.findById(hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Hostel not found with id: " + hostelId));

        hostel.setName(request.name());
        hostel.setPhone(request.phone());
        hostel.setAddressLane1(request.addressLane1());
        hostel.setAddressLane2(request.addressLane2());
        hostel.setCity(request.city());
        hostel.setState(request.state());
        hostel.setZipcode(request.zipcode());
        if (request.totalFloors() != null) {
            hostel.setTotalFloors(request.totalFloors());
        }

        Hostel updated = hostelRepository.save(hostel);
        return HostelDto.fromEntity(updated);
    }
}

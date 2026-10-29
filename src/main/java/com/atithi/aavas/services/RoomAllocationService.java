package com.atithi.aavas.services;

import com.atithi.aavas.requests.CreateRoomAllocationRequest;
import com.atithi.aavas.responses.RoomAllocationResponse;

import java.util.List;

public interface RoomAllocationService {

    RoomAllocationResponse createAllocation(Long hostelId, CreateRoomAllocationRequest request);

    RoomAllocationResponse endAllocation(Long hostelId, Long allocationId);

    RoomAllocationResponse endAllocation(Long hostelId, Long allocationId, com.atithi.aavas.requests.EndRoomAllocationRequest request);

    com.atithi.aavas.responses.CheckoutPreviewResponse getCheckoutPreview(Long hostelId, Long allocationId, java.time.LocalDate checkOutDate);

    RoomAllocationResponse getActiveAllocationForCustomer(Long hostelId, Long customerId);

    List<RoomAllocationResponse> getAllocations(Long hostelId);

    List<RoomAllocationResponse> getAllocationsForCustomer(Long hostelId, Long customerId);

    List<RoomAllocationResponse> getActiveAllocationsForRoom(Long hostelId, Long roomId);
}

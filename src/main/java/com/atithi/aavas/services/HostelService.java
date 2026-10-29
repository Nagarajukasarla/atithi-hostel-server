package com.atithi.aavas.services;

import com.atithi.aavas.requests.UpdateHostelRequest;
import com.atithi.aavas.responses.HostelDto;

public interface HostelService {
    HostelDto getHostel(Long hostelId);
    HostelDto updateHostel(Long hostelId, UpdateHostelRequest request);
}

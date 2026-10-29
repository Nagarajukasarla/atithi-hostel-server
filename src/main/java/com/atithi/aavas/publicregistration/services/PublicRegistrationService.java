package com.atithi.aavas.publicregistration.services;

import com.atithi.aavas.publicregistration.dtos.PublicRegistrationInfoDto;
import com.atithi.aavas.publicregistration.dtos.PublicRegistrationRequest;
import com.atithi.aavas.responses.CustomerResponse;

public interface PublicRegistrationService {
    PublicRegistrationInfoDto getRegistrationInfo(String token);
    CustomerResponse registerGuest(String token, PublicRegistrationRequest request);
}

package com.atithi.aavas.publicregistration.controllers;

import com.atithi.aavas.publicregistration.dtos.PublicRegistrationInfoDto;
import com.atithi.aavas.publicregistration.dtos.PublicRegistrationRequest;
import com.atithi.aavas.publicregistration.services.PublicRegistrationService;
import com.atithi.aavas.responses.ApiResponse;
import com.atithi.aavas.responses.CustomerResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/public/registration", "/api/v1/public/registration"})
@RequiredArgsConstructor
public class PublicRegistrationController {

    private final PublicRegistrationService publicRegistrationService;

    @GetMapping("/{token}")
    public ResponseEntity<PublicRegistrationInfoDto> getRegistrationInfo(
            @PathVariable String token
    ) {
        PublicRegistrationInfoDto info = publicRegistrationService.getRegistrationInfo(token);
        return ResponseEntity.ok(info);
    }

    @PostMapping("/{token}")
    public ResponseEntity<ApiResponse<CustomerResponse>> registerGuest(
            @PathVariable String token,
            @Valid @RequestBody PublicRegistrationRequest request
    ) {
        CustomerResponse customer = publicRegistrationService.registerGuest(token, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Guest registration submitted successfully", customer));
    }
}

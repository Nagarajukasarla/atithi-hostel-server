package com.atithi.aavas.publicregistration.controllers;

import com.atithi.aavas.models.Hostel;
import com.atithi.aavas.models.UserPrincipal;
import com.atithi.aavas.publicregistration.services.QrService;
import com.atithi.aavas.repositories.HostelRepository;
import com.atithi.aavas.utils.TokenGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/qr")
@RequiredArgsConstructor
@Slf4j
public class QrController {

    private final QrService qrService;
    private final HostelRepository hostelRepository;

    @Value("${app.public-base-url:http://localhost:5173}")
    private String publicBaseUrl;

    @GetMapping(produces = "image/svg+xml")
    public ResponseEntity<byte[]> getHostelQr(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long hostelId = userPrincipal.getHostelId();
        Hostel hostel = hostelRepository.findById(hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Hostel not found with id: " + hostelId));

        String token = hostel.getToken();
        if (token == null || token.isBlank()) {
            // Safety fallback: if existing hostel lacked a token, assign and persist one
            token = TokenGenerator.generateToken();
            hostel.setToken(token);
            hostelRepository.save(hostel);
            log.info("Assigned missing token to hostel id: {}", hostelId);
        }

        String sanitizedBaseUrl = publicBaseUrl.replaceAll("/+$", "");
        String registrationUrl = sanitizedBaseUrl + "/public/customer-registration/" + token;

        String svg = qrService.generateSvg(registrationUrl);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/svg+xml"))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(svg.getBytes(StandardCharsets.UTF_8));
    }
}

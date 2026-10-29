package com.atithi.aavas.responses;

public record AuthResponse(
        String message,
        String token,
        UserDto user,
        HostelDto hostel
) {}

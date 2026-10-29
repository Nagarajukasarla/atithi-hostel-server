package com.atithi.aavas.responses;

import com.atithi.aavas.enums.UserRole;
import com.atithi.aavas.enums.UserStatus;
import com.atithi.aavas.models.User;

public record UserDto(
        Long id,
        Long hostelId,
        String name,
        String email,
        String phone,
        UserRole role,
        UserStatus status
) {
    public static UserDto fromEntity(User user) {
        return new UserDto(
                user.getId(),
                user.getHostelId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus()
        );
    }
}

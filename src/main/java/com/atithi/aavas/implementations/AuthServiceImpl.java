package com.atithi.aavas.implementations;

import com.atithi.aavas.enums.HostelStatus;
import com.atithi.aavas.enums.UserRole;
import com.atithi.aavas.enums.UserStatus;
import com.atithi.aavas.models.Hostel;
import com.atithi.aavas.models.User;
import com.atithi.aavas.repositories.HostelRepository;
import com.atithi.aavas.repositories.UserRepository;
import com.atithi.aavas.requests.LoginRequest;
import com.atithi.aavas.requests.SignupRequest;
import com.atithi.aavas.responses.AuthResponse;
import com.atithi.aavas.responses.HostelDto;
import com.atithi.aavas.responses.UserDto;
import com.atithi.aavas.security.JwtService;
import com.atithi.aavas.services.AuthService;
import com.atithi.aavas.utils.TokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final HostelRepository hostelRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("User already exists with email: " + request.email());
        }
        // 1. Create and persist Hostel
        String hostelToken = generateUniqueHostelToken();
        Hostel hostel = Hostel.builder()
                .name(request.hostelName())
                .phone(request.phone())
                .addressLane1(request.addressLane1())
                .addressLane2(request.addressLane2())
                .city(request.city())
                .state(request.state())
                .zipcode(request.zipcode())
                .status(HostelStatus.INACTIVE)
                .token(hostelToken)
                .build();
        Hostel savedHostel = hostelRepository.save(hostel);

        // 2. Create and persist User linked to the new Hostel
        User user = User.builder()
                .hostel(savedHostel)
                .name(request.ownerName())
                .email(request.email())
                .passwd(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .role(UserRole.OWNER)
                .status(UserStatus.INACTIVE)
                .build();
        User savedUser = userRepository.save(user);

        // 3. Generate JWT Token
        String token = jwtService.generateToken(savedUser);

        return new AuthResponse(
                "Signup successful",
                token,
                UserDto.fromEntity(savedUser),
                HostelDto.fromEntity(savedHostel)
        );
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                "Login successful",
                token,
                UserDto.fromEntity(user),
                HostelDto.fromEntity(user.getHostel())
        );
    }

    private String generateUniqueHostelToken() {
        String token;
        do {
            token = TokenGenerator.generateToken();
        } while (hostelRepository.existsByToken(token));
        return token;
    }
}

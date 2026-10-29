package com.atithi.aavas.services;

import com.atithi.aavas.requests.LoginRequest;
import com.atithi.aavas.requests.SignupRequest;
import com.atithi.aavas.responses.AuthResponse;

public interface AuthService {
    AuthResponse signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
}

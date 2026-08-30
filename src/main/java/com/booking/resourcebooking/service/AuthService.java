package com.booking.resourcebooking.service;

import com.booking.resourcebooking.dto.auth.LoginRequest;
import com.booking.resourcebooking.dto.auth.LoginResponse;
import com.booking.resourcebooking.entity.User;
import com.booking.resourcebooking.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        // Throws BadCredentialsException (mapped to 401) on failure.
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = (User) authentication.getPrincipal();
        String role = user.getRole().name();
        String token = jwtUtil.generateToken(user, role);

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(user.getUsername())
                .role(role)
                .expiresInMs(jwtUtil.getExpirationMs())
                .build();
    }
}

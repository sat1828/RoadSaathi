package com.roadsaathi.backend.service;

import com.roadsaathi.backend.dto.AuthResponse;
import com.roadsaathi.backend.dto.AuthResponse.UserDto;
import com.roadsaathi.backend.dto.LoginRequest;
import com.roadsaathi.backend.dto.RegisterRequest;
import com.roadsaathi.backend.exception.BadRequestException;
import com.roadsaathi.backend.model.User;
import com.roadsaathi.backend.repository.UserRepository;
import com.roadsaathi.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role("DRIVER")
                .build();

        user = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(org.springframework.security.core.userdetails.User
                .withUsername(user.getId().toString())
                .password(user.getPassword())
                .roles(user.getRole())
                .build());

        String refreshToken = jwtTokenProvider.generateRefreshToken(org.springframework.security.core.userdetails.User
                .withUsername(user.getId().toString())
                .password(user.getPassword())
                .roles(user.getRole())
                .build());

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .build();

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .user(userDto)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = jwtTokenProvider.generateToken(org.springframework.security.core.userdetails.User
                .withUsername(user.getId().toString())
                .password(user.getPassword())
                .roles(user.getRole())
                .build());

        String refreshToken = jwtTokenProvider.generateRefreshToken(org.springframework.security.core.userdetails.User
                .withUsername(user.getId().toString())
                .password(user.getPassword())
                .roles(user.getRole())
                .build());

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .build();

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .user(userDto)
                .build();
    }

    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    public UUID getUserIdFromToken(String token) {
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}

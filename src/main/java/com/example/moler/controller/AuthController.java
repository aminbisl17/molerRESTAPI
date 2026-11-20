package com.example.moler.controller;
import com.example.moler.configuration.TenantContext;
import com.example.moler.dto.LoginDTO;
import com.example.moler.dto.TokenResponseDTO;
import com.example.moler.model.menaxheri;
import com.example.moler.repository.MenaxheriRepository;
import com.example.moler.authentication.JwtUtil;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final MenaxheriRepository menaxheriRepository;
    private final AuthenticationManager menaxheriAuthManager;
    private final JwtUtil jwtUtil;

    public AuthController(
            MenaxheriRepository menaxheriRepository,
            @org.springframework.beans.factory.annotation.Qualifier("menaxheriAuthManager")
            AuthenticationManager menaxheriAuthManager,
            JwtUtil jwtUtil
    ) {
        this.menaxheriRepository = menaxheriRepository;
        this.menaxheriAuthManager = menaxheriAuthManager;
        this.jwtUtil = jwtUtil;
    }

    // 1. LOGIN
    @PostMapping("/menaxheri/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO dto) {

        try {
            menaxheriAuthManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            dto.username(),
                            dto.password()
                    )
            );

            menaxheri menaxheri = menaxheriRepository.findByUsername(dto.username())
                    .orElseThrow();

            String accessToken = jwtUtil.generateToken(
                    menaxheri.getId(),
                    menaxheri.getUsername(),
                    "ROLE_MENAXHER",
                    TenantContext.getTenantId()
            );

            String refreshToken = jwtUtil.generateRefreshToken(
                    menaxheri.getId(),
                    menaxheri.getUsername(),
                    "ROLE_MENAXHER",
                    TenantContext.getTenantId()
            );

            return ResponseEntity.ok(
                    new TokenResponseDTO(accessToken, refreshToken)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        }
    }

    // 2. REFRESH ACCESS TOKEN
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {

        String refreshToken = request.get("refreshToken");

        if (refreshToken == null || !jwtUtil.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid refresh token"));
        }

        Long tenantId = jwtUtil.extractTenantId(refreshToken);

        if (!tenantId.equals(TenantContext.getTenantId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Tenant mismatch"));
        }

        Long id = jwtUtil.extractId(refreshToken);
        String username = jwtUtil.extractUsername(refreshToken);
        String role = jwtUtil.extractRole(refreshToken);

        if (!menaxheriRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Menaxheri not found"));
        }

        String newAccessToken = jwtUtil.generateToken(
                id, username, role, tenantId
        );

        String newRefreshToken = jwtUtil.generateRefreshToken(
                id, username, role, tenantId
        );

        return ResponseEntity.ok(
                new TokenResponseDTO(newAccessToken, newRefreshToken)
        );
    }

    // 3. LOGOUT / DELETE REFRESH TOKEN
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {

        // This endpoint will need server-side refresh-token storage
        // to invalidate the token properly.

        return ResponseEntity.ok(
                Map.of("message", "Logout successful")
        );
    }
}
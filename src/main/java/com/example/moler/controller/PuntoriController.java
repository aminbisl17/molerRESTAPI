package com.example.moler.controller;

import com.example.moler.authentication.JwtUtil;
import com.example.moler.configuration.TenantContext;
import com.example.moler.dto.LoginDTO;
import com.example.moler.dto.PuntoriDTO.PuntoriResponseDTO;
import com.example.moler.dto.PuntoriDTO.UpdatePuntoriDTO;
import com.example.moler.model.puntori;
import com.example.moler.repository.PuntoriRepository;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/puntori")
public class PuntoriController {

    private final PuntoriRepository puntoriRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public PuntoriController(
            PuntoriRepository puntoriRepository,
            BCryptPasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.puntoriRepository = puntoriRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
public ResponseEntity<?> login(
        @RequestBody LoginDTO dto,
        HttpServletResponse response
) {

    if (dto.username() == null || dto.username().isBlank()
            || dto.password() == null || dto.password().isBlank()) {

        return ResponseEntity.badRequest()
                .body(Map.of("message", "Username and password are required"));
    }

    puntori puntori = puntoriRepository
            .findByUsername(dto.username().trim())
            .orElse(null);

    if (puntori == null ||
            !passwordEncoder.matches(dto.password(), puntori.getPassword())) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid username or password"));
    }

    String statusi = puntori.getStatusi();

    if (statusi == null || !"a".equalsIgnoreCase(statusi.trim())) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Your account is inactive"));
    }

    Long tenantId = TenantContext.getTenantId();

    String accessToken = jwtUtil.generateToken(
            puntori.getId(),
            puntori.getUsername(),
            "ROLE_PUNTOR",
            tenantId
    );

    String refreshToken = jwtUtil.generateRefreshToken(
            puntori.getId(),
            puntori.getUsername(),
            "ROLE_PUNTOR",
            tenantId
    );

    ResponseCookie cookie = ResponseCookie.from(
                    "PuntoriRefreshToken",
                    refreshToken
            )
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(7 * 24 * 60 * 60)
            .sameSite("None")
            .build();

    response.addHeader("Set-Cookie", cookie.toString());

    return ResponseEntity.ok(
            Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken
            )
    );
}

// REFRESH ACCESS TOKEN
@PostMapping("/refresh")
public ResponseEntity<?> refresh(
        @CookieValue(name = "PuntoriRefreshToken", required = false)
        String refreshToken
) {

    if (refreshToken == null || !jwtUtil.validateRefreshToken(refreshToken)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid refresh token"));
    }

    if (!"ROLE_PUNTOR".equals(jwtUtil.extractRole(refreshToken))) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Employee refresh token required"));
    }

    Long tenantId = jwtUtil.extractTenantId(refreshToken);

    if (tenantId == null || !tenantId.equals(TenantContext.getTenantId())) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Tenant mismatch"));
    }

    Long id = jwtUtil.extractId(refreshToken);
    String username = jwtUtil.extractUsername(refreshToken);

    puntori puntori = puntoriRepository.findById(id)
            .orElse(null);

    if (puntori == null ||
            !puntori.getUsername().equals(username)) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Employee not found"));
    }

    if (puntori.getStatusi() == null ||
            !"a".equalsIgnoreCase(puntori.getStatusi().trim())) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Employee account is inactive"));
    }

    String newAccessToken = jwtUtil.generateToken(
            id,
            username,
            "ROLE_PUNTOR",
            tenantId
    );

    String newRefreshToken = jwtUtil.generateRefreshToken(
            id,
            username,
            "ROLE_PUNTOR",
            tenantId
    );

    ResponseCookie cookie = ResponseCookie.from("PuntoriRefreshToken", newRefreshToken)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(7 * 24 * 60 * 60)
            .sameSite("None")
            .build();

    return ResponseEntity.ok()
            .header("Set-Cookie", cookie.toString())
            .body(Map.of("accessToken", newAccessToken));
}
    // GET LOGGED-IN EMPLOYEE DATA
    @GetMapping("/data")
    public ResponseEntity<?> getMyData(
            @RequestHeader("Authorization") String authorization
    ) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authorization token is required"));
        }

        String token = authorization.substring(7);

        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired token"));
        }

        if (!"ROLE_PUNTOR".equals(jwtUtil.extractRole(token))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Employee access required"));
        }

        Long tokenTenantId = jwtUtil.extractTenantId(token);
        Long currentTenantId = TenantContext.getTenantId();

        if (tokenTenantId == null || !tokenTenantId.equals(currentTenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Tenant mismatch"));
        }

        Long employeeId = jwtUtil.extractId(token);

        puntori puntori = puntoriRepository.findById(employeeId)
                .orElse(null);

        if (puntori == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Employee not found"));
        }

        PuntoriResponseDTO response = new PuntoriResponseDTO(
                puntori.getId(),
                puntori.getEmri(),
                puntori.getMbiemri(),
                puntori.getNumriTelefonit(),
                puntori.getEmail(),
                puntori.getUsername(),
                puntori.getPozita(),
                puntori.getRrogaDitore(),
                puntori.getStatusi()
        );

        return ResponseEntity.ok(response);
    }

    // UPDATE OWN ACCOUNT
@PutMapping("/data")
public ResponseEntity<?> updateMyData(
        @RequestHeader("Authorization") String authorization,
        @RequestBody UpdatePuntoriDTO dto
) {

    if (authorization == null || !authorization.startsWith("Bearer ")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Authorization token is required"));
    }

    String token = authorization.substring(7);

    if (!jwtUtil.validateToken(token)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid or expired token"));
    }

    if (!"ROLE_PUNTOR".equals(jwtUtil.extractRole(token))) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Employee access required"));
    }

    Long tokenTenantId = jwtUtil.extractTenantId(token);
    Long currentTenantId = TenantContext.getTenantId();

    if (tokenTenantId == null || !tokenTenantId.equals(currentTenantId)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Tenant mismatch"));
    }

    Long employeeId = jwtUtil.extractId(token);

    puntori puntori = puntoriRepository.findById(employeeId)
            .orElse(null);

    if (puntori == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Employee not found"));
    }

    // UPDATE EMAIL
    if (dto.email() != null) {

        String email = dto.email().trim();

        if (email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email cannot be empty"));
        }

        if (puntoriRepository.existsByEmailAndIdNot(email, employeeId)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email is already in use"));
        }

        puntori.setEmail(email);
    }

    // UPDATE USERNAME
    if (dto.username() != null) {

        String username = dto.username().trim();

        if (username.length() < 3) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Username must contain at least 3 characters"));
        }

        if (puntoriRepository.existsByUsernameAndIdNot(username, employeeId)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Username is already in use"));
        }

        puntori.setUsername(username);
    }

    // UPDATE PASSWORD
    if (dto.password() != null) {

        if (dto.password().length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Password must contain at least 8 characters"));
        }

        puntori.setPassword(passwordEncoder.encode(dto.password()));
    }

    puntori saved = puntoriRepository.save(puntori);

    PuntoriResponseDTO response = new PuntoriResponseDTO(
            saved.getId(),
            saved.getEmri(),
            saved.getMbiemri(),
            saved.getNumriTelefonit(),
            saved.getEmail(),
            saved.getUsername(),
            saved.getPozita(),
            saved.getRrogaDitore(),
            saved.getStatusi()
    );

    return ResponseEntity.ok(response);
}
}
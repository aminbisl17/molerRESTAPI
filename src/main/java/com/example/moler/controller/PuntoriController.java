package com.example.moler.controller;

import com.example.moler.authentication.JwtUtil;
import com.example.moler.configuration.TenantContext;
import com.example.moler.dto.LoginDTO;
import com.example.moler.dto.PuntoriDTO.PuntoriResponseDTO;
import com.example.moler.dto.PuntoriDTO.UpdatePuntoriDTO;
import com.example.moler.model.puntori;
import com.example.moler.repository.PuntoriRepository;
import com.example.moler.service.DashboardService;

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
    private final DashboardService dashboardService;

    public PuntoriController(
            PuntoriRepository puntoriRepository,
            BCryptPasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            DashboardService dashboardService
    ) {
        this.puntoriRepository = puntoriRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.dashboardService = dashboardService;
    }

    // GET /puntori/dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(
            @RequestHeader("Authorization") String authorization
    ) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Nevoitet tokeni i autorizuar"));
        }

        String token = authorization.substring(7);

        if (!"ROLE_PUNTOR".equals(jwtUtil.extractRole(token))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Nevoitet qasja e punonjësit"));
        }

        // Extract worker ID from JWT
        Long puntoriId = jwtUtil.extractId(token);

        return ResponseEntity.ok(
                dashboardService.getPuntoriDashboard(puntoriId)
        );
    }

    @PostMapping("/login")
public ResponseEntity<?> login(
        @RequestBody LoginDTO dto,
        HttpServletResponse response
) {

    if (dto.username() == null || dto.username().isBlank()
            || dto.password() == null || dto.password().isBlank()) {

        return ResponseEntity.badRequest()
                .body(Map.of("message", "Emri dhe fjalkalimi i përdoruesit janë të obligueshme!"));
    }

    puntori puntori = puntoriRepository
            .findByUsername(dto.username().trim())
            .orElse(null);

    if (puntori == null ||
            !passwordEncoder.matches(dto.password(), puntori.getPassword())) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Emri i përdoruesit ose fjalkalimi nuk është i sakt!"));
    }

    String statusi = puntori.getStatusi();

    if (statusi == null || !"a".equalsIgnoreCase(statusi.trim())) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Llogaria e juaj është joaktive"));
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

    // GET LOGGED-IN EMPLOYEE DATA
    @GetMapping("/data")
    public ResponseEntity<?> getMyData(
            @RequestHeader("Authorization") String authorization
    ) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Nevoitet tokeni i autorizuar"));
        }

        String token = authorization.substring(7);

        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Tokeni nuk është valid ose ka skaduar"));
        }

        if (!"ROLE_PUNTOR".equals(jwtUtil.extractRole(token))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Nevoitet qasja e punonjësit"));
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
                puntori.getUsername(),
                puntori.getPozita(),
                puntori.getRrogaDitore(),
                puntori.getStatusi(),
                puntori.getDataKrijimit(),
                puntori.getDataEDeaktivizimit()
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

    // UPDATE PHONE NUMBER
if (dto.numriTelefonit() != null) {

    String nrtel = dto.numriTelefonit().trim();

    if (nrtel.isBlank()) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Phone number cannot be empty"));
    }

    if (puntoriRepository.existsByNumriTelefonitAndIdNot(nrtel, employeeId)) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Phone number is already in use"));
    }

    puntori.setNumriTelefonit(nrtel);
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
            saved.getUsername(),
            saved.getPozita(),
            saved.getRrogaDitore(),
            saved.getStatusi(),
            saved.getDataKrijimit(),
            saved.getDataEDeaktivizimit()        
    );

    return ResponseEntity.ok(response);
}
}
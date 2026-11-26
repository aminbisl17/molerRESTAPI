package com.example.moler.controller;

import com.example.moler.dto.MenaxheriDTO.MenaxheriResponseDTO;
import com.example.moler.authentication.JwtUtil;
import com.example.moler.configuration.TenantContext;
import com.example.moler.dto.MenaxheriDTO.RegisterMenaxheri;
import com.example.moler.model.menaxheri;
import com.example.moler.model.puntori;
import com.example.moler.repository.MenaxheriRepository;
import com.example.moler.repository.PuntoriRepository;
import com.example.moler.service.DashboardService;
import com.example.moler.dto.MenaxheriDTO.UpdateMenaxheriDTO;
import com.example.moler.dto.MenaxheriDTO.UpdatePuntoriDTO;
import com.example.moler.dto.PuntoriDTO.PuntoriResponseDTO;
import com.example.moler.dto.PuntoriDTO.RegisterPuntoriDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/menaxheri")
public class MenaxheriController {

    private final MenaxheriRepository menaxheriRepository;

    private final JwtUtil jwtUtil;

    private final PuntoriRepository puntoriRepository;

    private final DashboardService dashboardService;


    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

public MenaxheriController(
        MenaxheriRepository menaxheriRepository,
        PuntoriRepository puntoriRepository,
        JwtUtil jwtUtil,
        DashboardService dashboardService) {

    this.menaxheriRepository = menaxheriRepository;
    this.puntoriRepository = puntoriRepository;
    this.jwtUtil = jwtUtil;
    this.dashboardService = dashboardService;
}

// --------------------------------------------------
// MENAXHERI DASHBOARD
// GET /menaxheri/dashboard
// --------------------------------------------------

@GetMapping("/dashboard")
public ResponseEntity<?> getDashboard(
        @RequestHeader("Authorization") String authorization) {

    if (authorization == null || !authorization.startsWith("Bearer ")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Access token is missing"));
    }

    String token = authorization.substring(7);

    if (!"ROLE_MENAXHER".equals(jwtUtil.extractRole(token))) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Nevoitet qasja e menaxherit"));
    }

    // Extract manager ID from JWT
    Long menaxheriId = jwtUtil.extractId(token);

    return ResponseEntity.ok(
            dashboardService.getMenaxheriDashboard(menaxheriId)
    );
}

    // --------------------------------------------------
    // REGISTER MENAXHERI
    // --------------------------------------------------

    @PostMapping("/register")
    public ResponseEntity<?> registerMenaxheri(
            @RequestBody RegisterMenaxheri dto) {

        // Only allow registration in moler_test
        if (!"moler_test".equals(TenantContext.getTenantKey())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message",
                            "Registration is only allowed for moler_test"));
        }

        // Validate required fields
        if (dto.emri() == null || dto.emri().isBlank()
                || dto.mbiemri() == null || dto.mbiemri().isBlank()
                || dto.numriTelefonit() == null || dto.numriTelefonit().isBlank()
                || dto.username() == null || dto.username().isBlank()
                || dto.password() == null || dto.password().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Required fields are missing"));
        }

        // Check existing username
        if (menaxheriRepository.existsByUsername(dto.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Username already exists"));
        }

        // Check existing phone number
        if (menaxheriRepository.existsByNumriTelefonit(
                dto.numriTelefonit())) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Phone number already exists"));
        }

        // Create manager
        menaxheri menaxheri = new menaxheri();

        menaxheri.setEmri(dto.emri());
        menaxheri.setMbiemri(dto.mbiemri());
        menaxheri.setNumriTelefonit(dto.numriTelefonit());
        menaxheri.setEmail(dto.email());
        menaxheri.setUsername(dto.username());

        // BCrypt password hashing
        menaxheri.setPassword(
                passwordEncoder.encode(dto.password())
        );

        menaxheriRepository.save(menaxheri);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Menaxheri registered successfully",
                        "username", menaxheri.getUsername()
                ));
    }

    // --------------------------------------------------
// GET LOGGED-IN MENAXHERI
// --------------------------------------------------

@GetMapping("/data")
public ResponseEntity<?> getMyProfile(
        @RequestHeader("Authorization") String authorization) {

    if (authorization == null || !authorization.startsWith("Bearer ")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Access token is missing"));
    }

    String token = authorization.substring(7);

    // Extract manager ID from JWT
    Long menaxheriId = jwtUtil.extractId(token);

    // Find manager using token ID
    menaxheri menaxheri = menaxheriRepository.findById(menaxheriId)
            .orElse(null);

    if (menaxheri == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Menaxheri not found"));
    }

    MenaxheriResponseDTO response = new MenaxheriResponseDTO(
            menaxheri.getId(),
            menaxheri.getEmri(),
            menaxheri.getMbiemri(),
            menaxheri.getNumriTelefonit(),
            menaxheri.getEmail(),
            menaxheri.getUsername()
    );

    return ResponseEntity.ok(response);
}

// --------------------------------------------------
// UPDATE LOGGED-IN MENAXHERI
// --------------------------------------------------

@PutMapping("/update")
public ResponseEntity<?> updateMyProfile(
        @RequestHeader("Authorization") String authorization,
        @RequestBody UpdateMenaxheriDTO dto) {

    String token = authorization.substring(7);

    Long menaxheriId = jwtUtil.extractId(token);

    menaxheri menaxheri = menaxheriRepository.findById(menaxheriId)
            .orElse(null);

    if (menaxheri == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Menaxheri not found"));
    }

    // Update personal information
    if (dto.emri() != null && !dto.emri().isBlank()) {
        menaxheri.setEmri(dto.emri());
    }

    if (dto.mbiemri() != null && !dto.mbiemri().isBlank()) {
        menaxheri.setMbiemri(dto.mbiemri());
    }

    if (dto.numriTelefonit() != null && !dto.numriTelefonit().isBlank()) {

        if (!dto.numriTelefonit().equals(menaxheri.getNumriTelefonit())
                && menaxheriRepository.existsByNumriTelefonit(dto.numriTelefonit())) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Phone number already exists"));
        }

        menaxheri.setNumriTelefonit(dto.numriTelefonit());
    }

    if (dto.email() != null && !dto.email().isBlank()) {
        menaxheri.setEmail(dto.email());
    }

    if (dto.username() != null && !dto.username().isBlank()) {

        if (!dto.username().equals(menaxheri.getUsername())
                && menaxheriRepository.existsByUsername(dto.username())) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Username already exists"));
        }

        menaxheri.setUsername(dto.username());
    }

    // Update password using BCrypt
    if (dto.password() != null && !dto.password().isBlank()) {

        if (dto.password().length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Password must contain at least 8 characters"));
        }

        menaxheri.setPassword(passwordEncoder.encode(dto.password()));
    }

    menaxheriRepository.save(menaxheri);

    MenaxheriResponseDTO response = new MenaxheriResponseDTO(
            menaxheri.getId(),
            menaxheri.getEmri(),
            menaxheri.getMbiemri(),
            menaxheri.getNumriTelefonit(),
            menaxheri.getEmail(),
            menaxheri.getUsername()
    );

    return ResponseEntity.ok(response);
}

// --------------------------------------------------
// REGISTER PUNTORI
// --------------------------------------------------

@PostMapping("/puntori/register")
public ResponseEntity<?> registerPuntori(
        @RequestBody RegisterPuntoriDTO dto) {

    // Validate required fields
    if (dto.emri() == null || dto.emri().isBlank()
            || dto.mbiemri() == null || dto.mbiemri().isBlank()
            || dto.numriTelefonit() == null || dto.numriTelefonit().isBlank()
            || dto.username() == null || dto.username().isBlank()
            || dto.password() == null || dto.password().isBlank()
            || dto.pozita() == null || dto.pozita().isBlank()
            || dto.rrogaDitore() == null) {

        return ResponseEntity.badRequest()
                .body(Map.of("message", "Required fields are missing"));
    }

    // Validate password length
    if (dto.password().length() < 8) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Password must contain at least 8 characters"));
    }

    // Validate position: p, nm, m
    String pozita = dto.pozita().trim().toLowerCase();

    if (!java.util.Set.of("p", "nm", "m").contains(pozita)) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Pozita must be p, nm, or m"));
    }

    // Validate daily salary
    if (dto.rrogaDitore().signum() < 0) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Daily salary cannot be negative"));
    }

    // Check existing username
    if (puntoriRepository.existsByUsername(dto.username())) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Username already exists"));
    }

    // Check existing phone number
    if (puntoriRepository.existsByNumriTelefonit(dto.numriTelefonit())) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Phone number already exists"));
    }

    // Create employee
    puntori puntori = new puntori();

    puntori.setEmri(dto.emri().trim());
    puntori.setMbiemri(dto.mbiemri().trim());
    puntori.setNumriTelefonit(dto.numriTelefonit().trim());
    puntori.setUsername(dto.username().trim());

    // Hash password
    puntori.setPassword(passwordEncoder.encode(dto.password()));

    // Employee position and salary
    puntori.setPozita(pozita);
    puntori.setRrogaDitore(dto.rrogaDitore());

    // Default status
    puntori.setStatusi("a");

    // Save employee
    puntori savedPuntori = puntoriRepository.save(puntori);

    // Return response DTO
    PuntoriResponseDTO response = new PuntoriResponseDTO(
            savedPuntori.getId(),
            savedPuntori.getEmri(),
            savedPuntori.getMbiemri(),
            savedPuntori.getNumriTelefonit(),
            savedPuntori.getUsername(),
            savedPuntori.getPozita(),
            savedPuntori.getRrogaDitore(),
            savedPuntori.getStatusi(),
            savedPuntori.getDataKrijimit(),
            savedPuntori.getDataEDeaktivizimit()
    );

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}

// PUT /menaxheri/puntori/{id}
@PutMapping("/puntori/{id}")
public ResponseEntity<?> updatePuntori(
        @PathVariable Long id,
        @RequestBody UpdatePuntoriDTO dto) {

    puntori puntori = puntoriRepository.findById(id).orElse(null);

    if (puntori == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Puntori not found"));
    }

    // Validate position
    if (dto.pozita() != null && !dto.pozita().isBlank()) {
        String pozita = dto.pozita().trim().toLowerCase();

        if (!java.util.Set.of("p", "nm", "m").contains(pozita)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Pozita must be p, nm, or m"));
        }

        puntori.setPozita(pozita);
    }

    // Validate status
    if (dto.statusi() != null && !dto.statusi().isBlank()) {
        String statusi = dto.statusi().trim().toLowerCase();

        if (!java.util.Set.of("a", "ja").contains(statusi)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Statusi must be a (aktiv) or ja (jo aktiv)"));
        }

        puntori.setStatusi(statusi);
    }

    // Update ordinary fields
    if (dto.emri() != null && !dto.emri().isBlank()) {
        puntori.setEmri(dto.emri().trim());
    }

    if (dto.mbiemri() != null && !dto.mbiemri().isBlank()) {
        puntori.setMbiemri(dto.mbiemri().trim());
    }

    if (dto.numriTelefonit() != null && !dto.numriTelefonit().isBlank()) {

        String phone = dto.numriTelefonit().trim();

        if (puntoriRepository.existsByNumriTelefonitAndIdNot(phone, id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Phone number already exists"));
        }

        puntori.setNumriTelefonit(phone);
    }


    if (dto.rrogaDitore() != null) {
        if (dto.rrogaDitore().signum() < 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Daily salary cannot be negative"));
        }

        puntori.setRrogaDitore(dto.rrogaDitore());
    }

    puntori updated = puntoriRepository.save(puntori);

    PuntoriResponseDTO response = new PuntoriResponseDTO(
            updated.getId(),
            updated.getEmri(),
            updated.getMbiemri(),
            updated.getNumriTelefonit(),
            updated.getUsername(),
            updated.getPozita(),
            updated.getRrogaDitore(),
            updated.getStatusi(),
            updated.getDataKrijimit(),
            updated.getDataEDeaktivizimit()
    );

    return ResponseEntity.ok(response);
}
// DELETE /menaxheri/puntori/{id}
@DeleteMapping("/puntori/{id}")
public ResponseEntity<?> deletePuntori(@PathVariable Long id) {

    puntori puntori = puntoriRepository.findById(id).orElse(null);

    if (puntori == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Puntori not found"));
    }

    puntoriRepository.delete(puntori);

    return ResponseEntity.ok(
            Map.of("message", "Puntori deleted successfully")
    );
}
// GET /menaxheri/puntori
@GetMapping("/puntori")
public ResponseEntity<?> getAllPuntori() {

    List<puntori> puntoret = puntoriRepository.findAll();

    List<PuntoriResponseDTO> response = puntoret.stream()
            .map(puntori -> new PuntoriResponseDTO(
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
            ))
            .toList();

    return ResponseEntity.ok(response);
}
}

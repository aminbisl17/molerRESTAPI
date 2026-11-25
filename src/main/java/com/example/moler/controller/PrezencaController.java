package com.example.moler.controller;

import com.example.moler.authentication.JwtUtil;
import com.example.moler.dto.Prezenca.AplikoPrezenceRequest;
import com.example.moler.dto.Prezenca.PrezencaFormDataResponse;
import com.example.moler.model.prezencapuntoreve;
import com.example.moler.service.PrezencaService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/prezenca")
public class PrezencaController {

    private final PrezencaService prezencaService;
        private final JwtUtil jwtUtil;

    public PrezencaController(PrezencaService prezencaService, JwtUtil jwtUtil) {
        this.prezencaService = prezencaService;
        this.jwtUtil = jwtUtil;
    }

  
    @PostMapping("/apliko")
public ResponseEntity<?> aplikoPrezence(
        @RequestHeader("Authorization") String authorization,
        @RequestBody AplikoPrezenceRequest request) {

    if (authorization == null || !authorization.startsWith("Bearer ")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Access token is missing"));
    }

    String token = authorization.substring(7);

    // Extract worker ID from JWT
    Long puntoriId = jwtUtil.extractId(token);

    // Apply for attendance
    List<prezencapuntoreve> prezencat =
            prezencaService.aplikoPrezence(request, puntoriId);

    return ResponseEntity.noContent().build();
}

   
@GetMapping("/form-data")
public ResponseEntity<?> getFormData(
        @RequestHeader("Authorization") String authorization) {

    if (authorization == null || !authorization.startsWith("Bearer ")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Access token is missing"));
    }

    String token = authorization.substring(7);

    // Extract worker ID from JWT
    Long puntoriId = jwtUtil.extractId(token);

    return ResponseEntity.ok(
            prezencaService.getFormData(puntoriId)
    );
}
}
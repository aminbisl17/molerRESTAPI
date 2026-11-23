
package com.example.moler.controller;

import com.example.moler.authentication.JwtUtil;
import com.example.moler.dto.Projekti.CreateProjektiRequest;
import com.example.moler.dto.Projekti.ProjektiResponse;
import com.example.moler.service.ProjektiService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/projekti")
public class ProjektiController {

    private final ProjektiService projektiService;
    private final JwtUtil jwtUtil;

    public ProjektiController(
            ProjektiService projektiService,
            JwtUtil jwtUtil
    ) {
        this.projektiService = projektiService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createProjekti(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @RequestBody CreateProjektiRequest request
    ) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Access token is missing"));
        }

        String token = authorization.substring(7);

        // Extract manager ID from JWT
        Long menaxheriId = jwtUtil.extractId(token);

        ProjektiResponse response =
                projektiService.createProjekti(request, menaxheriId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

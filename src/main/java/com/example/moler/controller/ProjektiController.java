
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
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization,

            @RequestBody CreateProjektiRequest request
    ) {

        // 1. Check authorization header
        if (authorization == null || authorization.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "status", 401,
                            "message", "Access token mungon."
                    ));
        }

        // 2. Check Bearer token format
        if (!authorization.startsWith("Bearer ")
                || authorization.substring(7).isBlank()) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "status", 401,
                            "message", "Formati i access token eshte i pavlefshem."
                    ));
        }

        // 3. Extract manager ID from JWT
        String token = authorization.substring(7);

        Long menaxheriId;

        try {
            menaxheriId = jwtUtil.extractId(token);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "status", 401,
                            "message", "Access token eshte i pavlefshem ose ka skaduar."
                    ));
        }

        // 4. Create project
        ProjektiResponse response =
                projektiService.createProjekti(request, menaxheriId);

        // 5. Return created project
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }
}

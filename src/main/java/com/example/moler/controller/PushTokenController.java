package com.example.moler.controller;

import com.example.moler.authentication.JwtUtil;
import com.example.moler.dto.PushToken.PushTokenRequest;
import com.example.moler.service.PushTokenService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/push-token")
public class PushTokenController {

    private final PushTokenService pushTokenService;
    private final JwtUtil jwtUtil;

    public PushTokenController(PushTokenService pushTokenService, JwtUtil jwtUtil) {
        this.pushTokenService = pushTokenService;
        this.jwtUtil = jwtUtil;
    }

    // POST /push-token — call after login and on every app start
    @PostMapping
    public ResponseEntity<?> regjistroToken(
            @RequestHeader("Authorization") String authorization,
            @RequestBody PushTokenRequest request) {

        return meRol(authorization, (idPerdoruesit, eshteMenaxher) -> {
            pushTokenService.regjistroToken(
                    request == null ? null : request.token(),
                    request == null ? null : request.platform(),
                    idPerdoruesit,
                    eshteMenaxher
            );
            return ResponseEntity.noContent().build();
        });
    }

    // DELETE /push-token — call on logout, before the JWT is removed
    @DeleteMapping
    public ResponseEntity<?> fshiToken(
            @RequestHeader("Authorization") String authorization,
            @RequestBody PushTokenRequest request) {

        return meRol(authorization, (idPerdoruesit, eshteMenaxher) -> {
            pushTokenService.fshiToken(
                    request == null ? null : request.token(),
                    idPerdoruesit,
                    eshteMenaxher
            );
            return ResponseEntity.noContent().build();
        });
    }

    private interface Veprim {
        ResponseEntity<?> ekzekuto(Long idPerdoruesit, boolean eshteMenaxher);
    }

    /** Reads user id and role from the JWT; only workers and managers are allowed. */
    private ResponseEntity<?> meRol(String authorization, Veprim veprim) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Access token is missing"));
        }

        String token = authorization.substring(7);
        String role = jwtUtil.extractRole(token);

        boolean eshteMenaxher = "ROLE_MENAXHER".equals(role);

        if (!eshteMenaxher && !"ROLE_PUNTOR".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Nuk keni qasje"));
        }

        return veprim.ekzekuto(jwtUtil.extractId(token), eshteMenaxher);
    }
}

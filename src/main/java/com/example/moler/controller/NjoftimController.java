package com.example.moler.controller;

import com.example.moler.authentication.JwtUtil;
import com.example.moler.service.NjoftimService;
import com.example.moler.service.NjoftimService.Perdoruesi;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.function.Function;

/**
 * Notifications tab for the logged-in worker or manager.
 * The user always comes from the JWT; nobody can see or change another user's notifications.
 */
@RestController
@RequestMapping("/njoftimet")
public class NjoftimController {

    private final NjoftimService njoftimService;
    private final JwtUtil jwtUtil;

    public NjoftimController(NjoftimService njoftimService, JwtUtil jwtUtil) {
        this.njoftimService = njoftimService;
        this.jwtUtil = jwtUtil;
    }

    // GET /njoftimet?page=0&size=20 — my notifications, newest first
    @GetMapping
    public ResponseEntity<?> listo(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return meperdorues(authorization, p ->
                ResponseEntity.ok(njoftimService.listo(p, page, size)));
    }

    // GET /njoftimet/te-palexuara/numri — unread count for the tab badge
    @GetMapping("/te-palexuara/numri")
    public ResponseEntity<?> numriTePalexuarave(
            @RequestHeader("Authorization") String authorization) {

        return meperdorues(authorization, p ->
                ResponseEntity.ok(Map.of("numri", njoftimService.numriTePalexuarave(p))));
    }

    // PATCH /njoftimet/{id}/lexuar — mark one as read
    @PatchMapping("/{id}/lexuar")
    public ResponseEntity<?> shenoSiTeLexuar(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {

        return meperdorues(authorization, p ->
                ResponseEntity.ok(njoftimService.shenoSiTeLexuar(p, id)));
    }

    // PATCH /njoftimet/lexuar — mark all as read
    @PatchMapping("/lexuar")
    public ResponseEntity<?> shenoTeGjithaSiTeLexuara(
            @RequestHeader("Authorization") String authorization) {

        return meperdorues(authorization, p ->
                ResponseEntity.ok(Map.of("perditesuara", njoftimService.shenoTeGjithaSiTeLexuara(p))));
    }

    // DELETE /njoftimet/{id} — delete one
    @DeleteMapping("/{id}")
    public ResponseEntity<?> fshi(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {

        return meperdorues(authorization, p -> {
             njoftimService.fshi(p, id);
            return ResponseEntity.noContent().build();
        });
    }

    // DELETE /njoftimet — delete all of mine
    @DeleteMapping
    public ResponseEntity<?> fshiTeGjitha(
            @RequestHeader("Authorization") String authorization) {

        return meperdorues(authorization, p ->
                ResponseEntity.ok(Map.of("fshira", njoftimService.fshiTeGjitha(p))));
    }

    /** Reads id and role from the JWT; only workers and managers are allowed. */
    private ResponseEntity<?> meperdorues(
            String authorization,
            Function<Perdoruesi, ResponseEntity<?>> veprim) {

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

        return veprim.apply(new Perdoruesi(jwtUtil.extractId(token), eshteMenaxher));
    }
}

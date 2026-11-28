package com.example.moler.controller;

import com.example.moler.authentication.JwtUtil;
import com.example.moler.dto.Kerkesa.AprovoKerkesenRequest;
import com.example.moler.dto.Kerkesa.KrijoKerkesenRequest;
import com.example.moler.dto.Kerkesa.PerditesoKerkesenRequest;
import com.example.moler.dto.Kerkesa.RefuzoKerkesenRequest;
import com.example.moler.service.KerkesaService;
import com.example.moler.service.NjoftimService.Perdoruesi;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.function.Function;

/**
 * Requests. The user always comes from the JWT.
 * - Workers: create (sent to a chosen manager), list, view, edit and delete their own requests.
 * - Managers: cannot create; list and view requests sent to them, approve / decline.
 */
@RestController
@RequestMapping("/kerkesat")
public class KerkesaController {

    private final KerkesaService kerkesaService;
    private final JwtUtil jwtUtil;

    public KerkesaController(KerkesaService kerkesaService, JwtUtil jwtUtil) {
        this.kerkesaService = kerkesaService;
        this.jwtUtil = jwtUtil;
    }

    // POST /kerkesat — worker only, sent to the chosen manager
    @PostMapping
    public ResponseEntity<?> krijo(
            @RequestHeader("Authorization") String authorization,
            @RequestBody KrijoKerkesenRequest request) {

        return puntor(authorization, p ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(kerkesaService.krijo(p.id(), request)));
    }

    // GET /kerkesat/menaxheret — worker only, managers to choose from when creating
    @GetMapping("/menaxheret")
    public ResponseEntity<?> menaxheret(
            @RequestHeader("Authorization") String authorization) {

        return puntor(authorization, p ->
                ResponseEntity.ok(kerkesaService.menaxheretPerZgjedhje()));
    }

    // GET /kerkesat?statusi=w — worker: own requests, manager: requests sent to them
    @GetMapping
    public ResponseEntity<?> listo(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) String statusi) {

        return meperdorues(authorization, p ->
                ResponseEntity.ok(kerkesaService.listo(p, statusi)));
    }

    // GET /kerkesat/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> merr(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {

        return meperdorues(authorization, p ->
                ResponseEntity.ok(kerkesaService.merr(p, id)));
    }

    // PUT /kerkesat/{id} — worker only, own request while waiting
    @PutMapping("/{id}")
    public ResponseEntity<?> perditeso(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id,
            @RequestBody PerditesoKerkesenRequest request) {

        return puntor(authorization, p ->
                ResponseEntity.ok(kerkesaService.perditeso(p.id(), id, request)));
    }

    // PATCH /kerkesat/{id}/aprovo — manager only
    @PatchMapping("/{id}/aprovo")
    public ResponseEntity<?> aprovo(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id,
            @RequestBody(required = false) AprovoKerkesenRequest request) {

        return menaxher(authorization, p ->
                ResponseEntity.ok(kerkesaService.aprovo(p.id(), id, request)));
    }

    // PATCH /kerkesat/{id}/refuzo — manager only
    @PatchMapping("/{id}/refuzo")
    public ResponseEntity<?> refuzo(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id,
            @RequestBody(required = false) RefuzoKerkesenRequest request) {

        return menaxher(authorization, p ->
                ResponseEntity.ok(kerkesaService.refuzo(p.id(), id, request)));
    }

    // DELETE /kerkesat/{id} — worker only, own request while waiting
    @DeleteMapping("/{id}")
    public ResponseEntity<?> fshi(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {

        return puntor(authorization, p -> {
            kerkesaService.fshi(p.id(), id);
            return ResponseEntity.noContent().build();
        });
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

    /** Same as meperdorues, but only managers are allowed. */
    private ResponseEntity<?> menaxher(
            String authorization,
            Function<Perdoruesi, ResponseEntity<?>> veprim) {

        return meperdorues(authorization, p -> {
            if (!p.eshteMenaxher()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Nevoitet qasja e menaxherit"));
            }
            return veprim.apply(p);
        });
    }

    /** Same as meperdorues, but only workers are allowed. */
    private ResponseEntity<?> puntor(
            String authorization,
            Function<Perdoruesi, ResponseEntity<?>> veprim) {

        return meperdorues(authorization, p -> {
            if (p.eshteMenaxher()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Vetem puntoret mund ta bejne kete veprim"));
            }
            return veprim.apply(p);
        });
    }
}

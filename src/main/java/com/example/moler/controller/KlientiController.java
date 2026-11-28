package com.example.moler.controller;

import com.example.moler.dto.Klienti.CreateKlientiRequest;
import com.example.moler.dto.Klienti.KlientiResponse;
import com.example.moler.dto.Klienti.UpdateKlientiRequest;
import com.example.moler.dto.Projekti.ProjektiResponse;
import com.example.moler.service.KlientiService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Access is restricted to MENAXHER in SecurityConfig ("/klienti/**")
@RestController
@RequestMapping("/klienti")
public class KlientiController {

    private final KlientiService klientiService;

    public KlientiController(KlientiService klientiService) {
        this.klientiService = klientiService;
    }

    @PostMapping
    public ResponseEntity<KlientiResponse> createKlienti(
            @RequestBody CreateKlientiRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(klientiService.createKlienti(request));
    }

    @GetMapping
    public ResponseEntity<List<KlientiResponse>> getAllKlientet() {
        return ResponseEntity.ok(klientiService.getAllKlientet());
    }

    @GetMapping("/{id}")
    public ResponseEntity<KlientiResponse> getKlientiById(@PathVariable Long id) {
        return ResponseEntity.ok(klientiService.getKlientiById(id));
    }

    @GetMapping("/{id}/projektet")
    public ResponseEntity<List<ProjektiResponse>> getProjektetEKlientit(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(klientiService.getProjektetEKlientit(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<KlientiResponse> updateKlienti(
            @PathVariable Long id,
            @RequestBody UpdateKlientiRequest request
    ) {
        return ResponseEntity.ok(klientiService.updateKlienti(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKlienti(@PathVariable Long id) {
        klientiService.deleteKlienti(id);
        return ResponseEntity.noContent().build();
    }
}

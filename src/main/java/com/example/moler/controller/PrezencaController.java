package com.example.moler.controller;

import com.example.moler.dto.Prezenca.AplikoPrezenceRequest;
import com.example.moler.dto.Prezenca.PrezencaFormDataResponse;
import com.example.moler.model.prezencapuntoreve;
import com.example.moler.service.PrezencaService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/prezenca")
public class PrezencaController {

    private final PrezencaService prezencaService;

    public PrezencaController(PrezencaService prezencaService) {
        this.prezencaService = prezencaService;
    }

    @PostMapping("/apliko")
    public ResponseEntity<?> aplikoPrezence(
            @RequestBody AplikoPrezenceRequest request
    ) {
        List<prezencapuntoreve> prezencat =
                prezencaService.aplikoPrezence(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(prezencat.stream().map(prezenca -> 
                        java.util.Map.of(
                                "id", prezenca.getId(),
                                "idProjekti", prezenca.getProjekti().getId(),
                                "idPuntorit", prezenca.getPuntori().getId(),
                                "dita", prezenca.getDita(),
                                "llojiOrarit", prezenca.getLlojiOrarit(),
                                "statusi", prezenca.getStatusi()
                        )
                ).toList());
    }

    @GetMapping("/form-data/{idPuntorit}")
public ResponseEntity<PrezencaFormDataResponse> getFormData(
        @PathVariable Long idPuntorit
) {
    return ResponseEntity.ok(
            prezencaService.getFormData(idPuntorit)
    );
}
}
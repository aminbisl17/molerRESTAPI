
package com.example.moler.dto.Projekti;

import com.example.moler.dto.Klienti.KlientiResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjektiResponse(
        Long id,
        String emri,
        String lokacioni,
        String kerkesa,
        String statusi,
        String prioriteti,
        String ambienti,
        LocalDate dataFillimitTePlanifikuar,
        LocalDate dataPerfundimitTePlanifikuar,
        LocalDateTime dataKrijimit,
        // null when no client is assigned
        KlientiResponse klienti
) {
}

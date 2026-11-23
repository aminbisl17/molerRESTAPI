package com.example.moler.dto.Projekti;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjektiResponse(
        Long id,
        String emri,
        String lokacioni,
        String statusi,
        String prioriteti,
        String ambienti,
        LocalDate dataFillimitTePlanifikuar,
        LocalDate dataPerfundimitTePlanifikuar,
        LocalDateTime dataKrijimit
) {
}

package com.example.moler.dto.Projekti;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public record CreateProjektiRequest(
        String emri,
        String lokacioni,
        String kerkesa,
        String prioriteti,
        String ambienti,
        LocalDate dataFillimitTePlanifikuar,
        LocalDate dataPerfundimitTePlanifikuar,
        List<Long> menaxheret,
        List<Long> puntoret,
        List<MaterialRequest> materialet
) {
    public record MaterialRequest(
            Long idMaterialit,
            BigDecimal sasia
    ) {
    }
}

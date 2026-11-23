package com.example.moler.dto.Projekti;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        List<MaterialRequest> materialet,
        DetajetRequest detajet
) {

    public record MaterialRequest(
            Long idMaterialit,
            BigDecimal sasia
    ) {}

    public record DetajetRequest(
            BigDecimal m2,
            BigDecimal pagesa,
            String pershkrimi
    ) {}
}

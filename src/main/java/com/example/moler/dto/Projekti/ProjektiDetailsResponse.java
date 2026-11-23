package com.example.moler.dto.Projekti;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProjektiDetailsResponse(

        List<MenaxherResponse> menaxheret,

        List<PuntorResponse> puntoret,

        List<MaterialResponse> materialet,

        DetajetResponse detajet

) {

    public record MenaxherResponse(
            Long id,
            String emri,
            String mbiemri
    ) {}

    public record PuntorResponse(
            Long id,
            String emri,
            String mbiemri,
            String pozita
    ) {}

    public record MaterialResponse(
            Long idMaterialit,
            String emri,
            String kategoria,
            BigDecimal cmimi,
            BigDecimal sasia,
            BigDecimal cmimiNeMomentinEPerdorimit
    ) {}

    public record DetajetResponse(
            BigDecimal m2,
            BigDecimal pagesa,
            String pershkrimi,
            LocalDateTime ditaFillimit,
            LocalDateTime ditaAnulimit,
            LocalDateTime ditaPerfundimit,
            String pershkrimiAnulimit
    ) {}
}
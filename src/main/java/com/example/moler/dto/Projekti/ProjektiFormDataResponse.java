package com.example.moler.dto.Projekti;

import java.math.BigDecimal;
import java.util.List;

public record ProjektiFormDataResponse(

        List<MenaxherResponse> menaxheret,

        List<PuntorResponse> puntoret,

        List<MaterialResponse> materialet

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
            Long id,
            String emri,
            BigDecimal cmimi,
            String kategoria
    ) {}
}


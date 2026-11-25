package com.example.moler.dto.Prezenca;

import java.util.List;

public record PrezencaFormDataResponse(
        List<Long> projektetEFundit,
        List<ProjektiData> projektet,
        boolean kaAplikuarSot
) {
    public record ProjektiData(
            Long id,
            String emri,
            String kerkesa,
            String ambienti
    ) {}
}
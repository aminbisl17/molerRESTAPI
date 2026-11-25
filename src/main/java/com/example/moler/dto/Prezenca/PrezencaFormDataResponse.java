package com.example.moler.dto.Prezenca;

import java.time.LocalDateTime;
import java.util.List;

public record PrezencaFormDataResponse(
        List<Long> projektetEFundit,
        List<ProjektiData> projektet,
        boolean kaAplikuarSot,
        String statusi, // today's attendance status: "nv", "v", "c", or null if not applied
        String arsyejaAnulimit, // only when statusi is "c", otherwise null
        LocalDateTime dataAnulimit // only when statusi is "c", otherwise null
) {
    public record ProjektiData(
            Long id,
            String emri,
            String kerkesa,
            String ambienti
    ) {}
}
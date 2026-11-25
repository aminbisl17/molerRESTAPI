package com.example.moler.dto.Prezenca;

import java.time.LocalDate;
import java.util.List;

/**
 * Unverified ("nv") attendance of one worker for one day,
 * with the projects worked on that day (prezenca_puntoreve_detajet).
 */
public record PrezencaPaVerifikuarResponse(
        Long idPrezences,
        Long idPuntorit,
        String emri,
        String mbiemri,
        String pozita,
        LocalDate dita,
        String llojiOrarit, // "h" or "f"
        List<ProjektiData> projektet
) {
    public record ProjektiData(
            Long idProjektit,
            String emriProjektit,
            String kerkesa,
            String ambienti
    ) {}
}

package com.example.moler.dto.Prezenca;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Attendance of one worker for one day in any status,
 * with the projects worked on that day (prezenca_puntoreve_detajet).
 */
public record PrezencaResponse(
        Long idPrezences,
        Long idPuntorit,
        String emri,
        String mbiemri,
        String pozita,
        LocalDate dita,
        String llojiOrarit,        // "h" or "f"
        String statusi,            // "nv", "v" or "c"
        String arsyejaAnulimit,    // only when "c"
        LocalDateTime dataAnulimit, // only when "c"
        LocalDateTime dataKrijimit,
        List<ProjektiData> projektet
) {
    public record ProjektiData(
            Long idProjektit,
            String emriProjektit,
            String kerkesa,
            String ambienti
    ) {}
}

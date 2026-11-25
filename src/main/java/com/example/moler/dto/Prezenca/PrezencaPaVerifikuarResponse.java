package com.example.moler.dto.Prezenca;

import java.time.LocalDate;
import java.util.List;

/**
 * Unverified ("nv") attendance of one worker for one day.
 * A single application creates one Prezenca_Puntoreve row per project,
 * so each project carries the id of its own prezenca row.
 */
public record PrezencaPaVerifikuarResponse(
        Long idPuntorit,
        String emri,
        String mbiemri,
        String pozita,
        LocalDate dita,
        List<ProjektiData> projektet
) {
    public record ProjektiData(
            Long idPrezences,
            Long idProjektit,
            String emriProjektit,
            String kerkesa,
            String ambienti
    ) {}
}

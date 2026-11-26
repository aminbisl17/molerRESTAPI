package com.example.moler.dto.Prezenca;

import java.util.List;

/**
 * Partial update of an attendance record; null fields are left unchanged.
 */
public record PerditesoPrezenceRequest(
        String statusi,          // "nv", "v" or "c"
        String llojiOrarit,      // "h" or "f"
        List<Long> idProjektet,  // replaces the attendance projects
        String arsyejaAnulimit   // only allowed when the status is "c"
) {}

package com.example.moler.dto.Dashboard;

public record MenaxheriDashboardResponse(
        long puntoretAktiv,           // workers with status "a"
        long pjesmarrjetPaVerifikuara, // "nv" attendance on the manager's projects
        long projektetAktive           // projects with status "e" or "w"
) {}

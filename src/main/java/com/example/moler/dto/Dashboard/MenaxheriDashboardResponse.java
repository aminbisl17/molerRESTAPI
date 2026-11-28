package com.example.moler.dto.Dashboard;

import java.math.BigDecimal;

public record MenaxheriDashboardResponse(
        long puntoretAktiv,           // workers with status "a"
        long pjesmarrjetPaVerifikuara, // "nv" attendance on the manager's projects
        long projektetAktive,          // projects with status "e" or "w"
        BigDecimal pasqyra,            // sum of pagesa from projects with status "cm"
        long nrKlienteve               // total number of clients
) {}

package com.example.moler.dto.Dashboard;

import java.math.BigDecimal;

public record PuntoriDashboardResponse(
        double pjesmarrjetEVerifikuara, // verified days worked this month (a half day "h" counts 0.5)
        BigDecimal rrogaEAkumuluar     // pay earned this month ("h" days count half)
) {}

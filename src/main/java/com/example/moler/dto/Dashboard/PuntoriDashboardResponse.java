package com.example.moler.dto.Dashboard;

import java.math.BigDecimal;

public record PuntoriDashboardResponse(
        long pjesmarrjetEVerifikuara, // verified attendance this month
        BigDecimal rrogaEAkumuluar     // pay earned this month ("h" days count half)
) {}

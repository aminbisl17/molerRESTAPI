package com.example.moler.dto.MenaxheriDTO;

import java.math.BigDecimal;

public record UpdatePuntoriDTO(
        String emri,
        String mbiemri,
        String numriTelefonit,
        String pozita,
        BigDecimal rrogaDitore,
        String statusi
) {}
package com.example.moler.dto.PuntoriDTO;

import java.math.BigDecimal;

public record PuntoriResponseDTO(
        Long id,
        String emri,
        String mbiemri,
        String numriTelefonit,
        String username,
        String pozita,
        BigDecimal rrogaDitore,
        String statusi
) {}
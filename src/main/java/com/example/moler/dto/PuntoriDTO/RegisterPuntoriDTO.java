package com.example.moler.dto.PuntoriDTO;

import java.math.BigDecimal;

public record RegisterPuntoriDTO(
        String emri,
        String mbiemri,
        String numriTelefonit,
        String username,
        String password,
        String pozita,
        BigDecimal rrogaDitore
) {}
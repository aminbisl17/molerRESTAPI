package com.example.moler.dto.Prezenca;

import java.util.List;

public record AplikoPrezenceRequest(
        List<Long> idProjektet,
        String llojiOrarit
) {}
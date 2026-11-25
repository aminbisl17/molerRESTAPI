package com.example.moler.dto.Prezenca;

import java.util.List;

public record VerifikoPrezenceRequest(
        List<Long> idPrezencave
) {}

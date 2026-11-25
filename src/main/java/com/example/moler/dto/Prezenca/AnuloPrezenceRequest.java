package com.example.moler.dto.Prezenca;

import java.util.List;

public record AnuloPrezenceRequest(
        List<Long> idPrezencave,
        String arsyejaAnulimit // optional
) {}

package com.example.moler.dto.Kerkesa;

import java.time.LocalDateTime;

public record KerkesaResponse(
        Long id,
        String titulli,
        String pershkrimi,
        String statusi,                 // w, a, d
        LocalDateTime dataKrijimit,
        LocalDateTime dataAprovimit,    // set by the database trigger
        LocalDateTime dataRefuzimit,    // set by the database trigger
        String pershkrimiAprovimit,
        String pershkrimiAnulimit,
        Krijuesi krijuesi
) {

    // Who created the request
    public record Krijuesi(
            Long id,
            String emri,
            String mbiemri,
            String roli                 // "PUNTOR" or "MENAXHER"
    ) {}
}

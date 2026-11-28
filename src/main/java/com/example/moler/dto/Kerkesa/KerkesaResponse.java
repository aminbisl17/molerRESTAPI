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
        Personi puntori,                // the worker who created the request
        Personi menaxheri               // the manager the request was sent to
) {

    public record Personi(
            Long id,
            String emri,
            String mbiemri
    ) {}
}

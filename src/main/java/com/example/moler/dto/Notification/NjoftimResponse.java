package com.example.moler.dto.Notification;

import java.time.LocalDateTime;
import java.util.Map;

public record NjoftimResponse(
        Long id,
        String tipi,               // e.g. "prezenca_e_re"; null for manual messages
        String titulli,
        String mesazhi,
        Map<String, Object> data,  // same data the push carried, e.g. {"idPrezences": 123}
        boolean eshteLexuar,
        LocalDateTime dataLeximit,
        LocalDateTime dataKrijimit // UTC
) {}

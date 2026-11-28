package com.example.moler.dto.Klienti;

import com.example.moler.model.klienti;

public record KlientiResponse(
        Long id,
        String emri,
        String mbiemri,
        String numriITelefonit,
        String lokacioni
) {

    // Returns null when no client is assigned
    public static KlientiResponse from(klienti k) {
        if (k == null) {
            return null;
        }

        return new KlientiResponse(
                k.getId(),
                k.getEmri(),
                k.getMbiemri(),
                k.getNumriITelefonit(),
                k.getLokacioni()
        );
    }
}

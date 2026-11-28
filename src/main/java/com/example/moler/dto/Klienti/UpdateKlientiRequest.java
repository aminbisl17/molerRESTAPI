package com.example.moler.dto.Klienti;

// Fields left null are not changed.
// numriITelefonit sent as an empty string removes the phone number.
public record UpdateKlientiRequest(
        String emri,
        String mbiemri,
        String numriITelefonit,
        String lokacioni
) {
}

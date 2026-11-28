package com.example.moler.dto.Kerkesa;

// The owner comes from the JWT; status starts as "w" and dates are set by the database
public record KrijoKerkesenRequest(
        String titulli,
        String pershkrimi
) {}

package com.example.moler.dto.Kerkesa;

// Worker edits a waiting request. Fields left null are not changed.
// idMenaxherit sends the request to a different manager.
public record PerditesoKerkesenRequest(
        Long idMenaxherit,
        String titulli,
        String pershkrimi
) {}

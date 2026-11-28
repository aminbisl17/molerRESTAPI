package com.example.moler.dto.Kerkesa;

// Created by a worker; the worker comes from the JWT.
// idMenaxherit = the manager the request is sent to (required).
// Status starts as "w" and dates are set by the database.
public record KrijoKerkesenRequest(
        Long idMenaxherit,
        String titulli,
        String pershkrimi
) {}

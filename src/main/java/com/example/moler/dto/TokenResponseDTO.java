package com.example.moler.dto;

public record TokenResponseDTO(
        String accessToken,
        String refreshToken
) {}
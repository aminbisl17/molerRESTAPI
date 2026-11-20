package com.example.moler.dto.CompanyDTO;

public record RegisterCompanyDTO(
        String tenantKey,
        String username,
        String password
) {}
package com.example.moler.dto.CompanyDTO;

public record RegisterCompanyDTO(
        String tenantKey,
        String company_name,
        String username,
        String password
) {}
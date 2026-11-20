package com.example.moler.dto;

public record TenantInfo(
        Long id,
        String tenantKey,
        String schemaName,
        String name,
        boolean active,
        String domain
) {
}
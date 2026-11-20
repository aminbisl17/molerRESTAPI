package com.example.moler.authentication;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.moler.configuration.TenantContext;
import com.example.moler.dto.TenantInfo;
import com.example.moler.service.TenantService;

@Component
public class TenantFilter extends OncePerRequestFilter {

    private final TenantService tenantService;

    public TenantFilter(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getRequestURI();

        return path.startsWith("/actuator")
                || path.startsWith("/auth/company/enrollment/verify")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        TenantInfo tenant;

        try {

            String tenantKey = request.getHeader("X-Tenant-ID");

            if (tenantKey != null && !tenantKey.isBlank()) {

                tenant = tenantService.findByKey(tenantKey);

            } else {

                String origin = request.getHeader("Origin");

                if (origin == null || origin.isBlank()) {
                    response.sendError(
                            HttpServletResponse.SC_BAD_REQUEST,
                            "Missing tenant information"
                    );
                    return;
                }

                String host;

                try {
                    host = new URI(origin).getHost();
                } catch (URISyntaxException | IllegalArgumentException e) {
                    response.sendError(
                            HttpServletResponse.SC_BAD_REQUEST,
                            "Invalid origin"
                    );
                    return;
                }

                if (host == null || host.isBlank()) {
                    response.sendError(
                            HttpServletResponse.SC_BAD_REQUEST,
                            "Invalid domain"
                    );
                    return;
                }

                host = host.toLowerCase(Locale.ROOT);

                tenant = tenantService.findByDomain(host);
            }

            if (tenant == null || !tenant.active()) {
                response.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "Tenant is inactive or unavailable"
                );
                return;
            }

        } catch (Exception e) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Unknown tenant"
            );
            return;
        }

        try {

            TenantContext.setTenant(tenant.schemaName());
            TenantContext.setTenantId(tenant.id());
            TenantContext.setTenantKey(tenant.tenantKey());

            chain.doFilter(request, response);

        } finally {
            TenantContext.clear();
        }
    }
}
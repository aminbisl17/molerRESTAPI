package com.example.moler.authentication;

import com.example.moler.configuration.TenantContext;

import io.jsonwebtoken.Claims;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private final UserDetailsService menaxheriDetailsService;
    private final UserDetailsService puntoriDetailsService;

    public JwtAuthFilter(
            JwtUtil jwtUtil,
            @Qualifier("menaxheriDetailsService")
            UserDetailsService menaxheriDetailsService,
            @Qualifier("puntoriDetailsService")
            UserDetailsService puntoriDetailsService
    ) {
        this.jwtUtil = jwtUtil;
        this.menaxheriDetailsService = menaxheriDetailsService;
        this.puntoriDetailsService = puntoriDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip WebSocket requests
        if (request.getRequestURI().startsWith("/ws")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            String header = request.getHeader("Authorization");

            if (header != null && header.startsWith("Bearer ")) {

                String token = header.substring(7);

                // Validate token before extracting claims
                if (!jwtUtil.validateToken(token)) {
                    response.sendError(
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Invalid JWT token"
                    );
                    return;
                }

                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);

                Long tokenTenantId = jwtUtil.extractTenantId(token);
                Long currentTenantId = TenantContext.getTenantId();

                // Verify tenant identity
                if (tokenTenantId == null ||
                        currentTenantId == null ||
                        !tokenTenantId.equals(currentTenantId)) {

                    response.sendError(
                            HttpServletResponse.SC_FORBIDDEN,
                            "Tenant mismatch"
                    );
                    return;
                }

                if (username != null &&
                        SecurityContextHolder.getContext().getAuthentication() == null) {

                    UserDetails userDetails;

                    if ("ROLE_MENAXHER".equals(role)) {

                        userDetails =
                                menaxheriDetailsService.loadUserByUsername(username);

                    } else if ("ROLE_PUNTOR".equals(role)) {

                        userDetails =
                                puntoriDetailsService.loadUserByUsername(username);

                    } else {

                        response.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Invalid user role"
                        );
                        return;
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                }
            }

        } catch (Exception e) {

            SecurityContextHolder.clearContext();

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid JWT token"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}


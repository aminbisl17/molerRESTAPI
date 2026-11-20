package com.example.moler.authentication;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final long ACCESS_TOKEN_EXPIRATION =
            1000L * 60 * 60 * 24;

    private static final long REFRESH_TOKEN_EXPIRATION =
            1000L * 60 * 60 * 24 * 30;

    private final SecretKey key;

    public JwtUtil(@Value("${SECRET_KEY}") String secret) {
        this.key = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    // --------------------------------------------------
    // NORMAL ACCESS TOKEN
    // --------------------------------------------------

    public String generateToken(
            Long id,
            String username,
            String role,
            Long tenantId
    ) {
        return Jwts.builder()
                .setSubject(username)
                .claim("id", id)
                .claim("role", role)
                .claim("tenantId", tenantId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(
                        System.currentTimeMillis()
                                + ACCESS_TOKEN_EXPIRATION
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // --------------------------------------------------
    // REFRESH TOKEN
    // --------------------------------------------------

    public String generateRefreshToken(
            Long id,
            String username,
            String role,
            Long tenantId
    ) {
        return Jwts.builder()
                .setSubject(username)
                .claim("id", id)
                .claim("role", role)
                .claim("tenantId", tenantId)
                .claim("type", "REFRESH")
                .setIssuedAt(new Date())
                .setExpiration(new Date(
                        System.currentTimeMillis()
                                + REFRESH_TOKEN_EXPIRATION
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // --------------------------------------------------
    // COMPANY ACCESS TOKEN
    // --------------------------------------------------

    public String generateCompanyToken(
            Long id,
            String username,
            String role
    ) {
        return Jwts.builder()
                .setSubject(username)
                .claim("id", id)
                .claim("role", role)
                .claim("type", "COMPANY_ACCESS")
                .setIssuedAt(new Date())
                .setExpiration(new Date(
                        System.currentTimeMillis()
                                + ACCESS_TOKEN_EXPIRATION
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // --------------------------------------------------
    // TENANT ACCESS TOKEN
    // --------------------------------------------------

    public String generateTenantToken(
            String username,
            String tenantKey
    ) {
        return Jwts.builder()
                .setSubject(username)
                .claim("tenantKey", tenantKey)
                .claim("type", "TENANT_ACCESS")
                .setIssuedAt(new Date())
                .setExpiration(new Date(
                        System.currentTimeMillis()
                                + ACCESS_TOKEN_EXPIRATION
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // --------------------------------------------------
    // EXTRACT CLAIMS
    // --------------------------------------------------

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public Long extractId(String token) {
        Object id = getClaims(token).get("id");
        return id instanceof Number number
                ? number.longValue()
                : null;
    }

    public Long extractTenantId(String token) {
        Object tenantId = getClaims(token).get("tenantId");
        return tenantId instanceof Number number
                ? number.longValue()
                : null;
    }

    public String extractTenantKey(String token) {
        return getClaims(token).get("tenantKey", String.class);
    }

    public String extractType(String token) {
        return getClaims(token).get("type", String.class);
    }

    public String extractCode(String token) {
        return getClaims(token).get("code", String.class);
    }

    // --------------------------------------------------
    // VALIDATE NORMAL ACCESS TOKEN
    // --------------------------------------------------

    public boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);

            String type = claims.get("type", String.class);

            return type == null;

        } catch (Exception e) {
            return false;
        }
    }

    // --------------------------------------------------
    // VALIDATE REFRESH TOKEN
    // --------------------------------------------------

    public boolean validateRefreshToken(String token) {
        try {
            return "REFRESH".equals(
                    getClaims(token).get("type", String.class)
            );

        } catch (Exception e) {
            return false;
        }
    }

    // --------------------------------------------------
    // VALIDATE COMPANY TOKEN
    // --------------------------------------------------

    public boolean validateCompanyToken(String token) {
        try {
            return "COMPANY_ACCESS".equals(
                    getClaims(token).get("type", String.class)
            );

        } catch (Exception e) {
            return false;
        }
    }

    // --------------------------------------------------
    // VALIDATE TENANT TOKEN
    // --------------------------------------------------

    public boolean validateTenantToken(String token) {
        try {
            return "TENANT_ACCESS".equals(
                    getClaims(token).get("type", String.class)
            );

        } catch (Exception e) {
            return false;
        }
    }

    // --------------------------------------------------
    // PARSE JWT
    // --------------------------------------------------

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

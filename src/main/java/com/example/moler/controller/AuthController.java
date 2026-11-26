package com.example.moler.controller;
import com.example.moler.configuration.TenantContext;
import com.example.moler.dto.LoginDTO;
import com.example.moler.dto.TokenResponseDTO;
import com.example.moler.dto.CompanyDTO.CompanyLoginDTO;
import com.example.moler.dto.CompanyDTO.RegisterCompanyDTO;
import com.example.moler.dto.CompanyDTO.VerifyCompanyCodeDTO;
import com.example.moler.model.menaxheri;
import com.example.moler.model.puntori;
import com.example.moler.repository.MenaxheriRepository;
import com.example.moler.repository.PuntoriRepository;

import jakarta.servlet.http.HttpServletResponse;

import com.example.moler.authentication.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final MenaxheriRepository menaxheriRepository;
    private final AuthenticationManager menaxheriAuthManager;
    private final JwtUtil jwtUtil;
   private final SecureRandom secureRandom = new SecureRandom();
    private final JdbcTemplate jdbcTemplate;
    private final PuntoriRepository puntoriRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public AuthController(
        MenaxheriRepository menaxheriRepository,
        PuntoriRepository puntoriRepository,
        @org.springframework.beans.factory.annotation.Qualifier("menaxheriAuthManager")
        AuthenticationManager menaxheriAuthManager,
        JwtUtil jwtUtil,
        JdbcTemplate jdbcTemplate
) {
    this.menaxheriRepository = menaxheriRepository;
    this.puntoriRepository = puntoriRepository;
    this.menaxheriAuthManager = menaxheriAuthManager;
    this.jwtUtil = jwtUtil;
    this.jdbcTemplate = jdbcTemplate;
}

@PostMapping("/refresh")
public ResponseEntity<?> refresh(
        @RequestBody Map<String, String> request
) {

    String refreshToken = request.get("refreshToken");

    if (refreshToken == null || refreshToken.isBlank()
            || !jwtUtil.validateRefreshToken(refreshToken)) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid refresh token"));
    }

    String role = jwtUtil.extractRole(refreshToken);

    if (!"ROLE_MENAXHER".equals(role)
            && !"ROLE_PUNTOR".equals(role)) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Invalid refresh token role"));
    }

    Long tenantId = jwtUtil.extractTenantId(refreshToken);

    if (tenantId == null || !tenantId.equals(TenantContext.getTenantId())) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Tenant mismatch"));
    }

    Long id = jwtUtil.extractId(refreshToken);
    String username = jwtUtil.extractUsername(refreshToken);

    // MANAGER
    if ("ROLE_MENAXHER".equals(role)) {

        menaxheri menaxheri = menaxheriRepository.findById(id)
                .orElse(null);

        if (menaxheri == null
                || !menaxheri.getUsername().equals(username)) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Manager not found"));
        }
    }

    // EMPLOYEE
    if ("ROLE_PUNTOR".equals(role)) {

        puntori puntori = puntoriRepository.findById(id)
                .orElse(null);

        if (puntori == null
                || !puntori.getUsername().equals(username)) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Employee not found"));
        }

        if (puntori.getStatusi() == null
                || !"a".equalsIgnoreCase(puntori.getStatusi().trim())) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Llogaria është joaktive"));
        }
    }

    String newAccessToken = jwtUtil.generateToken(
            id,
            username,
            role,
            tenantId
    );

    String newRefreshToken = jwtUtil.generateRefreshToken(
            id,
            username,
            role,
            tenantId
    );

    return ResponseEntity.ok(
            new TokenResponseDTO(
                    newAccessToken,
                    newRefreshToken
            )
    );
}
    // 1. LOGIN
    @PostMapping("/menaxheri/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO dto) {

        try {
            menaxheriAuthManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            dto.username(),
                            dto.password()
                    )
            );

            menaxheri menaxheri = menaxheriRepository.findByUsername(dto.username())
                    .orElseThrow();

            String accessToken = jwtUtil.generateToken(
                    menaxheri.getId(),
                    menaxheri.getUsername(),
                    "ROLE_MENAXHER",
                    TenantContext.getTenantId()
            );

            String refreshToken = jwtUtil.generateRefreshToken(
                    menaxheri.getId(),
                    menaxheri.getUsername(),
                    "ROLE_MENAXHER",
                    TenantContext.getTenantId()
            );

            return ResponseEntity.ok(
                    new TokenResponseDTO(accessToken, refreshToken)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        }
    }

    // 3. LOGOUT / DELETE REFRESH TOKEN
@PostMapping("/menaxheri/logout")
public ResponseEntity<?> logout(HttpServletResponse response) {

    String[] cookieNames = {
            "refreshToken"
    };

    for (String cookieName : cookieNames) {

        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    return ResponseEntity.noContent().build();
}

    @PostMapping("/company/login")
    public ResponseEntity<?> login(
            @RequestBody CompanyLoginDTO dto
    ) {

        if (dto.username() == null || dto.username().isBlank()
                || dto.password() == null || dto.password().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Username and password are required"));
        }

        List<Map<String, Object>> accounts = jdbcTemplate.queryForList(
                """
                SELECT Id, TenantId, Username, Password
                FROM dbo.companyauth
                WHERE Username = ?
                """,
                dto.username().trim()
        );

        if (accounts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        }

        Map<String, Object> account = accounts.get(0);

        Long id = ((Number) account.get("Id")).longValue();
        String username = (String) account.get("Username");
        String passwordHash = (String) account.get("Password");

        if (!passwordEncoder.matches(dto.password(), passwordHash)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        }

        String companyToken = jwtUtil.generateCompanyToken(
                id,
                username,
                "ROLE_COMPANY"
        );

        return ResponseEntity.ok(
                Map.of("companyToken", companyToken)
        );
    }

    // --------------------------------------------------
    // GENERATE SIX-DIGIT ENROLLMENT CODE
    // POST /auth/company/enrollment/code
    // Requires COMPANY_ACCESS token
    // --------------------------------------------------

    @PostMapping("/company/enrollment/code")
    public ResponseEntity<?> generateEnrollmentCode(
            @RequestHeader("Authorization") String authorization
    ) {

        String token = extractBearerToken(authorization);

        if (token == null || !jwtUtil.validateCompanyToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid company token"));
        }

        Long companyId = jwtUtil.extractId(token);

        // Confirm that the company account still exists.
        Integer accountCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM dbo.companyauth
                WHERE Id = ?
                """,
                Integer.class,
                companyId
        );

        if (accountCount == null || accountCount == 0) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Company account not found"));
        }

        // Generate a six-digit numerical code.
        String code = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        String codeHash = passwordEncoder.encode(code);

        // Invalidate previous unused codes for this company.
        jdbcTemplate.update(
                """
                UPDATE dbo.companyauth_codes
                SET IsUsed = 1
                WHERE CompanyAuthId = ?
                  AND IsUsed = 0
                """,
                companyId
        );

        // Save the new code with a five-minute expiration.
        jdbcTemplate.update(
                """
                INSERT INTO dbo.companyauth_codes
                    (CompanyAuthId, CodeHash, ExpiresAt)
                VALUES
                    (?, ?, DATEADD(MINUTE, 5, SYSUTCDATETIME()))
                """,
                companyId,
                codeHash
        );

        // Development response: return the code directly.
        // In production, deliver it through email or SMS.
        return ResponseEntity.ok(
                Map.of(
                        "message", "Enrollment code generated successfully",
                        "code", code,
                        "expiresInMinutes", 5
                )
        );
    }

    // --------------------------------------------------
    // VERIFY ENROLLMENT CODE
    // POST /auth/company/enrollment/verify
    // --------------------------------------------------

   @PostMapping("/company/enrollment/verify")
public ResponseEntity<?> verifyEnrollmentCode(
        @RequestBody VerifyCompanyCodeDTO dto
) {

    if (dto.username() == null || dto.username().isBlank()
            || dto.code() == null
            || !dto.code().matches("\\d{6}")) {

        return ResponseEntity.badRequest()
                .body(Map.of("message", "Valid username and six-digit code are required"));
    }

    List<Map<String, Object>> codes = jdbcTemplate.queryForList(
            """
            SELECT
                c.Id AS CodeId,
                c.CompanyAuthId,
                c.CodeHash,
                a.Username,
                a.TenantId,
                a.company_name AS CompanyName
            FROM dbo.companyauth_codes c
            INNER JOIN dbo.companyauth a
                ON a.Id = c.CompanyAuthId
            WHERE a.Username = ?
              AND c.IsUsed = 0
              AND c.ExpiresAt > SYSUTCDATETIME()
            ORDER BY c.CreatedAt DESC
            """,
            dto.username().trim()
    );

    if (codes.isEmpty()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid or expired enrollment code"));
    }

    Map<String, Object> matchedCode = null;

    for (Map<String, Object> row : codes) {
        String codeHash = (String) row.get("CodeHash");

        if (passwordEncoder.matches(dto.code(), codeHash)) {
            matchedCode = row;
            break;
        }
    }

    if (matchedCode == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid or expired enrollment code"));
    }

    Long codeId = ((Number) matchedCode.get("CodeId")).longValue();
    Long tenantId = ((Number) matchedCode.get("TenantId")).longValue();

    // Mark code as used only if it is still valid and unused.
    int updated = jdbcTemplate.update(
            """
            UPDATE dbo.companyauth_codes
            SET IsUsed = 1
            WHERE Id = ?
              AND IsUsed = 0
              AND ExpiresAt > SYSUTCDATETIME()
            """,
            codeId
    );

    if (updated != 1) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Code has already been used or expired"));
    }

    // Retrieve tenant key.
    String tenantKey;

    try {
        tenantKey = jdbcTemplate.queryForObject(
                """
                SELECT tenant_key
                FROM dbo.tenants
                WHERE id = ?
                  AND is_active = 1
                """,
                String.class,
                tenantId
        );
    } catch (EmptyResultDataAccessException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Tenant is inactive or does not exist"));
    }

    // HashMap allows a null CompanyName for accounts created before the column existed.
    Map<String, Object> response = new HashMap<>();
    response.put("tenantKey", tenantKey);
    response.put("companyName", matchedCode.get("CompanyName"));

    return ResponseEntity.ok(response);
}
    // --------------------------------------------------
    // TOKEN HELPER
    // --------------------------------------------------

    private String extractBearerToken(String authorization) {

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            return null;
        }

        return authorization.substring(7);
    }

    // --------------------------------------------------
// COMPANY REGISTRATION
// POST /auth/company/register
// --------------------------------------------------

@PostMapping("/company/register")
public ResponseEntity<?> registerCompany(
        @RequestBody RegisterCompanyDTO dto
) {

    if (dto.tenantKey() == null || dto.tenantKey().isBlank()
            || dto.company_name() == null || dto.company_name().isBlank()
            || dto.username() == null || dto.username().isBlank()
            || dto.password() == null || dto.password().isBlank()) {

        return ResponseEntity.badRequest()
                .body(Map.of("message", "All fields are required"));
    }

    if (dto.username().trim().length() < 3) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Username must contain at least 3 characters"));
    }

    if (dto.password().length() < 8) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Password must contain at least 8 characters"));
    }

    // Find the active tenant
    List<Map<String, Object>> tenants = jdbcTemplate.queryForList(
            """
            SELECT id
            FROM dbo.tenants
            WHERE tenant_key = ?
              AND is_active = 1
            """,
            dto.tenantKey().trim()
    );

    if (tenants.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Tenant not found or inactive"));
    }

    Long tenantId = ((Number) tenants.get(0).get("id")).longValue();

    // Check whether this tenant already has a company account
    Integer tenantAccountCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM dbo.companyauth
            WHERE TenantId = ?
            """,
            Integer.class,
            tenantId
    );

    if (tenantAccountCount != null && tenantAccountCount > 0) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "This tenant already has a company account"));
    }

    // Check username uniqueness
    Integer usernameCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM dbo.companyauth
            WHERE Username = ?
            """,
            Integer.class,
            dto.username().trim()
    );

    if (usernameCount != null && usernameCount > 0) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Username already exists"));
    }

    // BCrypt password hashing
    String hashedPassword = passwordEncoder.encode(dto.password());

    jdbcTemplate.update(
            """
            INSERT INTO dbo.companyauth (
                TenantId,
                company_name,
                Username,
                Password
            )
            VALUES (?, ?, ?, ?)
            """,
            tenantId,
            dto.company_name().trim(),
            dto.username().trim(),
            hashedPassword
    );

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of(
                    "message", "Company registered successfully",
                    "companyName", dto.company_name().trim(),
                    "username", dto.username().trim(),
                    "tenantKey", dto.tenantKey().trim()
            ));
}
}
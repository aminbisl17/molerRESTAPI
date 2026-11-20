
package com.example.moler.service;

import java.security.SecureRandom;
import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.moler.dto.TenantInfo;

@Service
public class TenantService {

    private static final Pattern SCHEMA_PATTERN =
            Pattern.compile("^[a-z][a-z0-9_]{0,49}$");

    private static final Set<String> RESERVED_SCHEMAS =
            Set.of("dbo", "sys", "guest", "information_schema");

    private static final Pattern GO_LINE =
            Pattern.compile("(?im)^[ \\t]*GO[ \\t]*\\r?$");

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbcTemplate;

    public TenantService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record RegisteredTenant(
            TenantInfo tenant,
            String dbUser,
            String dbPassword
    ) {}

    // --------------------------------------------------
    // REGISTER TENANT
    // --------------------------------------------------

    public RegisteredTenant registerTenant(
            String name,
            String tenantKey,
            String schemaName,
            String domain) {

        validate(name, tenantKey, schemaName);

        checkNotTaken(tenantKey, schemaName, domain);

        String dbUser = schemaName + "_user";
        String password = generatePassword();

        List<String> batches = new ArrayList<>();

        batches.addAll(loadBatches(
                "db/TenantProvision.sql",
                schemaName,
                password
        ));

        batches.addAll(loadBatches(
                "db/TenantTemplate.sql",
                schemaName,
                password
        ));

        DataSource dataSource = jdbcTemplate.getDataSource();

        if (dataSource == null) {
            throw new IllegalStateException("DataSource is unavailable");
        }

        try (Connection con = dataSource.getConnection()) {

            boolean previousAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);

            try {

                try (Statement statement = con.createStatement()) {
                    for (String batch : batches) {
                        statement.execute(batch);
                    }
                }

                long id = insertTenantRow(
                        con,
                        name,
                        tenantKey,
                        schemaName,
                        domain
                );

                con.commit();

                return new RegisteredTenant(
                      new TenantInfo(
        id,
        tenantKey,
        schemaName,
        name,
        true,
        domain
),
                        dbUser,
                        password
                );

            } catch (SQLException | RuntimeException e) {

                con.rollback();

                throw new IllegalStateException(
                        "Failed to register tenant: " + e.getMessage(),
                        e
                );

            } finally {
                con.setAutoCommit(previousAutoCommit);
            }

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Database error while registering tenant",
                    e
            );
        }
    }

    // --------------------------------------------------
    // VALIDATION
    // --------------------------------------------------

    private void validate(
            String name,
            String tenantKey,
            String schemaName) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Tenant name is required"
            );
        }

        if (tenantKey == null || tenantKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Tenant key is required"
            );
        }

        if (schemaName == null ||
                !SCHEMA_PATTERN.matcher(schemaName).matches()) {

            throw new IllegalArgumentException(
                    "Invalid schema name"
            );
        }

        if (RESERVED_SCHEMAS.contains(schemaName)
                || schemaName.startsWith("db_")) {

            throw new IllegalArgumentException(
                    "Reserved schema name"
            );
        }
    }

    // --------------------------------------------------
    // CHECK EXISTING TENANTS
    // --------------------------------------------------

    private void checkNotTaken(
            String tenantKey,
            String schemaName,
            String domain) {

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM dbo.tenants
                WHERE tenant_key = ?
                   OR schema_name = ?
                   OR (? IS NOT NULL AND domain = ?)
                """,
                Integer.class,
                tenantKey,
                schemaName,
                domain,
                domain
        );

        if (count != null && count > 0) {
            throw new IllegalArgumentException(
                    "Tenant key, schema or domain already exists"
            );
        }

        Integer schemaExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM sys.schemas
                WHERE name = ?
                """,
                Integer.class,
                schemaName
        );

        if (schemaExists != null && schemaExists > 0) {
            throw new IllegalArgumentException(
                    "Schema already exists"
            );
        }

        Integer userExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM sys.database_principals
                WHERE name = ?
                """,
                Integer.class,
                schemaName + "_user"
        );

        if (userExists != null && userExists > 0) {
            throw new IllegalArgumentException(
                    "Database user already exists"
            );
        }
    }

    // --------------------------------------------------
    // INSERT CENTRAL TENANT RECORD
    // --------------------------------------------------

    private long insertTenantRow(
            Connection con,
            String name,
            String tenantKey,
            String schemaName,
            String domain) throws SQLException {

        String sql = """
                INSERT INTO dbo.tenants
                    (schema_name, name, tenant_key, domain)
                OUTPUT INSERTED.id
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, schemaName);
            ps.setString(2, name);
            ps.setString(3, tenantKey);
            ps.setString(4, domain);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    // --------------------------------------------------
    // LOAD SQL SCRIPTS
    // --------------------------------------------------

    private List<String> loadBatches(
            String location,
            String schemaName,
            String password) {

        String script;

        try {
            ClassPathResource resource =
                    new ClassPathResource(location);

            try (var input = resource.getInputStream()) {
                script = new String(
                        input.readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8
                );
            }

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot read SQL file: " + location,
                    e
            );
        }

        script = script.replace("\uFEFF", "")
                .replace("{{SCHEMA}}", schemaName)
                .replace("{{PASSWORD}}", password);

        List<String> batches = new ArrayList<>();

        for (String part : GO_LINE.split(script)) {

            String batch = part.trim();

            if (!batch.isEmpty()) {
                batches.add(batch);
            }
        }

        return batches;
    }

    // --------------------------------------------------
    // GENERATE DATABASE PASSWORD
    // --------------------------------------------------

    private String generatePassword() {

        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghijkmnpqrstuvwxyz";
        String digits = "23456789";
        String symbols = "!#$%*-_=+";

        String all = upper + lower + digits + symbols;

        List<Character> chars = new ArrayList<>();

        chars.add(upper.charAt(RANDOM.nextInt(upper.length())));
        chars.add(lower.charAt(RANDOM.nextInt(lower.length())));
        chars.add(digits.charAt(RANDOM.nextInt(digits.length())));
        chars.add(symbols.charAt(RANDOM.nextInt(symbols.length())));

        while (chars.size() < 24) {
            chars.add(all.charAt(RANDOM.nextInt(all.length())));
        }

        Collections.shuffle(chars, RANDOM);

        StringBuilder password = new StringBuilder();

        chars.forEach(password::append);

        return password.toString();
    }

    // --------------------------------------------------
    // FIND TENANT BY KEY
    // --------------------------------------------------
public TenantInfo findByKey(String tenantKey) {

    String sql = """
            SELECT id, tenant_key, schema_name, name, is_active, domain
            FROM dbo.tenants
            WHERE tenant_key = ?
            """;

    return jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> new TenantInfo(
                    rs.getLong("id"),
                    rs.getString("tenant_key"),
                    rs.getString("schema_name"),
                    rs.getString("name"),
                    rs.getBoolean("is_active"),
                    rs.getString("domain")
            ),
            tenantKey
    );
}
    // --------------------------------------------------
    // FIND TENANT BY DOMAIN
    // --------------------------------------------------

  public TenantInfo findByDomain(String domain) {

    String sql = """
            SELECT id, tenant_key, schema_name, name, is_active, domain
            FROM dbo.tenants
            WHERE domain = ?
            """;

    return jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> new TenantInfo(
                    rs.getLong("id"),
                    rs.getString("tenant_key"),
                    rs.getString("schema_name"),
                    rs.getString("name"),
                    rs.getBoolean("is_active"),
                    rs.getString("domain")
            ),
            domain
    );
}
    // --------------------------------------------------
    // GET SCHEMA NAME
    // --------------------------------------------------

    public String getSchemaName(String tenantKey) {

        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT schema_name
                    FROM dbo.tenants
                    WHERE tenant_key = ?
                    AND is_active = 1
                    """,
                    String.class,
                    tenantKey
            );

        } catch (Exception e) {
            return null;
        }
    }
}

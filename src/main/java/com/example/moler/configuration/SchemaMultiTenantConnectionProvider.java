package com.example.moler.configuration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

@Component
public class SchemaMultiTenantConnectionProvider
        implements MultiTenantConnectionProvider<String> {

    private static final String BOOTSTRAP_TENANT = "dbo";

    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-z][a-z0-9_]{0,127}$");

    private final DataSource dataSource;

    public SchemaMultiTenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection)
            throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenant) throws SQLException {

        Connection connection = dataSource.getConnection();

        if (tenant == null || BOOTSTRAP_TENANT.equals(tenant)) {
            return connection;
        }

        try {
            if (!VALID_SCHEMA.matcher(tenant).matches()) {
                throw new SQLException("Invalid tenant identifier");
            }

            String databaseUser = tenant + "_user";

            try (Statement statement = connection.createStatement()) {
                statement.execute(
                        "EXECUTE AS USER = '" + databaseUser + "'"
                );
            }

            return connection;

        } catch (SQLException | RuntimeException exception) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }

            throw exception;
        }
    }

    @Override
    public void releaseConnection(
            String tenant,
            Connection connection) throws SQLException {

        try {
            if (tenant != null && !BOOTSTRAP_TENANT.equals(tenant)) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("REVERT");
                }
            }
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return unwrapType.isInstance(this);
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {

        if (unwrapType.isInstance(this)) {
            return unwrapType.cast(this);
        }

        throw new IllegalArgumentException(
                "Cannot unwrap to " + unwrapType
        );
    }
}
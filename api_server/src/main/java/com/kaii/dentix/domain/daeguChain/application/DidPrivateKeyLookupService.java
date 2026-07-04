package com.kaii.dentix.domain.daeguChain.application;

import com.kaii.dentix.domain.daeguChain.config.DidDatabaseProperties;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Service
@RequiredArgsConstructor
public class DidPrivateKeyLookupService {

    private final DidDatabaseProperties properties;

    public DidAccountKey findByDid(String did) {
        if (isBlank(did)) {
            throw new BadRequestApiException("user DID is required");
        }
        if (!properties.isConfigured()) {
            throw new BadRequestApiException("DID DB is not configured");
        }

        String tableName = safeIdentifier(properties.getTableName(), "DID");
        String didColumn = safeIdentifier(properties.getDidColumn(), "DID");
        String privateKeyColumn = safeIdentifier(properties.getPrivateKeyColumn(), "private_key");
        String accountAddressColumn = safeIdentifier(properties.getAccountAddressColumn(), "account_address");
        String sql = "select " + quote(privateKeyColumn) + ", " + quote(accountAddressColumn)
                + " from " + quote(tableName)
                + " where " + quote(didColumn) + " = ?";

        try (
                Connection connection = DriverManager.getConnection(
                        properties.getUrl(),
                        properties.getUsername(),
                        properties.getPassword()
                );
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, did);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new BadRequestApiException("DID private key is not found");
                }
                String privateKey = resultSet.getString(privateKeyColumn);
                String accountAddress = resultSet.getString(accountAddressColumn);
                if (isBlank(privateKey) || isBlank(accountAddress)) {
                    throw new BadRequestApiException("DID private key or account address is empty");
                }
                return new DidAccountKey(privateKey, accountAddress);
            }
        } catch (SQLException exception) {
            throw new BadRequestApiException("DID DB lookup failed: " + exception.getMessage());
        }
    }

    private String safeIdentifier(String value, String defaultValue) {
        String identifier = isBlank(value) ? defaultValue : value.trim();
        if (!identifier.matches("[A-Za-z0-9_]+")) {
            throw new BadRequestApiException("Invalid DID DB identifier");
        }
        return identifier;
    }

    private String quote(String identifier) {
        return "`" + identifier + "`";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record DidAccountKey(String privateKey, String accountAddress) {
    }
}

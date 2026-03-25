package savage.commoneconomy.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import savage.commoneconomy.SavsCommonEconomy;
import savage.commoneconomy.config.ConfigManager;
import savage.commoneconomy.model.AccountData;

import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * SQL-based storage implementation using HikariCP.
 * Handles MariaDB/MySQL storage for cross-server synchronization.
 */
public class SqlStorage implements EconomyStorage {

    private HikariDataSource dataSource;
    private final ExecutorService executor;

    public SqlStorage(ExecutorService executor) {
        this.executor = executor;
        initConnection();
        createTable();
    }

    private void initConnection() {
        var config = ConfigManager.getConfig().storage;
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mariadb://" + config.host + ":" + config.port + "/" + config.database);
        hikariConfig.setUsername(config.user);
        hikariConfig.setPassword(config.password);
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setPoolName("SavsEconomyPool");

        this.dataSource = new HikariDataSource(hikariConfig);
    }

    private void createTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS savs_economy_balances (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16) NOT NULL, " +
                    "balance DECIMAL(20,2) NOT NULL, " +
                    "version BIGINT NOT NULL DEFAULT 0" +
                    ")");
        } catch (SQLException e) {
            SavsCommonEconomy.LOGGER.error("Failed to create SQL table", e);
        }
    }

    @Override
    public CompletableFuture<AccountData> loadAccount(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT name, balance, version FROM savs_economy_balances WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        AccountData data = new AccountData(rs.getString("name"), rs.getBigDecimal("balance"));
                        data.setVersion(rs.getLong("version"));
                        return data;
                    }
                }
            } catch (SQLException e) {
                SavsCommonEconomy.LOGGER.error("Failed to load account from SQL: " + uuid, e);
            }
            return null;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> saveAccount(UUID uuid, AccountData data) {
        return CompletableFuture.runAsync(() -> {
            // Use Optimistic Locking: only update if version matches (if implemented in DB)
            // For now, simple UPSERT
            String sql = "INSERT INTO savs_economy_balances (uuid, name, balance, version) VALUES (?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE name = ?, balance = ?, version = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, data.getName());
                stmt.setBigDecimal(3, data.getBalance());
                stmt.setLong(4, data.getVersion());
                
                stmt.setString(5, data.getName());
                stmt.setBigDecimal(6, data.getBalance());
                stmt.setLong(7, data.getVersion());
                
                stmt.executeUpdate();
            } catch (SQLException e) {
                SavsCommonEconomy.LOGGER.error("Failed to save account to SQL: " + uuid, e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Map<UUID, AccountData>> loadAllAccounts() {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, AccountData> accounts = new HashMap<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT uuid, name, balance, version FROM savs_economy_balances")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        AccountData data = new AccountData(rs.getString("name"), rs.getBigDecimal("balance"));
                        data.setVersion(rs.getLong("version"));
                        accounts.put(uuid, data);
                    }
                }
            } catch (SQLException e) {
                SavsCommonEconomy.LOGGER.error("Failed to load all accounts from SQL", e);
            }
            return accounts;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> deleteAccount(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM savs_economy_balances WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                SavsCommonEconomy.LOGGER.error("Failed to delete account from SQL: " + uuid, e);
            }
        }, executor);
    }

    @Override
    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}

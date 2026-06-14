package com.webscraper.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the MySQL connection pool (HikariCP) for XAMPP.
 *
 * Default XAMPP MySQL settings:
 *   host = localhost   port = 3306   user = root   password = (empty)
 *
 * These can be overridden with environment variables:
 *   DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
 */
public class Database {

    private static HikariDataSource ds;

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    public static void init() {
        String host = env("DB_HOST", "localhost");
        String port = env("DB_PORT", "3306");
        String name = env("DB_NAME", "webscraper");
        String user = env("DB_USER", "root");
        String pass = env("DB_PASSWORD", "");

        // First, ensure the database exists (connect without a schema)
        createDatabaseIfMissing(host, port, name, user, pass);

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + name
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(5);
        cfg.setPoolName("webscraper-pool");
        cfg.setConnectionTimeout(8000);

        ds = new HikariDataSource(cfg);

        createTables();
        System.out.println("✅ Connected to MySQL database '" + name + "' at " + host + ":" + port);
    }

    private static void createDatabaseIfMissing(String host, String port, String name,
                                                String user, String pass) {
        String rootUrl = "jdbc:mysql://" + host + ":" + port
                + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        try (Connection c = java.sql.DriverManager.getConnection(rootUrl, user, pass);
             Statement st = c.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + name
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not connect to MySQL. Is XAMPP MySQL running? " + e.getMessage(), e);
        }
    }

    private static void createTables() {
        String users = """
            CREATE TABLE IF NOT EXISTS users (
              id            INT AUTO_INCREMENT PRIMARY KEY,
              name          VARCHAR(100)  NOT NULL,
              email         VARCHAR(190)  NOT NULL UNIQUE,
              password_hash VARCHAR(255)  NOT NULL,
              created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
            )""";

        String history = """
            CREATE TABLE IF NOT EXISTS scrape_history (
              id          INT AUTO_INCREMENT PRIMARY KEY,
              user_id     INT NOT NULL,
              url         VARCHAR(500) NOT NULL,
              title       VARCHAR(500),
              link_count  INT DEFAULT 0,
              image_count INT DEFAULT 0,
              created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
              FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )""";

        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate(users);
            st.executeUpdate(history);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create tables: " + e.getMessage(), e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (ds == null) throw new IllegalStateException("Database not initialized");
        return ds.getConnection();
    }

    public static void close() {
        if (ds != null) ds.close();
    }
}

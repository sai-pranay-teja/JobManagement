package model;

import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class DatabaseUtil {
    private static final String JDBC_URL;
    private static final String JDBC_USER;
    private static final String JDBC_PASSWORD;

    static {
        // Attempt to load from environment variables
        String envUrl = System.getenv("JDBC_URL");
        String envUser = System.getenv("JDBC_USER");
        String envPassword = System.getenv("JDBC_PASSWORD");

        if (envUrl != null && envUser != null && envPassword != null) {
            JDBC_URL = envUrl;
            JDBC_USER = envUser;
            JDBC_PASSWORD = envPassword;
        } else {
            // Fallback to loading from the external configuration file
            Properties props = new Properties();
            try (InputStream input = DatabaseUtil.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (input == null) {
                    throw new RuntimeException("Unable to find config.properties");
                }
                props.load(input);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load database configuration", e);
            }
            JDBC_URL = props.getProperty("jdbc.url");
            JDBC_USER = props.getProperty("jdbc.user");
            JDBC_PASSWORD = props.getProperty("jdbc.password");
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignored */ }
        try { if (stmt != null) stmt.close(); } catch (SQLException e) { /* ignored */ }
        try { if (conn != null) conn.close(); } catch (SQLException e) { /* ignored */ }
    }
}

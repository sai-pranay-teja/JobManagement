import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;

public class TestAppPart1 {

    private static final String JDBC_URL = System.getenv("JDBC_URL");
    private static final String JDBC_USER = System.getenv("JDBC_USER");
    private static final String JDBC_PASS = System.getenv("JDBC_PASSWORD");

    @Test
    public void testUserInsertAndFetch() {
        String testEmail = "saipranay@example.com";
        String testName = "sai2";

        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS)) {
            // Insert user
            String insertSQL = "INSERT INTO users (name, email) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
                stmt.setString(1, testName);
                stmt.setString(2, testEmail);
                stmt.executeUpdate();
            }

            // Fetch user
            String selectSQL = "SELECT name FROM users WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(selectSQL)) {
                stmt.setString(1, testEmail);
                ResultSet rs = stmt.executeQuery();

                assertTrue(rs.next(), "User should exist in DB");
                String fetchedName = rs.getString("name");
                assertEquals(testName, fetchedName, "Fetched name should match inserted name");
            }

        } catch (SQLException e) {
            fail("DB operation failed: " + e.getMessage());
        }
    }
}

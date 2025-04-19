import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import model.DatabaseUtil;

public class TestAppPart1 {

    @Test
    public void testInsertAndFetchUser() {
        String testEmail = "sai12@example.com";
        String testName = "spt";

        Connection conn = null;
        PreparedStatement insertStmt = null;
        PreparedStatement selectStmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();

            // Insert user
            String insertSQL = "INSERT INTO users (name, email) VALUES (?, ?)";
            insertStmt = conn.prepareStatement(insertSQL);
            insertStmt.setString(1, testName);
            insertStmt.setString(2, testEmail);
            insertStmt.executeUpdate();

            // Fetch user
            String selectSQL = "SELECT name FROM users WHERE email = ?";
            selectStmt = conn.prepareStatement(selectSQL);
            selectStmt.setString(1, testEmail);
            rs = selectStmt.executeQuery();

            assertTrue(rs.next(), "User should exist in database");
            String retrievedName = rs.getString("name");
            assertEquals(testName, retrievedName, "Name should match inserted value");

        } catch (SQLException e) {
            fail("Database operation failed: " + e.getMessage());
        } finally {
            DatabaseUtil.close(conn, selectStmt, rs);
            DatabaseUtil.close(null, insertStmt, null);
        }
    }
}

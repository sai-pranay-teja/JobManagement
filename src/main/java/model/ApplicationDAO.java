package model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ApplicationDAO {
    
    // Add new job application
    public void addApplication(Application application) throws SQLException {
        String sql = "INSERT INTO applications (job_id, applicant_id, status, resume_path) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, application.getJobId());
            stmt.setInt(2, application.getApplicantId());
            stmt.setString(3, application.getStatus());
            stmt.setString(4, application.getResumePath());
            stmt.executeUpdate();
        }
    }

    // Get applications for employer
    public List<Application> getApplicationsByEmployer(int employerId) throws SQLException {
        List<Application> applications = new ArrayList<>();
        String sql = "SELECT a.*, j.title AS job_title, u.name AS applicant_name "
                   + "FROM applications a "
                   + "JOIN jobs j ON a.job_id = j.job_id "
                   + "JOIN users u ON a.applicant_id = u.user_id "
                   + "WHERE j.employer_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                applications.add(mapApplication(rs));
            }
        }
        return applications;
    }

    // Get applications for job seeker
    public List<Application> getApplicationsByApplicant(int applicantId) throws SQLException {
        List<Application> applications = new ArrayList<>();
        String sql = "SELECT a.*, j.title AS job_title FROM applications a "
                   + "JOIN jobs j ON a.job_id = j.job_id "
                   + "WHERE a.applicant_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, applicantId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                applications.add(mapApplication(rs));
            }
        }
        return applications;
    }

    // Update application status
    public void updateApplicationStatus(int applicationId, String status) throws SQLException {
        String sql = "UPDATE applications SET status = ? WHERE application_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, applicationId);
            stmt.executeUpdate();
        }
    }

    // Delete job and related applications
    public boolean deleteJob(int jobId, int employerId) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false);

            // Delete applications first
            String deleteAppsSQL = "DELETE FROM applications WHERE job_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteAppsSQL)) {
                stmt.setInt(1, jobId);
                stmt.executeUpdate();
            }

            // Delete job
            String deleteJobSQL = "DELETE FROM jobs WHERE job_id = ? AND employer_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteJobSQL)) {
                stmt.setInt(1, jobId);
                stmt.setInt(2, employerId);
                int affected = stmt.executeUpdate();
                conn.commit();
                return affected > 0;
            }
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.close();
        }
    }

    private Application mapApplication(ResultSet rs) throws SQLException {
        Application app = new Application();
        app.setApplicationId(rs.getInt("application_id"));
        app.setJobId(rs.getInt("job_id"));
        app.setApplicantId(rs.getInt("applicant_id"));
        app.setStatus(rs.getString("status"));
        app.setApplicationDate(rs.getString("application_date"));
        app.setResumePath(rs.getString("resume_path"));
        app.setJobTitle(rs.getString("job_title"));
        
        try {
            app.setApplicantName(rs.getString("applicant_name"));
        } catch (SQLException e) {
            // Column not present in some queries
        }
        
        return app;
    }
    
    
    public void deleteApplication(int applicationId) throws SQLException {
        String sql = "DELETE FROM applications WHERE application_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, applicationId);
            stmt.executeUpdate();
        }
    }
}
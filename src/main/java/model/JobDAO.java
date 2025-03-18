// src/model/JobDAO.java
package model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JobDAO {
    public void addJob(Job job) throws SQLException {
        String sql = "INSERT INTO jobs (title, description, location, salary, employer_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, job.getTitle());
            stmt.setString(2, job.getDescription());
            stmt.setString(3, job.getLocation());
            stmt.setDouble(4, job.getSalary());
            stmt.setInt(5, job.getEmployerId());
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    job.setJobId(rs.getInt(1));
                }
            }
        }
    }

    public List<Job> getJobsByEmployer(int employerId) throws SQLException {
        List<Job> jobs = new ArrayList<>();
        String sql = "SELECT * FROM jobs WHERE employer_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    jobs.add(mapJob(rs));
                }
            }
        }
        return jobs;
    }

    public List<Job> getAllJobs() throws SQLException {
        List<Job> jobs = new ArrayList<>();
        String sql = "SELECT * FROM jobs";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                jobs.add(mapJob(rs));
            }
        }
        return jobs;
    }

    public Job getJobById(int jobId) throws SQLException {
        String sql = "SELECT * FROM jobs WHERE job_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, jobId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapJob(rs);
                }
            }
        }
        return null;
    }

    public void updateJob(Job job) throws SQLException {
        String sql = "UPDATE jobs SET title = ?, description = ?, location = ?, salary = ? WHERE job_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, job.getTitle());
            stmt.setString(2, job.getDescription());
            stmt.setString(3, job.getLocation());
            stmt.setDouble(4, job.getSalary());
            stmt.setInt(5, job.getJobId());
            stmt.executeUpdate();
        }
    }

    public void deleteJob(int jobId) throws SQLException {
        String sql = "DELETE FROM jobs WHERE job_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, jobId);
            stmt.executeUpdate();
        }
    }

    private Job mapJob(ResultSet rs) throws SQLException {
        Job job = new Job();
        job.setJobId(rs.getInt("job_id"));
        job.setTitle(rs.getString("title"));
        job.setDescription(rs.getString("description"));
        job.setLocation(rs.getString("location"));
        job.setSalary(rs.getDouble("salary"));
        job.setEmployerId(rs.getInt("employer_id"));
        return job;
    }
}
package model;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@WebServlet("/viewJobs")
public class ViewJobsController extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        try {
            JobDAO jobDAO = new JobDAO();
            List<Job> jobs;

            // If the user is an employer, they see only their jobs.
            if ("EMPLOYER".equalsIgnoreCase(user.getRole())) {
                jobs = jobDAO.getJobsByEmployer(user.getUserId());
            } else { // For job seekers, filter out jobs they've applied to.
                List<Job> allJobs = jobDAO.getAllJobs();
                List<Application> applications = new ApplicationDAO().getApplicationsByApplicant(user.getUserId());
                Set<Integer> appliedJobIds = new HashSet<>();
                for (Application app : applications) {
                    appliedJobIds.add(app.getJobId());
                }
                jobs = new ArrayList<>();
                for (Job job : allJobs) {
                    if (!appliedJobIds.contains(job.getJobId())) {
                        jobs.add(job);
                    }
                }
            }

            request.setAttribute("jobs", jobs);
            request.getRequestDispatcher("/viewJobs.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Database error", e);
        }
    }
}

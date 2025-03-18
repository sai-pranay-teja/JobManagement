// src/model/JobController.java
package model;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/postJob")
public class JobController extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        
        if (user == null || !user.getRole().equals("EMPLOYER")) {
            response.sendRedirect("login.jsp");
            return;
        }

        Job job = new Job();
        job.setTitle(request.getParameter("title"));
        job.setDescription(request.getParameter("description"));
        job.setLocation(request.getParameter("location"));
        job.setSalary(Double.parseDouble(request.getParameter("salary")));
        job.setEmployerId(user.getUserId());

        try {
            new JobDAO().addJob(job);
            response.sendRedirect(request.getContextPath() + "/viewJobs?action=myJobs");
        } catch (SQLException e) {
            throw new ServletException("Error posting job", e);
        }
    }
}
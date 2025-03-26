package model;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/viewApplications")
public class ViewApplicationsController extends HttpServlet {
    /**
	 * 
	 */
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
            ApplicationDAO appDAO = new ApplicationDAO();
            List<Application> applications;

            if ("JOB_SEEKER".equals(user.getRole())) {
                applications = appDAO.getApplicationsByApplicant(user.getUserId());
            } else {
                applications = appDAO.getApplicationsByEmployer(user.getUserId());
            }

            request.setAttribute("applications", applications);
            request.getRequestDispatcher("viewApplications.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Database error", e);
        }
    }
}
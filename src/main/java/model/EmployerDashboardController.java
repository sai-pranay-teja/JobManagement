package model;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@WebServlet("/employerDashboard")
public class EmployerDashboardController extends HttpServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        
        if (user == null || !user.getRole().equals("EMPLOYER")) {
            response.sendRedirect("login.jsp");
            return;
        }

        try {
            List<Application> pendingApplications = new ApplicationDAO()
                .getApplicationsByEmployer(user.getUserId())
                .stream()
                .filter(app -> "PENDING".equals(app.getStatus()))
                .collect(Collectors.toList());
            
            request.setAttribute("pendingApplications", pendingApplications);
            request.getRequestDispatcher("/employerDashboard.jsp").forward(request, response);
            
        } catch (SQLException e) {
            throw new ServletException("Database error", e);
        }
    }
}
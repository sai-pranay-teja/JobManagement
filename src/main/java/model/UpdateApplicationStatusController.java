package model;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;

@WebServlet("/updateApplicationStatus")
public class UpdateApplicationStatusController extends HttpServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        
        if (user == null || !user.getRole().equals("EMPLOYER")) {
            response.sendRedirect("login.jsp");
            return;
        }

        try {
            int appId = Integer.parseInt(request.getParameter("appId"));
            String status = request.getParameter("status");
            
            new ApplicationDAO_1().updateApplicationStatus(appId, status);
            
            response.sendRedirect("employerDashboard");
            
        } catch (Exception e) {
            throw new ServletException("Status update failed", e);
        }
    }
}
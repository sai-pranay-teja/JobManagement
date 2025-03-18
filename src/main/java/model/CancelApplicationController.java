package model;


import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;


@WebServlet("/cancelApplication")
public class CancelApplicationController extends HttpServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null || !"JOB_SEEKER".equals(user.getRole())) {
            response.sendRedirect("login.jsp");
            return;
        }

        try {
            int appId = Integer.parseInt(request.getParameter("appId"));
            new ApplicationDAO().deleteApplication(appId);
            response.sendRedirect("viewApplications");
        } catch (Exception e) {
            throw new ServletException("Cancel failed", e);
        }
    }
}
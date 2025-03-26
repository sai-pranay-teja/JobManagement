package model;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;

@WebServlet("/deleteJob")
public class DeleteJobController extends HttpServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        
        // 1. Validate employer role
        if (user == null || !"EMPLOYER".equals(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        try {
            // 2. Get job ID parameter
            int jobId = Integer.parseInt(request.getParameter("jobId"));
            
            // 3. Delete job from database
            ApplicationDAO dao = new ApplicationDAO();
            boolean success = dao.deleteJob(jobId, user.getUserId());
            
            // 4. Redirect to employer's job list
            if (success) {
                response.sendRedirect(request.getContextPath() + "/viewJobs?action=myJobs");
            } else {
                response.sendRedirect(request.getContextPath() + "/viewJobs?action=myJobs&error=delete_failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/viewJobs?action=myJobs&error=server_error");
        }
    }
}
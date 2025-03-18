package model;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.nio.file.Paths;

@WebServlet("/applyJob")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 1, // 1 MB
    maxFileSize = 1024 * 1024 * 10,      // 10 MB
    maxRequestSize = 1024 * 1024 * 100   // 100 MB
)
public class ApplicationController extends HttpServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String UPLOAD_DIR = "resumes";

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        
        if (user == null || !user.getRole().equals("JOB_SEEKER")) {
            response.sendRedirect("login.jsp");
            return;
        }

        try {
            int jobId = Integer.parseInt(request.getParameter("jobId"));
            
            String appPath = request.getServletContext().getRealPath("");
            String savePath = appPath + File.separator + UPLOAD_DIR;
            
            File fileSaveDir = new File(savePath);
            if (!fileSaveDir.exists()) fileSaveDir.mkdir();
            
            Part filePart = request.getPart("resume");
            String fileName = System.currentTimeMillis() + "_" + user.getUserId() + "_" 
                            + Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            
            filePart.write(savePath + File.separator + fileName);
            
            Application application = new Application();
            application.setJobId(jobId);
            application.setApplicantId(user.getUserId());
            application.setStatus("PENDING");
            application.setResumePath(UPLOAD_DIR + "/" + fileName);
            
            new ApplicationDAO_1().addApplication(application);
            
            response.sendRedirect("viewApplications");
            
        } catch (Exception e) {
            throw new ServletException("Application failed", e);
        }
    }
}
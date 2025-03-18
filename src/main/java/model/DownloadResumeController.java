package model;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;

@WebServlet("/downloadResume")
public class DownloadResumeController extends HttpServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String resumePath = request.getParameter("path");
        String fullPath = getServletContext().getRealPath(resumePath);
        File file = new File(fullPath);

        if (file.exists()) {
            response.setContentType(getServletContext().getMimeType(fullPath));
            response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            try (InputStream in = new FileInputStream(file);
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, java.util.ArrayList" %>
<%@ page import="model.Application" %>
<%@ page import="model.User" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Applications</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/styles.css">
</head>
<body>
    <% 
        // Check if a user is logged in; if not, redirect to the login page.
        Object userObj = session.getAttribute("user");
        if (userObj == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }
        User user = (User) userObj;
        
        // Retrieve the applications attribute safely.
        Object appsObj = request.getAttribute("applications");
        List<Application> applications = new ArrayList<>();
        if (appsObj instanceof List<?>) {
            for (Object o : (List<?>) appsObj) {
                if (o instanceof Application) {
                    applications.add((Application) o);
                }
            }
        }
    %>

    <h1>Applications</h1>
    <p>Below is the list of your job applications. Please review the details carefully.</p>
    
    <% 
        // Display success or error messages if available.
        Object message = request.getAttribute("message");
        if (message != null) { 
    %>
        <div class="alert"><%= message %></div>
    <% } %>
    
    <table border="1">
        <thead>
            <tr>
                <th>Job Title</th>
                <th>Date</th>
                <th>Status</th>
                <% if ("EMPLOYER".equals(user.getRole())) { %>
                    <th>Applicant</th>
                <% } %>
                <th>Resume</th>
            </tr>
        </thead>
        <tbody>
            <% if (!applications.isEmpty()) { 
                   for (Application app : applications) { %>
                <tr>
                    <td><%= app.getJobTitle() %></td>
                    <td><%= app.getApplicationDate() %></td>
                    <td><%= app.getStatus() %></td>
                    <% if ("EMPLOYER".equals(user.getRole())) { %>
                        <td><%= app.getApplicantName() %></td>
                    <% } %>
					<td>
    					<a href="downloadResume?path=<%= java.net.URLEncoder.encode(app.getResumePath(), "UTF-8") %>">
        					Download
    					</a>
					</td>
                    <td>
    					<form action="<%= request.getContextPath() %>/cancelApplication" method="post"
          onsubmit="return confirm('Are you sure you want to cancel this application?')">
        					<input type="hidden" name="appId" value="<%= app.getApplicationId() %>">
        					<button type="submit" class="cancel-btn">Cancel</button>
    					</form>
				   </td>
                </tr>
            <%   } 
               } else { %>
                <tr>
                    <td colspan="5">No applications found.</td>
                </tr>
            <% } %>
        </tbody>
    </table>
    <br>
    <a href="<%= request.getContextPath() %>/viewJobs">Back to Jobs</a>
</body>
</html>

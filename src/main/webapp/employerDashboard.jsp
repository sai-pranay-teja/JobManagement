<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List, java.util.ArrayList" %>
<%@ page import="model.Application" %>
<!DOCTYPE html>
<html>
<head>
    <title>Employer Dashboard</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
    <%-- Other code (user check etc.) --%>
    <%
        Object pendingObj = request.getAttribute("pendingApplications");
        List<Application> pendingApplications = new ArrayList<>();
        if (pendingObj instanceof List<?>) {
            for (Object item : (List<?>) pendingObj) {
                if (item instanceof Application) {
                    pendingApplications.add((Application) item);
                }
            }
        }
    %>
    
    <h2>Pending Applications (<%= pendingApplications.size() %>)</h2>
    
    <table border="1">
        <tr>
            <th>Job Title</th>
            <th>Applicant Name</th>
            <th>Application Date</th>
            <th>Resume</th>
            <th>Action</th>
        </tr>
        <% for (Application app : pendingApplications) { %>
        <tr>
            <td><%= app.getJobTitle() %></td>
            <td><%= app.getApplicantName() %></td>
            <td><%= app.getApplicationDate() %></td>
            <td>
                <a href="downloadResume?path=<%= java.net.URLEncoder.encode(app.getResumePath(), "UTF-8") %>">
                   Download
                </a>
           </td>
            <td>
                <form action="updateApplicationStatus" method="post">
                    <input type="hidden" name="appId" value="<%= app.getApplicationId() %>">
                    <button type="submit" name="status" value="ACCEPTED" class="accept-btn">Accept</button>
                    <button type="submit" name="status" value="REJECTED" class="reject-btn">Reject</button>
                </form>
            </td>
        </tr>
        <% } %>
    </table>
    <div class="navigation">
        <a href="<%= request.getContextPath() %>/postJob.jsp">Post New Job</a> |
        <a href="<%= request.getContextPath() %>/viewApplications">Job Status</a> |
        <a href="<%= request.getContextPath() %>/viewJobs?action=myJobs">View My Jobs</a> |
    <%--    <a href="<%= request.getContextPath() %>/employerDashboard">Back to Dashboard</a> | --%>
        <a href="<%= request.getContextPath() %>/logout">Logout</a>
    </div>
</body>
</html>

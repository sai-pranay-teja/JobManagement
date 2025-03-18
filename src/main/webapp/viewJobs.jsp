<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, java.util.ArrayList" %>
<%@ page import="model.Job" %>
<%@ page import="model.User" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Job Listings</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/styles.css">
</head>
<body>
    <div class="container">
        <%-- Authentication Check: If user is not logged in, redirect to login page --%>
        <%
            Object userObj = session.getAttribute("user");
            if (userObj == null) {
                response.sendRedirect(request.getContextPath() + "/login.jsp");
                return;
            }
            User user = (User) userObj;
        %>

        <%-- Safely retrieve job listings from request attribute --%>
        <%
            Object jobsObj = request.getAttribute("jobs");
            List<Job> jobs = new ArrayList<>();
            if (jobsObj instanceof List<?>) {
                for (Object obj : (List<?>) jobsObj) {
                    if (obj instanceof Job) {
                        jobs.add((Job) obj);
                    }
                }
            }
        %>

        <%-- Header Section --%>
        <header class="header">
            <h1>Job Listings</h1>
            <div class="user-info">
                Welcome, <%= user.getName() %>!
                <a href="<%= request.getContextPath() %>/logout" class="logout-btn">Logout</a>
            </div>
        </header>

        <%-- Display Messages --%>
        <%
            Object messageObj = request.getAttribute("message");
            if (messageObj != null) {
        %>
            <div class="alert alert-success"><%= messageObj %></div>
        <%
            }
            Object errorObj = request.getAttribute("error");
            if (errorObj != null) {
        %>
            <div class="alert alert-error"><%= errorObj %></div>
        <%
            }
        %>

        <%-- Jobs Table --%>
        <table>
            <thead>
                <tr>
                    <th>Title</th>
                    <th>Location</th>
                    <th>Salary</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <%
                    if (jobs != null && !jobs.isEmpty()) {
                        for (Job job : jobs) {
                %>
                <tr>
                    <td><%= job.getTitle() %></td>
                    <td><%= job.getLocation() %></td>
                    <td>$<%= job.getSalary() %></td>
                    <td>
                        <div class="action-buttons">
                        <%
                            if ("JOB_SEEKER".equals(user.getRole())) {
                        %>
                            <form action="<%= request.getContextPath() %>/applyJob" method="post" enctype="multipart/form-data" class="apply-form">
                                <input type="hidden" name="jobId" value="<%= job.getJobId() %>">
                                <input type="file" name="resume" accept=".pdf,.doc,.docx" required onchange="validateResume(this)">
                                <button type="submit" class="apply-btn">Apply Now</button>
                            </form>
                        <%
                            } else if ("EMPLOYER".equals(user.getRole()) && "myJobs".equals(request.getParameter("action"))) {
                        %>
                            <a href="<%= request.getContextPath() %>/deleteJob?jobId=<%= job.getJobId() %>" class="delete-btn" onclick="return confirm('Are you sure you want to delete this job posting?')">Delete</a>
                        <%
                            }
                        %>
                        </div>
                    </td>
                </tr>
                <%
                        }
                    } else {
                %>
                <tr>
                    <td colspan="4" class="no-jobs">No jobs found matching your criteria</td>
                </tr>
                <%
                    }
                %>
            </tbody>
        </table>

        <%-- Navigation Links --%>
        <div class="navigation">
            <%
                if ("EMPLOYER".equals(user.getRole())) {
            %>
                <a href="<%= request.getContextPath() %>/postJob.jsp" class="btn">Post New Job</a>
                <a href="<%= request.getContextPath() %>/viewJobs?action=myJobs" class="btn">View My Jobs</a>
            <%
                }
            %>
            <a href="<%= request.getContextPath() %>/dashboard.jsp" class="btn">Back to Dashboard</a>
        </div>
    </div>

    <script>
        function validateResume(input) {
            const allowedExtensions = ['pdf', 'doc', 'docx'];
            const fileName = input.value.split('.').pop().toLowerCase();
            if (!allowedExtensions.includes(fileName)) {
                alert('Only PDF, DOC, and DOCX files are allowed!');
                input.value = '';
            }
        }
    </script>
</body>
</html>

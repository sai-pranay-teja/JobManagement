<%-- webapp/views/register.jsp --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Register</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
    <form action="${pageContext.request.contextPath}/register" method="post">
        <input type="text" name="name" placeholder="Name" required>
        <input type="email" name="contact" placeholder="Email" required>
        <select name="role" required>
            <option value="EMPLOYER">Employer</option>
            <option value="JOB_SEEKER">Job Seeker</option>
        </select>
        <input type="password" name="password" placeholder="Password" required> <!-- Add this -->
        <input type="text" name="skills" placeholder="Skills">
        <button type="submit">Register</button>
    </form>
    
    <p>Form action: ${pageContext.request.contextPath}/register</p>
    
</body>
</html>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>Dashboard</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
    <c:choose>
        <c:when test="${empty sessionScope.user}">
            <c:redirect url="login.jsp"/>
        </c:when>
        <c:when test="${sessionScope.user.role == 'EMPLOYER'}">
            <h1>Welcome Employer: ${sessionScope.user.name}</h1>
            <a href="postJob.jsp">Post New Job</a><br>
            <a href="viewJobs?action=myJobs">View My Jobs</a><br>
        </c:when>
        <c:otherwise>
            <h1>Welcome Job Seeker: ${sessionScope.user.name}</h1>
            <a href="viewJobs">Browse Jobs</a><br>
            <a href="viewApplications">My Applications</a><br>
        </c:otherwise>
    </c:choose>
    <a href="logout">Logout</a>
</body>
</html>
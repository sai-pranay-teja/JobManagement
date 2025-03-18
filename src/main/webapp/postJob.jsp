<%-- webapp/views/postJob.jsp --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>Post Job</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
    <c:if test="${empty sessionScope.user || sessionScope.user.role ne 'EMPLOYER'}">
        <c:redirect url="login.jsp"/>
    </c:if>
    
    <h1>Post New Job</h1>
    <form action="postJob" method="post">
        <input type="text" name="title" placeholder="Job Title" required>
        <textarea name="description" placeholder="Job Description" required></textarea>
        <input type="text" name="location" placeholder="Location" required>
        <input type="number" name="salary" step="0.01" placeholder="Salary" required>
        <button type="submit">Post Job</button>
    </form>
</body>
</html>
<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<html>
<head>
    <title>Error</title>
    <link rel="stylesheet" href="../css/styles.css">
</head>
<body>
    <h1>Error Occurred</h1>
    <p>${requestScope.error}</p>
    <a href="login.jsp">Go Back to Login</a>
</body>
</html>
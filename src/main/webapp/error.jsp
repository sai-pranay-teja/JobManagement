<%@ page isErrorPage="true" contentType="text/html;charset=UTF-8" %>
<html>
<head><title>Error</title></head>
<body>
  <h1>An error occurred</h1>
  <h3><%= exception.getMessage() %></h3>
  <pre>
<%
    exception.printStackTrace(new java.io.PrintWriter(out));
%>
  </pre>
  <a href="login.jsp">Back to Login</a>
</body>
</html>

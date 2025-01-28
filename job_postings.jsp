<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Job Postings</title>
</head>
<body>
    <h1>Job Postings</h1>
    <!-- HTML and JSP code for displaying job postings -->
    <form action="JobPostings" method="post">
        <input type="text" name="jobTitle" placeholder="Job Title" required>
        <input type="text" name="jobDescription" placeholder="Job Description" required>
        <button type="submit">Add Job Posting</button>
    </form>
</body>
</html>

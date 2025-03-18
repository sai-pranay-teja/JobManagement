// src/UserController.java
package model;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException; // Add this import
import java.sql.SQLException; // Add this import

@WebServlet("/register")
public class UserController extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        System.out.println("Register servlet called"); // Debug message
        User user = new User();
        user.setName(request.getParameter("name"));
        user.setContactInfo(request.getParameter("contact"));
        user.setPassword(request.getParameter("password")); // Add this line
        user.setRole(request.getParameter("role"));
        user.setSkills(request.getParameter("skills"));

        try {
            new UserDAO().addUser(user);
            System.out.println("User added successfully"); // Debug message

            response.sendRedirect(request.getContextPath() + "/login.jsp");
        } catch (SQLException e) {
            System.err.println("Error adding user: " + e.getMessage()); // Debug message
            throw new ServletException(e);
        }
    }
}
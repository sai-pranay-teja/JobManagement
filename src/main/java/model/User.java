package model;

public class User {
    private int userId;
    private String name;
    private String contactInfo;
    private String password;
    private String role;
    private String skills;

    // Getters
    public int getUserId() { return userId; }
    public String getName() { return name; }
    public String getContactInfo() { return contactInfo; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getSkills() { return skills; }

    // Setters
    public void setUserId(int userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(String role) { this.role = role; }
    public void setSkills(String skills) { this.skills = skills; }
}
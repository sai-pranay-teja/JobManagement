package model;

public class Application {
    private int applicationId;
    private int jobId;
    private int applicantId;
    private String status;
    private String applicationDate;
    private String resumePath;
    private String jobTitle;
    private String applicantName;

    // Default constructor
    public Application() {}

    // Parameterized constructor
    public Application(int jobId, int applicantId, String status) {
        this.jobId = jobId
        this.applicantId = applicantId;
        this.status = status;
    }

    // Getters and setters
    
    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }
    
    public int getJobId() { return jobId; }
    public void setJobId(int jobId) { this.jobId = jobId; }
    
    public int getApplicantId() { return applicantId; }
    public void setApplicantId(int applicantId) { this.applicantId = applicantId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getApplicationDate() { return applicationDate; }
    public void setApplicationDate(String applicationDate) { this.applicationDate = applicationDate; }
    
    public String getResumePath() { return resumePath; }
    public void setResumePath(String resumePath) { this.resumePath = resumePath; }
    
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
}

// package metrics;

// public class MetricRecord {
//     private String toolName;
//     private int totalPipelineTime;
//     private int deploymentTime;
//     private int leadTime;
//     private int rollbackTime;       // -1 if N/A
//     private long memoryBeforeUsed;
//     private long memoryAfterUsed;

//     public MetricRecord(String toolName) {
//         this.toolName = toolName;
//     }

//     public String getToolName() {
//         return toolName;
//     }

//     public void setToolName(String toolName) {
//         this.toolName = toolName;
//     }

//     public int getTotalPipelineTime() {
//         return totalPipelineTime;
//     }

//     public void setTotalPipelineTime(int totalPipelineTime) {
//         this.totalPipelineTime = totalPipelineTime;
//     }

//     public int getDeploymentTime() {
//         return deploymentTime;
//     }

//     public void setDeploymentTime(int deploymentTime) {
//         this.deploymentTime = deploymentTime;
//     }

//     public int getLeadTime() {
//         return leadTime;
//     }

//     public void setLeadTime(int leadTime) {
//         this.leadTime = leadTime;
//     }

//     public int getRollbackTime() {
//         return rollbackTime;
//     }

//     public void setRollbackTime(int rollbackTime) {
//         this.rollbackTime = rollbackTime;
//     }

//     public long getMemoryBeforeUsed() {
//         return memoryBeforeUsed;
//     }

//     public void setMemoryBeforeUsed(long memoryBeforeUsed) {
//         this.memoryBeforeUsed = memoryBeforeUsed;
//     }

//     public long getMemoryAfterUsed() {
//         return memoryAfterUsed;
//     }

//     public void setMemoryAfterUsed(long memoryAfterUsed) {
//         this.memoryAfterUsed = memoryAfterUsed;
//     }
// }


package metrics;

public class MetricRecord {
    private String toolName;
    private double totalPipelineTime; // Changed to double
    private double deploymentTime;
    private double leadTime;
    private int rollbackTime;       // -1 if N/A
    private double memoryBeforeUsed; // Changed to double
    private double memoryAfterUsed;
    private double duration; // Added
    private double cost;     // Added

    public MetricRecord(String toolName) {
        this.toolName = toolName;
    }

    // Getters/Setters for new fields
    public double getDuration() { return duration; }
    public void setDuration(double duration) { this.duration = duration; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    // Updated getters/setters for time fields
    public double getTotalPipelineTime() { return totalPipelineTime; }
    public void setTotalPipelineTime(double totalPipelineTime) { 
        this.totalPipelineTime = totalPipelineTime; 
    }

    public double getDeploymentTime() { return deploymentTime; }
    public void setDeploymentTime(double deploymentTime) { 
        this.deploymentTime = deploymentTime; 
    }

    public double getLeadTime() { return leadTime; }
    public void setLeadTime(double leadTime) { this.leadTime = leadTime; }

    // Memory fields
    public double getMemoryBeforeUsed() { return memoryBeforeUsed; }
    public void setMemoryBeforeUsed(double memoryBeforeUsed) { 
        this.memoryBeforeUsed = memoryBeforeUsed; 
    }

    public double getMemoryAfterUsed() { return memoryAfterUsed; }
    public void setMemoryAfterUsed(double memoryAfterUsed) { 
        this.memoryAfterUsed = memoryAfterUsed; 
    }

    // Existing methods for rollbackTime and toolName
    public int getRollbackTime() { return rollbackTime; }
    public void setRollbackTime(int rollbackTime) { 
        this.rollbackTime = rollbackTime; 
    }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { 
        this.toolName = toolName; 
    }
}
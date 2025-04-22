package metrics;

public class MetricRecord {
    private String toolName;
    private boolean isRollback;
    private double totalPipelineTime;
    private double rollbackTime = -1; // -1 means N/A
    private double memoryBeforeUsed;
    private double memoryAfterUsed;
    private double duration;
    private double cost;

    public MetricRecord(String toolName) {
        this.toolName = toolName;
        this.isRollback = toolName.contains("-rollback");
    }

    // Getters/Setters
    public String getToolName() { return toolName; }
    public boolean isRollback() { return isRollback; }
    public double getTotalPipelineTime() { return totalPipelineTime; }
    public void setTotalPipelineTime(double t) { this.totalPipelineTime = t; }
    public double getRollbackTime() { return rollbackTime; }
    public void setRollbackTime(double t) { this.rollbackTime = t; }
    public double getMemoryBeforeUsed() { return memoryBeforeUsed; }
    public void setMemoryBeforeUsed(double m) { this.memoryBeforeUsed = m; }
    public double getMemoryAfterUsed() { return memoryAfterUsed; }
    public void setMemoryAfterUsed(double m) { this.memoryAfterUsed = m; }
    public double getDuration() { return duration; }
    public void setDuration(double d) { this.duration = d; }
    public double getCost() { return cost; }
    public void setCost(double c) { this.cost = c; }
}
package metrics;

public class MetricRecord {
    private String toolName;
    private boolean isRollback;
    private double totalPipelineTime;
    private double rollbackTime;
    private double memoryBeforeUsed;
    private double memoryAfterUsed;

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
}
package metrics;

public class MetricRecord {
    private String toolName;
    private int totalPipelineTime;
    private int deploymentTime;
    private int leadTime;
    private int rollbackTime;       // -1 if N/A
    private long memoryBeforeUsed;
    private long memoryAfterUsed;

    public MetricRecord(String toolName) {
        this.toolName = toolName;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public int getTotalPipelineTime() {
        return totalPipelineTime;
    }

    public void setTotalPipelineTime(int totalPipelineTime) {
        this.totalPipelineTime = totalPipelineTime;
    }

    public int getDeploymentTime() {
        return deploymentTime;
    }

    public void setDeploymentTime(int deploymentTime) {
        this.deploymentTime = deploymentTime;
    }

    public int getLeadTime() {
        return leadTime;
    }

    public void setLeadTime(int leadTime) {
        this.leadTime = leadTime;
    }

    public int getRollbackTime() {
        return rollbackTime;
    }

    public void setRollbackTime(int rollbackTime) {
        this.rollbackTime = rollbackTime;
    }

    public long getMemoryBeforeUsed() {
        return memoryBeforeUsed;
    }

    public void setMemoryBeforeUsed(long memoryBeforeUsed) {
        this.memoryBeforeUsed = memoryBeforeUsed;
    }

    public long getMemoryAfterUsed() {
        return memoryAfterUsed;
    }

    public void setMemoryAfterUsed(long memoryAfterUsed) {
        this.memoryAfterUsed = memoryAfterUsed;
    }
}

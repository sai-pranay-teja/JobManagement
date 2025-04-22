package metrics;

public class MetricRecord {
    private String toolName;
    private boolean isRollback;
    private double totalPipelineTime;
    private double deploymentTime;     // ← new
    private double leadTime;           // ← new
    private double rollbackTime;
    private double memoryBeforeUsed;
    private double memoryAfterUsed;
    private double cost;

    public MetricRecord(String toolName) {
        this.toolName = toolName;
        this.isRollback = toolName.contains("-rollback");
    }

    // existing getters...
    public String getToolName()            { return toolName; }
    public boolean isRollback()           { return isRollback; }
    public double getTotalPipelineTime()  { return totalPipelineTime; }
    public double getRollbackTime()       { return rollbackTime; }
    public double getMemoryBeforeUsed()   { return memoryBeforeUsed; }
    public double getMemoryAfterUsed()    { return memoryAfterUsed; }

    // existing setters...
    public void setTotalPipelineTime(double t) { this.totalPipelineTime = t; }
    public void setRollbackTime(double t)      { this.rollbackTime = t; }
    public void setMemoryBeforeUsed(double m)  { this.memoryBeforeUsed = m; }
    public void setMemoryAfterUsed(double m)   { this.memoryAfterUsed = m; }

    // ← new getters & setters:
    public double getDeploymentTime()     { return deploymentTime; }
    public void   setDeploymentTime(double d) { this.deploymentTime = d; }

    public double getLeadTime()           { return leadTime; }
    public void   setLeadTime(double l)      { this.leadTime = l; }


    public double getCost()          { return cost; }
public void   setCost(double c)  { this.cost = c; }
}

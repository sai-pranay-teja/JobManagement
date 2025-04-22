package metrics;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class MetricsDashboardServlet extends HttpServlet {
    private List<MetricRecord> records;

    @Override
    public void init() throws ServletException {
        try {
            Path logsDir = Paths.get(getServletContext().getRealPath("/logs"));
            records = MetricsParser.parseAllLogs(logsDir);
        } catch (IOException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        
        // Split records
        List<MetricRecord> deployments = filterRecords(false);
        List<MetricRecord> rollbacks = filterRecords(true);

        // Compute stats
        DoubleSummaryStatistics deployTimeStats = getStats(deployments, "time");
        DoubleSummaryStatistics deployMemStats = getStats(deployments, "memory");
        DoubleSummaryStatistics rollbackStats = rollbacks.stream()
    .mapToDouble(r -> r.getRollbackTime())
    .filter(t -> t >= 0)  // Exclude invalid values
    .summaryStatistics();

        // Calculate indices
        Map<MetricRecord, Double> indices = new LinkedHashMap<>();
        addDeploymentIndices(indices, deployments, deployTimeStats, deployMemStats);
        addRollbackIndices(indices, rollbacks, rollbackStats);

        req.setAttribute("indexMap", indices);
        req.getRequestDispatcher("/metrics.jsp").forward(req, resp);
    }

    private List<MetricRecord> filterRecords(boolean isRollback) {
        return records.stream()
            .filter(r -> r.isRollback() == isRollback)
            .collect(Collectors.toList());
    }

    private DoubleSummaryStatistics getStats(List<MetricRecord> records, String type) {
        return records.stream()
            .mapToDouble(r -> type.equals("time") ? 
                (r.isRollback() ? r.getRollbackTime() : r.getTotalPipelineTime()) :
                (r.getMemoryAfterUsed() - r.getMemoryBeforeUsed()))
            .summaryStatistics();
    }

    private void addDeploymentIndices(Map<MetricRecord, Double> map, 
                                    List<MetricRecord> records,
                                    DoubleSummaryStatistics timeStats,
                                    DoubleSummaryStatistics memStats) {
        for (MetricRecord rec : records) {
            double memDelta = rec.getMemoryAfterUsed() - rec.getMemoryBeforeUsed();
            double idx = EfficiencyIndexCalculator.computeDeploymentIndex(
                rec.getTotalPipelineTime(), memDelta,
                timeStats.getMin(), timeStats.getMax(),
                memStats.getMin(), memStats.getMax()
            );
            map.put(rec, idx);
        }
    }

    private void addRollbackIndices(Map<MetricRecord, Double> map,
                                  List<MetricRecord> records,
                                  DoubleSummaryStatistics stats) {
        for (MetricRecord rec : records) {
            double idx = EfficiencyIndexCalculator.computeRollbackIndex(
                rec.getRollbackTime(), stats.getMin(), stats.getMax()
            );
            map.put(rec, idx);
        }
    }
}
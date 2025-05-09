package metrics;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class ExportMetricsCSVServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        // Parse logs
        List<MetricRecord> records;
        try {
            Path logsDir = Paths.get(getServletContext().getRealPath("/logs"));
            records = MetricsParser.parseAllLogs(logsDir);
        } catch (IOException e) {
            throw new ServletException("Failed to parse logs: " + e.getMessage(), e);
        }

        // Compute indices
        List<MetricRecord> deployments = filterRecords(records, false);
        List<MetricRecord> rollbacks = filterRecords(records, true);

        DoubleSummaryStatistics deployTimeStats = getStats(deployments, "time");
        DoubleSummaryStatistics deployMemStats = getStats(deployments, "memory");
        DoubleSummaryStatistics rollbackStats = getStats(rollbacks, "time");

        Map<MetricRecord, Double> indices = new LinkedHashMap<>();
        addDeploymentIndices(indices, deployments, deployTimeStats, deployMemStats);
        addRollbackIndices(indices, rollbacks, rollbackStats);

        // Set CSV response
        resp.setContentType("text/csv");
        resp.setHeader("Content-Disposition", "attachment; filename=\"metrics.csv\"");
        
        try (var writer = resp.getWriter()) {
            writer.write("tool,time,memory,index\n");
            for (Map.Entry<MetricRecord, Double> entry : indices.entrySet()) {
                MetricRecord rec = entry.getKey();
                double time = rec.isRollback() ? rec.getRollbackTime() : rec.getTotalPipelineTime();
                double memoryDelta = rec.getMemoryAfterUsed() - rec.getMemoryBeforeUsed();
                double index = entry.getValue();
                writer.printf(Locale.US, "%s,%.1f,%.1f,%.3f%n", 
                    rec.getToolName(), time, memoryDelta, index);
            }
        }
    }

    // --- Helper methods (replicated from MetricsDashboardServlet) ---
    private List<MetricRecord> filterRecords(List<MetricRecord> records, boolean isRollback) {
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
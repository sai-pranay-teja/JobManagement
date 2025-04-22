// package metrics;

// import jakarta.servlet.*;
// import jakarta.servlet.http.*;
// import java.io.IOException;
// import java.nio.file.*;
// import java.util.*;

// public class MetricsDashboardServlet extends HttpServlet {
//     private List<MetricRecord> records;

//     @Override
//     public void init() throws ServletException {
//         try {
//             Path logsDir = Paths.get(getServletContext().getRealPath("/logs"));
//             records = MetricsParser.parseAllLogs(logsDir);
//         } catch (IOException e) {
//             throw new ServletException(e);
//         }
//     }

//     @Override
//     protected void doGet(HttpServletRequest req, HttpServletResponse resp)
//             throws ServletException, IOException {
//         // compute min/max
//         IntSummaryStatistics ts = records.stream()
//             .mapToInt(MetricRecord::getTotalPipelineTime).summaryStatistics();
//         LongSummaryStatistics ms = records.stream()
//             .mapToLong(r -> r.getMemoryAfterUsed() - r.getMemoryBeforeUsed()).summaryStatistics();

//         Map<MetricRecord, Double> idxMap = new LinkedHashMap<>();
//         for (MetricRecord rec : records) {
//             long memDiff = rec.getMemoryAfterUsed() - rec.getMemoryBeforeUsed();
//             double idx = EfficiencyIndexCalculator.computeIndex(
//                 rec.getTotalPipelineTime(), ts.getMin(), ts.getMax(),
//                 memDiff, ms.getMin(), ms.getMax()
//             );
//             idxMap.put(rec, idx);
//         }

//         req.setAttribute("indexMap", idxMap);
//         req.getRequestDispatcher("/metrics.jsp").forward(req, resp);
//     }
// }



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
        // Compute min/max for time and memory delta
        DoubleSummaryStatistics ts = records.stream()
            .mapToDouble(MetricRecord::getTotalPipelineTime)
            .summaryStatistics();

        DoubleSummaryStatistics ms = records.stream()
            .mapToDouble(r -> r.getMemoryAfterUsed() - r.getMemoryBeforeUsed())
            .summaryStatistics();

        Map<MetricRecord, Double> idxMap = new LinkedHashMap<>();
        for (MetricRecord rec : records) {
            double memDelta = rec.getMemoryAfterUsed() - rec.getMemoryBeforeUsed();
            double idx = EfficiencyIndexCalculator.computeIndex(
                rec.getTotalPipelineTime(), ts.getMin(), ts.getMax(),
                memDelta, ms.getMin(), ms.getMax()
            );
            idxMap.put(rec, idx);
        }

        req.setAttribute("indexMap", idxMap);
        req.getRequestDispatcher("/metrics.jsp").forward(req, resp);
    }
}
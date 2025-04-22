package metrics;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class MetricsParser {
    // Regex Patterns
    private static final Pattern HEADER = Pattern.compile("CI/CD Metrics Summary");
    private static final Pattern KV = Pattern.compile("\\|\\s*(.+?)\\s*\\|\\s*(\\S+)\\s*\\|");
    private static final Pattern MEM_BEFORE = Pattern.compile(
    "Memory Usage BEFORE:.*?Mem:\\s+\\S+\\s+(\\d+\\.?\\d*)(Gi|Mi).*?Swap:", 
    Pattern.DOTALL
);
    private static final Pattern MEM_AFTER = Pattern.compile(
    "Memory Usage AFTER:.*?Mem:\\s+\\S+\\s+(\\d+\\.?\\d*)(Gi|Mi).*?Swap:", 
    Pattern.DOTALL
);
    private static final Pattern TIMESTAMP = Pattern.compile("\\| (\\d+) \\|");

    // Pricing (USD per minute)
    private static final Map<String, Double> TOOL_COST_PER_MIN = Map.of(
        "gha", 0.008,    // GitHub Actions
        "codebuild", 0.005, // AWS CodeBuild
        "jenkins", 0.003  // Jenkins (assumed infrastructure cost)
    );

    public static List<MetricRecord> parseAllLogs(Path logsDir) throws IOException {
        List<MetricRecord> records = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(logsDir, "*.log")) {
            for (Path p : ds) {
                records.add(parseLog(p));
            }
        }
        return records;
    }

    public static MetricRecord parseLog(Path logFile) throws IOException {
        List<String> lines = Files.readAllLines(logFile);
        String tool = extractToolName(logFile);
        MetricRecord rec = new MetricRecord(tool);

        long startTime = Long.MAX_VALUE;
        long endTime = Long.MIN_VALUE;
        StringBuilder sb = new StringBuilder();

        // Extract timestamps and build log content
        for (String line : lines) {
            Matcher tsMatcher = TIMESTAMP.matcher(line);
            if (tsMatcher.find()) {
                long timestamp = Long.parseLong(tsMatcher.group(1));
                startTime = Math.min(startTime, timestamp);
                endTime = Math.max(endTime, timestamp);
            }
            sb.append(line).append("\n");
        }

        // Calculate duration and cost
        double durationMin = (endTime - startTime) / 60000.0;
        rec.setDuration(durationMin);
        rec.setCost(calculateCost(tool, durationMin));

        // Parse time metrics
        Matcher kvMatcher = KV.matcher(sb);
        while (kvMatcher.find()) {
            String key = kvMatcher.group(1).trim();
            String val = kvMatcher.group(2).trim();
            try {
                switch (key) {
                    case "Total Pipeline Time (sec)": 
                        rec.setTotalPipelineTime(Double.parseDouble(val)); 
                        break;
                    case "Deployment Time (sec)": 
                        rec.setDeploymentTime(Double.parseDouble(val)); 
                        break;
                    case "Lead Time for Changes (sec)": 
                        rec.setLeadTime(Double.parseDouble(val)); 
                        break;
                }
            } catch (NumberFormatException ignored) {}
        }

        // Parse memory BEFORE and AFTER
        Matcher memBeforeMatcher = MEM_BEFORE.matcher(sb);
        if (memBeforeMatcher.find()) {
            rec.setMemoryBeforeUsed(parseSize(
                memBeforeMatcher.group(1), 
                memBeforeMatcher.group(2)
            ));
        }

        Matcher memAfterMatcher = MEM_AFTER.matcher(sb);
        if (memAfterMatcher.find()) {
            rec.setMemoryAfterUsed(parseSize(
                memAfterMatcher.group(1), 
                memAfterMatcher.group(2)
            ));
        }

        return rec;
    }

    private static double calculateCost(String tool, double durationMin) {
        return TOOL_COST_PER_MIN.getOrDefault(tool, 0.0) * durationMin;
    }

    private static double parseSize(String num, String unit) {
        double v = Double.parseDouble(num);
        return unit.equals("Gi") ? v * 1024 : v; // Convert GiB to MiB
    }

   private static String extractToolName(Path path) {
    String filename = path.getFileName().toString().toLowerCase();
    
    if (filename.contains("gha")) {
        return filename.contains("rollback") ? "gha-rollback" : "gha";
    }
    else if (filename.contains("jenkins")) {
        return filename.contains("rollback") ? "jenkins-rollback" : "jenkins";
    }
    else if (filename.contains("codebuild")) {
        return filename.contains("rollback") ? "codebuild-rollback" : "codebuild";
    }
    }
    return "unknown";
}

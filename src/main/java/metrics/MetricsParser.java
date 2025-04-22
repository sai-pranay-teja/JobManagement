package metrics;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class MetricsParser {
    private static final Pattern KV = Pattern.compile("\\|\\s*(.+?)\\s*\\|\\s*(\\S+)\\s*\\|");
    private static final Pattern MEM_BEFORE = Pattern.compile(
        "Memory Usage BEFORE:.*?\\|\\s*\\d+\\s*\\|\\s*Mem:\\s+\\S+\\s+(\\d+\\.?\\d*)(Gi|Mi)", 
        Pattern.DOTALL
    );
    private static final Pattern MEM_AFTER = Pattern.compile(
        "Memory Usage AFTER:.*?\\|\\s*\\d+\\s*\\|\\s*Mem:\\s+\\S+\\s+(\\d+\\.?\\d*)(Gi|Mi)", 
        Pattern.DOTALL
    );

    public static List<MetricRecord> parseAllLogs(Path logsDir) throws IOException {
        List<MetricRecord> records = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(logsDir, "*.log")) {
            for (Path p : ds) records.add(parseLog(p));
        }
        return records;
    }

    public static MetricRecord parseLog(Path logFile) throws IOException {
        List<String> lines = Files.readAllLines(logFile);
        MetricRecord rec = new MetricRecord(extractToolName(logFile));
        StringBuilder sb = new StringBuilder();

        // Parse timestamps and metrics
        long start = Long.MAX_VALUE, end = Long.MIN_VALUE;
        for (String line : lines) {
            Matcher m = Pattern.compile("\\| (\\d+) \\|").matcher(line);
            if (m.find()) {
                long ts = Long.parseLong(m.group(1));
                start = Math.min(start, ts);
                end = Math.max(end, ts);
            }
            sb.append(line).append("\n");
        }

        // Set duration and cost
        rec.setDuration((end - start) / 60000.0);
        rec.setCost(calculateCost(rec.getToolName(), rec.getDuration()));

        // Parse time metrics
        Matcher kvMatcher = KV.matcher(sb);
        while (kvMatcher.find()) {
            String key = kvMatcher.group(1).trim();
            String val = kvMatcher.group(2).trim();
            try {
                if (key.equals("Total Pipeline Time (sec)")) {
                    rec.setTotalPipelineTime(Double.parseDouble(val));
                } else if (key.equals("Rollback Deployment Time (sec)")) {
                    rec.setRollbackTime(Double.parseDouble(val));
                }
            } catch (NumberFormatException ignored) {}
        }

        // Parse memory only for non-rollbacks
        if (!rec.isRollback()) {
            Matcher mb = MEM_BEFORE.matcher(sb);
            if (mb.find()) rec.setMemoryBeforeUsed(parseSize(mb.group(1), mb.group(2)));
            Matcher ma = MEM_AFTER.matcher(sb);
            if (ma.find()) rec.setMemoryAfterUsed(parseSize(ma.group(1), ma.group(2)));
        }

        return rec;
    }

    private static double parseSize(String num, String unit) {
        double v = Double.parseDouble(num);
        return "Gi".equals(unit) ? v * 1024 : v;
    }

    private static double calculateCost(String tool, double duration) {
        Map<String, Double> prices = Map.of(
            "gha", 0.008, "codebuild", 0.005, "jenkins", 0.003
        );
        return prices.getOrDefault(tool.replace("-rollback", ""), 0.0) * duration;
    }

    private static String extractToolName(Path path) {
        String fn = path.getFileName().toString().toLowerCase();
        if (fn.contains("gha")) return fn.contains("rollback") ? "gha-rollback" : "gha";
        if (fn.contains("jenkins")) return fn.contains("rollback") ? "jenkins-rollback" : "jenkins";
        if (fn.contains("codebuild")) return fn.contains("rollback") ? "codebuild-rollback" : "codebuild";
        return "unknown";
    }
}
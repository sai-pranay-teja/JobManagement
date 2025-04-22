package metrics;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class MetricsParser {
    private static final Pattern TIME_PATTERN = Pattern.compile(
        "\\|\\s*(Total Pipeline Time|Rollback Deployment Time|Rollback completed in)\\s*\\|\\s*(\\d+\\.?\\d*)", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern MEM_USED = Pattern.compile(
        "Mem:\\s+\\S+\\s+(\\d+\\.?\\d*)(Gi|Mi)", 
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

        // Parse timestamps
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

        // Parse time metrics
        Matcher timeMatcher = TIME_PATTERN.matcher(sb);
        while (timeMatcher.find()) {
            String key = timeMatcher.group(1).replaceAll("[^a-zA-Z]", "").toLowerCase();
            String val = timeMatcher.group(2).trim();
            
            try {
                if (key.contains("totalpipeline")) {
                    rec.setTotalPipelineTime(Double.parseDouble(val));
                } 
                else if (key.contains("rollback")) {
                    rec.setRollbackTime(Double.parseDouble(val));
                }
            } catch (NumberFormatException e) {
                System.err.printf("Failed to parse time in %s: %s=%s%n", 
                    logFile.getFileName(), key, val);
            }
        }

        // Parse memory metrics for non-rollbacks
        if (!rec.isRollback()) {
            List<Double> memValues = new ArrayList<>();
            Matcher memMatcher = MEM_USED.matcher(sb);
            while (memMatcher.find()) {
                double value = parseSize(memMatcher.group(1), memMatcher.group(2));
                memValues.add(value);
            }
            if (memValues.size() >= 2) {
                rec.setMemoryBeforeUsed(memValues.get(0));
                rec.setMemoryAfterUsed(memValues.get(1));
            }
        }

        return rec;
    }

    private static double parseSize(String num, String unit) {
        double v = Double.parseDouble(num);
        return unit.equalsIgnoreCase("Gi") ? v * 1024 : v;
    }

    private static String extractToolName(Path path) {
        String fn = path.getFileName().toString().toLowerCase();
        if (fn.contains("gha")) return fn.contains("rollback") ? "gha-rollback" : "gha";
        if (fn.contains("jenkins")) return fn.contains("rollback") ? "jenkins-rollback" : "jenkins";
        if (fn.contains("codebuild")) return fn.contains("rollback") ? "codebuild-rollback" : "codebuild";
        return "unknown";
    }
}
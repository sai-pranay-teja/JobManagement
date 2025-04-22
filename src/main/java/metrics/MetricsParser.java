package metrics;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetricsParser {
    // Matches any “| Metric Name (sec) | 123 |” row
    private static final Pattern KV_SEC = Pattern.compile(
        "\\|\\s*([^|\\(]+?)\\s*(?:\\(sec\\))?\\s*\\|\\s*(\\d+\\.?\\d*)\\s*\\|",
        Pattern.CASE_INSENSITIVE
    );

    // Matches “Rollback completed in 8 seconds.”
    private static final Pattern JENKINS_ROLLBACK_SENTENCE =
        Pattern.compile("Rollback completed in\\s+(\\d+)\\s+seconds\\.", Pattern.CASE_INSENSITIVE);

    // Memory usage pattern
    private static final Pattern MEM_USED = Pattern.compile(
        "Mem:\\s+\\S+\\s+(\\d+\\.?\\d*)(Gi|Mi)",
        Pattern.DOTALL
    );

    // Cost row pattern
    private static final Pattern COST_PATTERN = Pattern.compile(
        "\\|\\s*Cost \\(USD\\)\\s*\\|\\s*(\\d+\\.?\\d*)\\s*\\|"
    );

    /**
     * Parse all .log files in the given directory into MetricRecord list.
     */
    public static List<MetricRecord> parseAllLogs(Path logsDir) throws IOException {
        List<MetricRecord> records = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(logsDir, "*.log")) {
            for (Path p : ds) {
                records.add(parseLog(p));
            }
        }
        return records;
    }

    /**
     * Parse a single log file into a MetricRecord.
     */
    public static MetricRecord parseLog(Path logFile) throws IOException {
        // Read all lines and join
        List<String> lines = Files.readAllLines(logFile);
        String all = String.join("\n", lines);

        // Initialize record with tool name
        String fileName = logFile.getFileName().toString();
        MetricRecord rec = new MetricRecord(extractToolName(fileName));

        // 1) Parse time metrics
        Matcher kv = KV_SEC.matcher(all);
        while (kv.find()) {
            String key = kv.group(1).trim().toLowerCase();
            double val = Double.parseDouble(kv.group(2));
            switch (key) {
                case "total pipeline time":
                    rec.setTotalPipelineTime(val);
                    break;
                case "deployment time":
                case "deployment duration":
                    rec.setDeploymentTime(val);
                    break;
                case "lead time for changes":
                    rec.setLeadTime(val);
                    break;
                case "rollback time":
                case "rollback deployment time":
                    rec.setRollbackTime(val);
                    break;
                default:
                    // ignore other metrics
            }
        }

        // 2) Jenkins-style rollback sentence override
        Matcher jr = JENKINS_ROLLBACK_SENTENCE.matcher(all);
        if (jr.find()) {
            rec.setRollbackTime(Double.parseDouble(jr.group(1)));
        }

        // 3) Parse cost
        Matcher costM = COST_PATTERN.matcher(all);
        if (costM.find()) {
            rec.setCost(Double.parseDouble(costM.group(1)));
        }

        // 4) Parse memory usage for non-rollbacks
        if (!rec.isRollback()) {
            List<Double> mem = new ArrayList<>();
            Matcher mm = MEM_USED.matcher(all);
            while (mm.find()) {
                mem.add(parseSize(mm.group(1), mm.group(2)));
            }
            if (mem.size() >= 2) {
                rec.setMemoryBeforeUsed(mem.get(0));
                rec.setMemoryAfterUsed(mem.get(1));
            }
        }

        return rec;
    }

    // Convert Mi/Gi string to Mi units
    private static double parseSize(String num, String unit) {
        double v = Double.parseDouble(num);
        return unit.equalsIgnoreCase("Gi") ? v * 1024 : v;
    }

    // Derive tool name from filename
    private static String extractToolName(String fn) {
        fn = fn.toLowerCase();
        if (fn.contains("gha")) return fn.contains("rollback") ? "gha-rollback" : "gha";
        if (fn.contains("jenkins")) return fn.contains("rollback") ? "jenkins-rollback" : "jenkins";
        if (fn.contains("codebuild")) return fn.contains("rollback") ? "codebuild-rollback" : "codebuild";
        return "unknown";
    }
}
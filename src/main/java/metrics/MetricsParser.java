package metrics;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class MetricsParser {
    private static final Pattern KV_SEC = Pattern.compile(
        "\\|\\s*([^|\\(]+?)\\s*(?:\\(sec\\))?\\s*\\|\\s*(\\d+\\.?\\d*)\\s*\\|",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern JENKINS_ROLLBACK_SENTENCE =
        Pattern.compile("Rollback completed in\\s+(\\d+)\\s+seconds\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern MEM_USED = Pattern.compile(
        "Mem:\\s+\\S+\\s+(\\d+\\.?\\d*)(Gi|Mi)", Pattern.DOTALL
    );


    // Capture the new Cost row
Pattern COST_PATTERN = Pattern.compile("\\|\\s*Cost \\(USD\\)\\s*\\|\\s*(\\d+\\.?\\d*)\\s*\\|");
Matcher costM = COST_PATTERN.matcher(all);
if (costM.find()) {
    rec.setCost(Double.parseDouble(costM.group(1)));
}

    // ← Restore this so your servlet can still call parseAllLogs(...)
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
        String all = String.join("\n", lines);
        MetricRecord rec = new MetricRecord(extractToolName(logFile.getFileName().toString()));

        // 1. Generic “| Name (sec) | 123 |” rows
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
                    // skip
            }
        }

        // 2. Jenkins‑style sentence override
        Matcher jr = JENKINS_ROLLBACK_SENTENCE.matcher(all);
        if (jr.find()) {
            rec.setRollbackTime(Double.parseDouble(jr.group(1)));
        }

        // 3. Memory before/after (only for non‑rollback runs)
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

    private static double parseSize(String num, String unit) {
        double v = Double.parseDouble(num);
        return unit.equalsIgnoreCase("Gi") ? v * 1024 : v;
    }

    private static String extractToolName(String fn) {
        fn = fn.toLowerCase();
        if (fn.contains("gha")) return fn.contains("rollback") ? "gha-rollback" : "gha";
        if (fn.contains("jenkins")) return fn.contains("rollback") ? "jenkins-rollback" : "jenkins";
        if (fn.contains("codebuild")) return fn.contains("rollback") ? "codebuild-rollback" : "codebuild";
        return "unknown";
    }
}

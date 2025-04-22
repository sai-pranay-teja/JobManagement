package metrics;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class MetricsParser {
    // Matches any “| Metric Name (sec) | 123 |” row
    private static final Pattern KV_SEC = Pattern.compile(
        "\\|\\s*([^|\\(]+?)\\s*(?:\\(sec\\))?\\s*\\|\\s*(\\d+\\.?\\d*)\\s*\\|",
        Pattern.CASE_INSENSITIVE
    );
    // Matches “Rollback completed in 8 seconds.”
    private static final Pattern JENKINS_ROLLBACK_SENTENCE =
        Pattern.compile("Rollback completed in\\s+(\\d+)\\s+seconds\\.", Pattern.CASE_INSENSITIVE);

    // MEM_USED stays the same
    private static final Pattern MEM_USED = Pattern.compile(
        "Mem:\\s+\\S+\\s+(\\d+\\.?\\d*)(Gi|Mi)",
        Pattern.DOTALL
    );

    public static MetricRecord parseLog(Path logFile) throws IOException {
        List<String> lines = Files.readAllLines(logFile);
        MetricRecord rec = new MetricRecord(extractToolName(logFile));

        // Join all lines for regex scanning
        String all = String.join("\n", lines);

        // 1) First, find all “| … | number |” rows
        Matcher kv = KV_SEC.matcher(all);
        while (kv.find()) {
            String key = kv.group(1).trim();
            double val = Double.parseDouble(kv.group(2));
            switch (key.toLowerCase()) {
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
                    // ignore other rows (like Test Summary)
            }
        }

        // 2) If Jenkins‐style sentence exists, override rollbackTime
        Matcher jr = JENKINS_ROLLBACK_SENTENCE.matcher(all);
        if (jr.find()) {
            double rb = Double.parseDouble(jr.group(1));
            rec.setRollbackTime(rb);
        }

        // 3) Parse memory before/after *only* for non‐rollbacks
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

    // unchanged helpers...
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

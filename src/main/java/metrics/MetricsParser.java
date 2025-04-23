// package metrics;

// import java.io.IOException;
// import java.nio.file.DirectoryStream;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;

// public class MetricsParser {
//     // Regex patterns
//     private static final Pattern KV_SEC = Pattern.compile(
//         "\\|\\s*([^|\\(]+?)\\s*(?:\\(sec\\))?\\s*\\|\\s*(\\d+\\.?\\d*)\\s*\\|",
//         Pattern.CASE_INSENSITIVE
//     );
    
//     private static final Pattern JENKINS_ROLLBACK_SENTENCE =
//         Pattern.compile("Rollback completed in\\s+(\\d+)\\s+seconds\\.", Pattern.CASE_INSENSITIVE);
    
//     // FIXED: Added missing closing parenthesis for the first group
//     private static final Pattern MEM_USED = Pattern.compile(
//         "(Mem:\\s+\\S+\\s+(\\d+\\.?\\d*)(Gi|Mi))" +  // free -h format
//         "|(used memory\\s+:\\s+(\\d+\\.?\\d*)\\s+MB)",  // vmstat format
//         Pattern.CASE_INSENSITIVE | Pattern.DOTALL
//     );
    
//     private static final Pattern COST_PATTERN = Pattern.compile(
//         "\\|\\s*Cost \\(USD\\)\\s*\\|\\s*(\\d+\\.?\\d*)\\s*\\|"
//     );
    

//     public static List<MetricRecord> parseAllLogs(Path logsDir) throws IOException {
//         List<MetricRecord> records = new ArrayList<>();
//         try (DirectoryStream<Path> ds = Files.newDirectoryStream(logsDir, "*.log")) {
//             for (Path p : ds) {
//                 records.add(parseLog(p));
//             }
//         }
//         return records;
//     }

//     public static MetricRecord parseLog(Path logFile) throws IOException {
//         List<String> lines = Files.readAllLines(logFile);
//         String all = String.join("\n", lines);
//         String fileName = logFile.getFileName().toString();
//         MetricRecord rec = new MetricRecord(extractToolName(fileName));

//         parseTimeMetrics(all, rec);
//         parseRollbackTime(all, rec);
//         parseCost(all, rec);
//         parseMemory(all, rec);

//         return rec;
//     }

//     private static void parseTimeMetrics(String content, MetricRecord rec) {
//         Matcher kv = KV_SEC.matcher(content);
//         while (kv.find()) {
//             String key = kv.group(1).trim().toLowerCase();
//             double val = Double.parseDouble(kv.group(2));
//             switch (key) {
//                 case "total pipeline time": rec.setTotalPipelineTime(val); break;
//                 case "deployment time":
//                 case "deployment duration": rec.setDeploymentTime(val); break;
//                 case "lead time for changes": rec.setLeadTime(val); break;
//                 case "rollback time":
//                 case "rollback deployment time": rec.setRollbackTime(val); break;
//             }
//         }
//     }

//     private static void parseRollbackTime(String content, MetricRecord rec) {
//         Matcher jr = JENKINS_ROLLBACK_SENTENCE.matcher(content);
//         if (jr.find()) {
//             rec.setRollbackTime(Double.parseDouble(jr.group(1)));
//         }
//     }

//     private static void parseCost(String content, MetricRecord rec) {
//         Matcher costM = COST_PATTERN.matcher(content);
//         if (costM.find()) {
//             rec.setCost(Double.parseDouble(costM.group(1)));
//         }
//     }

//     private static void parseMemory(String content, MetricRecord rec) {
//         if (rec.isRollback()) return;

//         List<Double> mem = new ArrayList<>();
//         Matcher mm = MEM_USED.matcher(content);
//         while (mm.find()) {
//             if (mm.group(2) != null) { // free -h format
//                 double value = Double.parseDouble(mm.group(2));
//                 String unit = mm.group(3);
//                 mem.add(unit.equalsIgnoreCase("Gi") ? value * 1024 : value);
//             } else if (mm.group(5) != null) { // vmstat format
//                 mem.add(Double.parseDouble(mm.group(5)));
//             }
//         }
        
//         if (mem.size() >= 2) {
//             rec.setMemoryBeforeUsed(mem.get(0));
//             rec.setMemoryAfterUsed(mem.get(1));
//         }
//     }

//     private static String extractToolName(String fn) {
//     fn = fn.toLowerCase();
    
//     if (fn.contains("jenkins")) {
//         return fn.contains("rollback") ? "jenkins-rollback" : "jenkins";
//     } 
//     else if (fn.contains("gha")) {
//         return fn.contains("rollback") ? "gha-rollback" : "gha";
//     } 
//     else if (fn.contains("codebuild")) {
//         return fn.contains("rollback") ? "codebuild-rollback" : "codebuild";
//     }
    
//     return "unknown";
// }
// }


package metrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MetricsParser {
    // Regex patterns
    private static final Pattern KV_SEC = Pattern.compile(
        "\\|\\s*([^|\\(]+?)\\s*(?:\\(sec\\))?\\s*\\|\\s*(\\d+\\.?\\d*)\\s*\\|",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern JENKINS_ROLLBACK_SENTENCE =
        Pattern.compile("Rollback completed in\\s+(\\d+)\\s+seconds\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern MEM_USED = Pattern.compile(
        "(Mem:\\s+\\S+\\s+(\\d+\\.?\\d*)(Gi|Mi))" +
        "|(used memory\\s+:\\s+(\\d+\\.?\\d*)\\s+MB)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern COST_PATTERN = Pattern.compile(
        "\\|\\s*Cost \\(USD\\)\\s*\\|\\s*(\\d+\\.?\\d*)\\s*\\|"
    );

    public static List<MetricRecord> parseAllLogs(Path logsDir) throws IOException {
        List<MetricRecord> records = new ArrayList<>();
        // Recursively scan all .log files in logsDir and subdirectories
        try (Stream<Path> paths = Files.walk(logsDir)) {
            paths.filter(p -> p.toString().toLowerCase().endsWith(".log"))
                 .forEach(p -> {
                     try {
                         records.add(parseLog(p));
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                 });
        }
        return records;
    }

    public static MetricRecord parseLog(Path logFile) throws IOException {
        String content = Files.readString(logFile);
        String fileName = logFile.getFileName().toString();
        MetricRecord rec = new MetricRecord(extractToolName(fileName));

        parseTimeMetrics(content, rec);
        parseRollbackTime(content, rec);
        parseCost(content, rec);
        parseMemory(content, rec);

        return rec;
    }

    private static void parseTimeMetrics(String content, MetricRecord rec) {
        Matcher kv = KV_SEC.matcher(content);
        while (kv.find()) {
            String key = kv.group(1).trim().toLowerCase();
            double val = Double.parseDouble(kv.group(2));
            switch (key) {
                case "total pipeline time": rec.setTotalPipelineTime(val); break;
                case "deployment time":
                case "deployment duration": rec.setDeploymentTime(val); break;
                case "lead time for changes": rec.setLeadTime(val); break;
                case "rollback time":
                case "rollback deployment time": rec.setRollbackTime(val); break;
            }
        }
    }

    private static void parseRollbackTime(String content, MetricRecord rec) {
        Matcher jr = JENKINS_ROLLBACK_SENTENCE.matcher(content);
        if (jr.find()) {
            rec.setRollbackTime(Double.parseDouble(jr.group(1)));
        }
    }

    private static void parseCost(String content, MetricRecord rec) {
        Matcher costM = COST_PATTERN.matcher(content);
        if (costM.find()) {
            rec.setCost(Double.parseDouble(costM.group(1)));
        }
    }

    private static void parseMemory(String content, MetricRecord rec) {
        if (rec.isRollback()) return;

        List<Double> mem = new ArrayList<>();
        Matcher mm = MEM_USED.matcher(content);
        while (mm.find()) {
            if (mm.group(2) != null) {
                double value = Double.parseDouble(mm.group(2));
                String unit = mm.group(3);
                mem.add(unit.equalsIgnoreCase("Gi") ? value * 1024 : value);
            } else if (mm.group(5) != null) {
                mem.add(Double.parseDouble(mm.group(5)));
            }
        }
        if (mem.size() >= 2) {
            rec.setMemoryBeforeUsed(mem.get(0));
            rec.setMemoryAfterUsed(mem.get(1));
        }
    }

    private static String extractToolName(String fn) {
        fn = fn.toLowerCase();
        if (fn.contains("jenkins")) {
            return fn.contains("rollback") ? "jenkins-rollback" : "jenkins";
        } else if (fn.contains("gha")) {
            return fn.contains("rollback") ? "gha-rollback" : "gha";
        } else if (fn.contains("codebuild")) {
            return fn.contains("rollback") ? "codebuild-rollback" : "codebuild";
        }
        return "unknown";
    }
}
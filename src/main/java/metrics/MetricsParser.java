package metrics;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class MetricsParser {
    private static final Pattern HEADER = Pattern.compile("CI/CD Metrics Summary");
    private static final Pattern KV = Pattern.compile("\|\\s*(.+?)\\s*\|\\s*(\\S+)\\s*\|");
    private static final Pattern MEM_BEFORE = Pattern.compile("Mem:.*used\\s+(\\d+\\.\\d+)(Gi|Mi)");
    private static final Pattern MEM_AFTER  = Pattern.compile("Memory Usage AFTER:.*used\\s+(\\d+\\.\\d+)(Gi|Mi)", Pattern.DOTALL);

    public static List<MetricRecord> parseAllLogs(Path logsDir) throws IOException {
        List<MetricRecord> list = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(logsDir, "*.log")) {
            for (Path p : ds) {
                list.add(parseLog(p));
            }
        }
        return list;
    }

    public static MetricRecord parseLog(Path logFile) throws IOException {
        List<String> lines = Files.readAllLines(logFile);
        String tool = logFile.getFileName().toString().replaceAll("\\.log$", "");
        MetricRecord rec = new MetricRecord(tool);

        Iterator<String> it = lines.iterator();
        while (it.hasNext()) {
            String line = it.next();
            if (HEADER.matcher(line).find()) {
                // advance until first KV table row
                while (it.hasNext()) {
                    line = it.next();
                    Matcher m = KV.matcher(line);
                    if (!m.find()) continue;
                    String key = m.group(1).trim();
                    String val = m.group(2).trim();
                    switch (key) {
                        case "Total Pipeline Time (sec)": rec.setTotalPipelineTime(Integer.parseInt(val)); break;
                        case "Deployment Time (sec)":     rec.setDeploymentTime(Integer.parseInt(val)); break;
                        case "Lead Time for Changes (sec)": rec.setLeadTime(Integer.parseInt(val)); break;
                        case "Rollback Time (sec)":       rec.setRollbackTime(val.equals("N/A")? -1 : Integer.parseInt(val)); break;
                        default: break;
                    }
                }

                // parse memory before/after
                StringBuilder sb = new StringBuilder();
                while (it.hasNext()) sb.append(it.next()).append("\n");
                Matcher mb = MEM_BEFORE.matcher(sb);
                if (mb.find()) rec.setMemoryBeforeUsed(parseSize(mb.group(1), mb.group(2)));
                Matcher ma = MEM_AFTER.matcher(sb);
                if (ma.find()) rec.setMemoryAfterUsed(parseSize(ma.group(1), ma.group(2)));
                break;
            }
        }
        return rec;
    }

    private static long parseSize(String num, String unit) {
        double v = Double.parseDouble(num);
        if (unit.equals("Gi")) return (long)(v * 1024);
        else /* Mi */             return (long)(v);
    }
}
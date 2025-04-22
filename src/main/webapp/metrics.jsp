<%@ page import="java.util.*, metrics.MetricRecord" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<%
    Map<MetricRecord, Double> indexMap = (Map<MetricRecord, Double>) request.getAttribute("indexMap");
    List<MetricRecord> deployments = new ArrayList<>();
    List<MetricRecord> rollbacks = new ArrayList<>();
    
    for (MetricRecord r : indexMap.keySet()) {
        if (r.isRollback()) rollbacks.add(r);
        else deployments.add(r);
    }
%>
<!DOCTYPE html>
<html>
<head>
    <title>CI/CD Metrics Report</title>
    <script src="https://www.gstatic.com/charts/loader.js"></script>
    <style>
        .metric-table { border-collapse: collapse; width: 80%; margin: 20px 0; }
        .metric-table th, .metric-table td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        .metric-table th { background-color: #f8f9fa; }
        .na { color: #6c757d; font-style: italic; }
        .chart-container { margin: 40px 0; }
    </style>
    <script>
        google.charts.load('current', {packages: ['corechart']});
        google.charts.setOnLoadCallback(drawChart);

        function drawChart() {
            const data = new google.visualization.DataTable();
            data.addColumn('string', 'Tool');
            data.addColumn('number', 'Efficiency Index');

            <% for (MetricRecord rec : indexMap.keySet()) { %>
                data.addRow(['<%= rec.getToolName() %>', <%= indexMap.get(rec) %>]);
            <% } %>

            const options = {
                title: 'CI/CD Efficiency Index Comparison',
                hAxis: { title: 'Tools' },
                vAxis: { 
                    title: 'Index (0-1 scale)', 
                    minValue: 0,
                    maxValue: 1,
                    viewWindow: { min: 0, max: 1 }
                },
                chartArea: { width: '70%', height: '70%' }
            };

            const chart = new google.visualization.ColumnChart(document.getElementById('chart_div'));
            chart.draw(data, options);
        }
    </script>
</head>

<a href="exportMetrics" style="display: block; margin: 20px;">Download Full Metrics CSV</a>
<body>
    <div class="chart-container">
        <div id="chart_div" style="width: 1000px; height: 600px;"></div>
    </div>

    <div class="metrics-section">
        <h2>Deployment Metrics</h2>
        <table class="metric-table">
            <tr>
                <th>Tool</th>
                <th>Time (sec)</th>
                <th>Memory Δ (MiB)</th>
                <th>Efficiency Index</th>
            </tr>
            <% for (MetricRecord r : deployments) { 
                double memDelta = r.getMemoryAfterUsed() - r.getMemoryBeforeUsed();
            %>
            <tr>
                <td><%= r.getToolName() %></td>
                <td><%= String.format("%.1f", r.getTotalPipelineTime()) %></td>
                <td><%= String.format("%.2f", memDelta) %></td>
                <td><%= String.format("%.3f", indexMap.get(r)) %></td>
            </tr>
            <% } %>
        </table>
    </div>

    <div class="metrics-section">
        <h2>Rollback Metrics</h2>
        <table class="metric-table">
            <tr>
                <th>Tool</th>
                <th>Time (sec)</th>
                <th>Efficiency Index</th>
            </tr>
            <% for (MetricRecord r : rollbacks) { %>
            <tr>
                <td><%= r.getToolName() %></td>
                <td><%= r.getRollbackTime() > 0 ? String.format("%.1f", r.getRollbackTime()) : "<span class='na'>N/A</span>" %></td>
                <td><%= String.format("%.3f", indexMap.get(r)) %></td>
            </tr>
            <% } %>
        </table>
    </div>
</body>
</html>
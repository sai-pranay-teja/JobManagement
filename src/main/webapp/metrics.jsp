<%@ page isErrorPage="true" %>
<%
    out.println("CHECKPOINT: JSP reached.<br>");
    Object obj = request.getAttribute("indexMap");
    if (obj == null) out.println("indexMap is NULL<br>");
    else out.println("indexMap is NOT NULL<br>");
%>



<%@ page import="java.util.*, metrics.MetricRecord" %>
<html>
<head>
  <title>CI/CD Metrics Dashboard</title>
  <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
  <script>
    google.charts.load('current', {packages:['corechart','table']});
    google.charts.setOnLoadCallback(drawChart);
    function drawChart() {
      var data = new google.visualization.DataTable();
      data.addColumn('string', 'Tool');
      data.addColumn('number', 'Efficiency Index');
      <% Map<MetricRecord,Double> map = (Map)request.getAttribute("indexMap");
         for (MetricRecord rec : map.keySet()) {
             double idx = map.get(rec);
      %>
      data.addRow(['<%=rec.getToolName()%>', <%=idx%>]);
      <% } %>
      var chart = new google.visualization.ColumnChart(
          document.getElementById('chart_div'));
      chart.draw(data);
    }
  </script>
</head>
<body>
  <h1>CI/CD Efficiency Comparison</h1>
  <div id="chart_div" style="width: 800px; height: 400px;"></div>
  <h2>Detailed Metrics</h2>
  <table border="1">
    <tr><th>Tool</th><th>Total Time</th><th>Memory Δ (Mi)</th><th>Index</th></tr>
    <% for (Map.Entry<MetricRecord,Double> e : map.entrySet()) {
         MetricRecord r = e.getKey(); double i = e.getValue();
    %>
    <tr>
      <td><%=r.getToolName()%></td>
      <td><%=r.getTotalPipelineTime()%></td>
      <td><%=(r.getMemoryAfterUsed()-r.getMemoryBeforeUsed())%></td>
      <td><%=String.format("%.3f", i)%></td>
    </tr>
    <% } %>
  </table>
</body>
</html>
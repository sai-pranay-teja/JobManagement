<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="metrics.MetricRecord" %>

<%
    // Fallback if no data  
    Object _idxMap = request.getAttribute("indexMap");
    if (_idxMap == null) {
%>
    <html>
    <head><title>No Metrics</title></head>
    <body>
      <h2>No data received. Servlet may not be forwarding properly.</h2>
    </body>
    </html>
<%
      return;
    }
%>

<html>
<head>
  <title>CI/CD Metrics Dashboard</title>
  <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
  <script>
    // prepare the chart
    google.charts.load('current', {packages:['corechart','table']});
    google.charts.setOnLoadCallback(drawChart);
    function drawChart() {
      var data = new google.visualization.DataTable();
      data.addColumn('string', 'Tool');
      data.addColumn('number', 'Efficiency Index');

      <!-- iterate entirely in JSTL/EL -->
      <c:forEach var="entry" items="${indexMap.entrySet()}">
        data.addRow([
          '${entry.key.toolName}',
          ${entry.value}
        ]);
      </c:forEach>

      var chart = new google.visualization.ColumnChart(
        document.getElementById('chart_div')
      );
      chart.draw(data);
    }
  </script>
</head>
<body>
  <h1>CI/CD Efficiency Comparison</h1>
  <div id="chart_div" style="width: 800px; height: 400px;"></div>

  <h2>Detailed Metrics</h2>
  <table border="1">
    <tr>
      <th>Tool</th>
      <th>Total Time (sec)</th>
      <th>Memory Δ (Mi)</th>
      <th>Index</th>
    </tr>
    <c:forEach var="entry" items="${indexMap.entrySet()}">
      <c:set var="rec" value="${entry.key}" />
      <c:set var="idx" value="${entry.value}" />
      <tr>
        <td>${rec.toolName}</td>
        <td>${rec.totalPipelineTime}</td>
        <td>${rec.memoryAfterUsed - rec.memoryBeforeUsed}</td>
        <td>${fn:formatNumber(idx, '###0.000')}</td>
      </tr>
    </c:forEach>
  </table>
</body>
</html>

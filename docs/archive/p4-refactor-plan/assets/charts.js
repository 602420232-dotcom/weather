(function() {
  var style = getComputedStyle(document.documentElement);
  var accent = style.getPropertyValue('--accent').trim();
  var accent2 = style.getPropertyValue('--accent2').trim();
  var ink = style.getPropertyValue('--ink').trim();
  var muted = style.getPropertyValue('--muted').trim();
  var rule = style.getPropertyValue('--rule').trim();
  var bg2 = style.getPropertyValue('--bg2').trim();
  var success = style.getPropertyValue('--success').trim();
  var warning = style.getPropertyValue('--warning').trim();
  var danger = style.getPropertyValue('--danger').trim();

  // --- Chart: Tech Stack Matrix (Scatter) ---
  var chartMatrix = echarts.init(document.getElementById('chart-matrix'), null, { renderer: 'svg' });
  chartMatrix.setOption({
    animation: false,
    tooltip: {
      trigger: 'item',
      appendToBody: true,
      formatter: function(p) {
        return '<strong>' + p.data[2] + '</strong><br/>成熟度: ' + p.data[0] + '<br/>升级优先级: ' + p.data[1];
      }
    },
    grid: { left: '10%', right: '10%', bottom: '12%', top: '10%' },
    xAxis: {
      type: 'value',
      name: '技术成熟度',
      nameTextStyle: { color: muted },
      min: 0, max: 10,
      axisLine: { lineStyle: { color: rule } },
      axisLabel: { color: muted },
      splitLine: { lineStyle: { color: rule, type: 'dashed' } }
    },
    yAxis: {
      type: 'value',
      name: '升级优先级',
      nameTextStyle: { color: muted },
      min: 0, max: 10,
      axisLine: { lineStyle: { color: rule } },
      axisLabel: { color: muted },
      splitLine: { lineStyle: { color: rule, type: 'dashed' } }
    },
    series: [{
      type: 'scatter',
      symbolSize: function(d) { return d[3] * 8; },
      data: [
        [2, 9.5, 'Spring Boot', 4, danger],
        [2, 9, 'Spring Cloud', 4, danger],
        [4, 7, 'Gateway', 3, warning],
        [8, 3, '前端 Vue/Vite', 2, success],
        [3, 8, '算法引擎', 3.5, warning],
        [5, 7, '配置安全', 2.5, warning],
        [6, 4, 'CI/CD', 2, accent]
      ],
      itemStyle: {
        color: function(p) { return p.data[4]; },
        shadowBlur: 10,
        shadowColor: 'rgba(0,0,0,0.3)'
      },
      label: {
        show: true,
        formatter: function(p) { return p.data[2]; },
        position: 'top',
        color: ink,
        fontSize: 11
      }
    }],
    graphic: [
      {
        type: 'rect',
        left: '10%', top: '10%',
        shape: { width: '40%', height: '40%' },
        style: { fill: 'rgba(248,113,113,0.05)', stroke: danger, lineWidth: 1, lineDash: [4, 4] }
      },
      {
        type: 'text',
        left: '12%', top: '12%',
        style: { text: '高优先级 / 低成熟度', fill: danger, fontSize: 11 }
      },
      {
        type: 'rect',
        left: '50%', top: '50%',
        shape: { width: '40%', height: '40%' },
        style: { fill: 'rgba(74,222,128,0.05)', stroke: success, lineWidth: 1, lineDash: [4, 4] }
      },
      {
        type: 'text',
        left: '52%', top: '52%',
        style: { text: '低优先级 / 高成熟度', fill: success, fontSize: 11 }
      }
    ]
  });
  window.addEventListener('resize', function() { chartMatrix.resize(); });
})();

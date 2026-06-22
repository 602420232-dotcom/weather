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

  // --- Chart: Frontend Chunks ---
  var chartChunks = echarts.init(document.getElementById('chart-chunks'), null, { renderer: 'svg' });
  chartChunks.setOption({
    animation: false,
    tooltip: {
      trigger: 'axis',
      appendToBody: true,
      axisPointer: { type: 'shadow' }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: ['vue-vendor', 'element-plus', 'element-icons', 'echarts', 'index'],
      axisLine: { lineStyle: { color: rule } },
      axisLabel: { color: muted }
    },
    yAxis: {
      type: 'value',
      name: 'KB (gzip)',
      nameTextStyle: { color: muted },
      axisLine: { lineStyle: { color: rule } },
      axisLabel: { color: muted },
      splitLine: { lineStyle: { color: rule, type: 'dashed' } }
    },
    series: [{
      type: 'bar',
      data: [
        { value: 145.32, itemStyle: { color: accent } },
        { value: 280.84, itemStyle: { color: accent2 } },
        { value: 42.15, itemStyle: { color: accent + '99' } },
        { value: 198.67, itemStyle: { color: accent + '66' } },
        { value: 156.43, itemStyle: { color: muted } }
      ],
      barWidth: '50%',
      label: {
        show: true,
        position: 'top',
        formatter: '{c} KB',
        color: ink
      }
    }]
  });
  window.addEventListener('resize', function() { chartChunks.resize(); });

  // --- Chart: 5D-VAR RMSE Improvement ---
  var chart5dvar = echarts.init(document.getElementById('chart-5dvar'), null, { renderer: 'svg' });
  chart5dvar.setOption({
    animation: false,
    tooltip: {
      trigger: 'axis',
      appendToBody: true
    },
    legend: {
      data: ['最大改善', '平均改善'],
      textStyle: { color: muted },
      top: 5
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: ['3D-VAR', '4D-VAR', '5D-VAR', '5D-VAR+'],
      axisLine: { lineStyle: { color: rule } },
      axisLabel: { color: muted }
    },
    yAxis: {
      type: 'value',
      name: 'RMSE 改善 (%)',
      nameTextStyle: { color: muted },
      axisLine: { lineStyle: { color: rule } },
      axisLabel: { color: muted, formatter: '{value}%' },
      splitLine: { lineStyle: { color: rule, type: 'dashed' } }
    },
    series: [
      {
        name: '最大改善',
        type: 'bar',
        data: [22.1, 29.6, 31.2, 38.8],
        itemStyle: { color: accent },
        barWidth: '30%',
        label: { show: true, position: 'top', formatter: '{c}%', color: ink }
      },
      {
        name: '平均改善',
        type: 'bar',
        data: [15.3, 21.8, 23.5, 29.4],
        itemStyle: { color: accent2 },
        barWidth: '30%',
        label: { show: true, position: 'top', formatter: '{c}%', color: ink }
      }
    ]
  });
  window.addEventListener('resize', function() { chart5dvar.resize(); });

  // --- Chart: Grayscale Verification ---
  var chartGray = echarts.init(document.getElementById('chart-gray'), null, { renderer: 'svg' });
  chartGray.setOption({
    animation: false,
    tooltip: {
      trigger: 'item',
      appendToBody: true,
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '10%',
      top: 'center',
      textStyle: { color: muted }
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderRadius: 6,
        borderColor: bg2,
        borderWidth: 2
      },
      label: {
        show: true,
        formatter: '{b}\n{c}',
        color: ink
      },
      data: [
        { value: 23, name: 'PASS', itemStyle: { color: success } },
        { value: 1, name: 'SKIP', itemStyle: { color: warning } },
        { value: 0, name: 'FAIL', itemStyle: { color: '#ef4444' } }
      ]
    }]
  });
  window.addEventListener('resize', function() { chartGray.resize(); });
})();

/* 血常规指标趋势图：切换指标、标注参考区间 */
(function () {
  'use strict';

  var canvas = document.getElementById('labChart');
  var select = document.getElementById('labItem');
  if (!canvas || !select || typeof Chart === 'undefined') return;

  var chart = null;
  var days = window.BP_DAYS || 0;

  function bandPlugin(low, high, unit) {
    return {
      id: 'refband',
      beforeDatasetsDraw: function (c) {
        var y = c.scales.y, area = c.chartArea;
        if (!y || !area || low == null || high == null) return;
        var top = y.getPixelForValue(high), bottom = y.getPixelForValue(low);
        var t = Math.max(area.top, Math.min(top, bottom));
        var b = Math.min(area.bottom, Math.max(top, bottom));
        if (b <= t) return;
        c.ctx.save();
        c.ctx.fillStyle = 'rgba(25,135,84,.10)';
        c.ctx.fillRect(area.left, t, area.right - area.left, b - t);
        c.ctx.restore();
      },
      afterDatasetsDraw: function (c) {
        var y = c.scales.y, area = c.chartArea;
        if (!y || !area) return;
        c.ctx.save();
        c.ctx.fillStyle = 'rgba(25,135,84,.75)';
        c.ctx.font = '11px sans-serif';
        var py = y.getPixelForValue(high);
        if (py >= area.top && py <= area.bottom) c.ctx.fillText('参考上限 ' + high, area.left + 4, py - 4);
        var pd = y.getPixelForValue(low);
        if (pd >= area.top && pd <= area.bottom) c.ctx.fillText('参考下限 ' + low, area.left + 4, pd + 12);
        c.ctx.restore();
      }
    };
  }

  function render(item) {
    fetch('/lab/chart/' + encodeURIComponent(item) + '?days=' + days, { cache: 'no-store' })
      .then(function (r) { return r.json(); })
      .then(function (data) {
        if (chart) { chart.destroy(); chart = null; }
        var pts = data.points || [];
        var labels = pts.map(function (p) { return p[0].slice(5); });
        var values = pts.map(function (p) { return p[1]; });
        var abnormal = pts.map(function (p) {
          return (p[1] < data.low || p[1] > data.high) ? '#dc3545' : '#0d6efd';
        });

        chart = new Chart(canvas, {
          type: 'line',
          data: {
            labels: labels,
            datasets: [{
              label: data.label,
              data: values,
              borderColor: '#0d6efd',
              backgroundColor: 'rgba(13,110,253,.10)',
              pointBackgroundColor: abnormal,
              pointBorderColor: abnormal,
              pointRadius: 5,
              pointHoverRadius: 7,
              borderWidth: 2,
              tension: 0.3,
              fill: true,
              spanGaps: true
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: { display: false },
              tooltip: {
                callbacks: {
                  label: function (item) {
                    return data.label + '：' + item.formattedValue + ' ' + data.unit;
                  }
                }
              }
            },
            scales: {
              x: { grid: { display: false }, ticks: { color: '#6c757d' } },
              y: { grid: { color: 'rgba(0,0,0,.06)' }, ticks: { color: '#6c757d' } }
            }
          },
          plugins: [bandPlugin(data.low, data.high, data.unit)]
        });
      })
      .catch(function () { /* 忽略 */ });
  }

  select.addEventListener('change', function () { render(select.value); });
  if (select.value) render(select.value);
})();

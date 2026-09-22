/* 血常规指标趋势图：切换指标、标注参考区间 */
(function () {
  'use strict';

  var canvas = document.getElementById('labChart');
  var select = document.getElementById('labItem');
  if (!canvas || !select) return;

  var chart = null;
  var days = window.BP_DAYS || 0;
  // 小屏适配：标签密度与点大小随宽度变化
  var width = canvas.clientWidth || (canvas.parentElement && canvas.parentElement.clientWidth) || 360;
  var narrow = width < 420;

  // 本脚本可能在 Chart.js 之前加载，等库就绪后再初始化
  function ready(fn) {
    if (typeof Chart !== 'undefined') { fn(); return; }
    var tries = 0;
    var timer = setInterval(function () {
      if (typeof Chart !== 'undefined') {
        clearInterval(timer);
        fn();
      } else if (++tries > 60) {          // 约 6 秒后放弃
        clearInterval(timer);
        canvas.insertAdjacentHTML('afterend',
          '<div class="alert alert-warning small mt-2">图表库未加载，请刷新页面重试。</div>');
      }
    }, 100);
  }

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
        c.ctx.fillStyle = 'rgba(127,227,192,.12)';
        c.ctx.fillRect(area.left, t, area.right - area.left, b - t);
        c.ctx.restore();
      },
      afterDatasetsDraw: function (c) {
        var y = c.scales.y, area = c.chartArea;
        if (!y || !area) return;
        c.ctx.save();
        c.ctx.fillStyle = 'rgba(127,227,192,.85)';
        c.ctx.font = '11px "Noto Sans SC", sans-serif';
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
          return (p[1] < data.low || p[1] > data.high) ? '#ff8080' : '#ffdd7a';
        });

        chart = new Chart(canvas, {
          type: 'line',
          data: {
            labels: labels,
            datasets: [{
              label: data.label,
              data: values,
              borderColor: '#ffdd7a',
              backgroundColor: 'rgba(255,221,122,.12)',
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
              x: {
                grid: { display: false },
                ticks: {
                  autoSkip: true,
                  maxTicksLimit: narrow ? 5 : 8,
                  maxRotation: 0,
                  color: '#a8b0d8',
                  font: { size: narrow ? 10 : 11 }
                }
              },
              y: {
                grid: { color: 'rgba(232,194,90,.10)' },
                ticks: { color: '#a8b0d8', font: { size: narrow ? 10 : 11 } }
              }
            }
          },
          plugins: [bandPlugin(data.low, data.high, data.unit)]
        });
      })
      .catch(function () { /* 忽略 */ });
  }

  select.addEventListener('change', function () { render(select.value); });
  ready(function () {
    if (select.value) render(select.value);
  });
})();

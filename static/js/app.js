/* 宋宝宝的记录 · 前端交互：趋势曲线图、分级预览、离线缓存 */
(function () {
  'use strict';

  /* ---------- Service Worker（PWA 离线） ---------- */
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', function () {
      navigator.serviceWorker.register('/sw.js').catch(function () { /* 忽略 */ });
    });
  }

  /* ---------- 血压分级（与后端保持一致） ---------- */
  var LEVELS = {
    low: { name: '血压偏低', css: 'text-bg-secondary' },
    normal: { name: '正常血压', css: 'text-bg-success' },
    elevated: { name: '正常高值', css: 'text-bg-info' },
    stage1: { name: '1 级高血压', css: 'text-bg-warning' },
    stage2: { name: '2 级高血压', css: 'text-bg-danger' },
    stage3: { name: '3 级高血压', css: 'text-bg-danger' }
  };
  var ORDER = ['low', 'normal', 'elevated', 'stage1', 'stage2', 'stage3'];

  function sysLevel(s) {
    if (s < 90) return 'low';
    if (s < 120) return 'normal';
    if (s < 140) return 'elevated';
    if (s < 160) return 'stage1';
    if (s < 180) return 'stage2';
    return 'stage3';
  }
  function diaLevel(d) {
    if (d < 60) return 'low';
    if (d < 80) return 'normal';
    if (d < 90) return 'elevated';
    if (d < 100) return 'stage1';
    if (d < 110) return 'stage2';
    return 'stage3';
  }
  function classify(s, d) {
    var a = sysLevel(s), b = diaLevel(d);
    return LEVELS[ORDER.indexOf(a) >= ORDER.indexOf(b) ? a : b];
  }

  /* ---------- 参考线插件：收缩压 140 / 舒张压 90 ---------- */
  var thresholdPlugin = {
    id: 'thresholds',
    afterDatasetsDraw: function (chart) {
      var ctx = chart.ctx, y = chart.scales.y, area = chart.chartArea;
      if (!y || !area) return;
      [{ v: 140, axis: 'y', color: 'rgba(255,221,122,.55)', text: '收缩压 140' },
       { v: 90, axis: 'y', color: 'rgba(127,227,192,.55)', text: '舒张压 90' },
       { v: 100, axis: 'y1', color: 'rgba(169,156,255,.5)', text: '心率 100' }].forEach(function (t) {
        // 收缩压/舒张压用左轴，心率参考线用右轴
        var scale = chart.scales[t.axis];
        var visible = t.axis === 'y' ? chart.isDatasetVisible(0) || chart.isDatasetVisible(1)
                                     : chart.isDatasetVisible(2);
        if (!scale || !visible) return;
        var py = scale.getPixelForValue(t.v);
        if (py < area.top || py > area.bottom) return;
        ctx.save();
        ctx.setLineDash([6, 4]);
        ctx.strokeStyle = t.color;
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(area.left, py);
        ctx.lineTo(area.right, py);
        ctx.stroke();
        ctx.setLineDash([]);
        ctx.fillStyle = t.color;
        ctx.font = '11px sans-serif';
        ctx.fillText(t.text, area.left + 4, py - 4);
        ctx.restore();
      });
    }
  };

  function fillGradient(canvas, rgba) {
    var ctx = canvas.getContext('2d');
    var g = ctx.createLinearGradient(0, 0, 0, canvas.clientHeight || 260);
    g.addColorStop(0, rgba.replace('COLOR', '0.22'));
    g.addColorStop(1, rgba.replace('COLOR', '0'));
    return g;
  }

  /* ---------- 初始化（等 DOM 与内联数据就绪） ---------- */
  function init() {
    initForm();
    initChart();
  }

  function initForm() {
    var sysInput = document.getElementById('systolic');
    var diaInput = document.getElementById('diastolic');
    var preview = document.getElementById('levelPreview');
    if (sysInput && diaInput && preview) {
      var render = function () {
        var s = parseInt(sysInput.value, 10), d = parseInt(diaInput.value, 10);
        if (!s || !d) { preview.innerHTML = ''; return; }
        var lv = classify(s, d);
        preview.innerHTML = '<div class="alert alert-light border mb-0">本次测量：' +
          '<span class="badge ' + lv.css + '">' + lv.name + '</span></div>';
      };
      sysInput.addEventListener('input', render);
      diaInput.addEventListener('input', render);
      render();
    }

    var bpForm = document.getElementById('bpForm');
    if (bpForm) {
      bpForm.addEventListener('submit', function (event) {
        if (!bpForm.checkValidity()) {
          event.preventDefault();
          event.stopPropagation();
        }
        bpForm.classList.add('was-validated');
      });
    }
  }

  function initChart() {
    var canvas = document.getElementById('bpChart');
    if (!canvas) return;
    // Chart.js 可能晚于本脚本加载，等库就绪再初始化
    if (typeof Chart === 'undefined') {
      var tries = 0;
      var timer = setInterval(function () {
        if (typeof Chart !== 'undefined') {
          clearInterval(timer);
          initChart();
        } else if (++tries > 60) {
          clearInterval(timer);
          canvas.insertAdjacentHTML('afterend',
            '<div class="alert alert-warning small mt-2">图表库未加载，请刷新页面重试。</div>');
        }
      }, 100);
      return;
    }

    var rows = window.BP_DATA || [];
    if (!rows.length) {
      canvas.insertAdjacentHTML('afterend',
        '<div class="text-center text-secondary py-5">暂无数据，点击右上角「＋ 记录」添加</div>');
      canvas.remove();
      return;
    }

    var times = rows.map(function (r) { return r.time || ''; });

    // 小屏适配：像素宽度决定 X 轴最多显示几个日期标签，避免标签重叠
    var axisWidth = canvas.clientWidth || (canvas.parentElement && canvas.parentElement.clientWidth) || 360;
    var maxTicks = Math.max(4, Math.min(10, Math.floor(axisWidth / 78)));
    // 数据点很多时只在悬停显示点，保持曲线干净
    var pointRadius = rows.length > 60 ? 0 : (rows.length > 30 ? 2 : 3);
    var isNarrow = axisWidth < 420;

    var chart = new Chart(canvas, {
      type: 'line',
      data: {
        labels: rows.map(function (r) { return r.date.slice(5); }),
        datasets: [
          {
            label: '收缩压', data: rows.map(function (r) { return r.systolic; }),
            yAxisID: 'y',
            borderColor: '#ffdd7a', backgroundColor: fillGradient(canvas, 'rgba(255,221,122,COLOR)'),
            pointBackgroundColor: '#ffdd7a', pointBorderColor: '#8a6516', borderWidth: 2.4,
            pointRadius: pointRadius, pointHoverRadius: 6, tension: 0.35, fill: true, spanGaps: true
          },
          {
            label: '舒张压', data: rows.map(function (r) { return r.diastolic; }),
            yAxisID: 'y',
            borderColor: '#7fe3c0', backgroundColor: fillGradient(canvas, 'rgba(127,227,192,COLOR)'),
            pointBackgroundColor: '#7fe3c0', pointBorderColor: '#1c6b52', borderWidth: 2.4,
            pointRadius: pointRadius, pointHoverRadius: 6, tension: 0.35, fill: true, spanGaps: true
          },
          {
            // 心率量级与血压不同，单独用右侧坐标轴，避免被压成一条平线
            label: '心率', data: rows.map(function (r) { return r.pulse; }),
            yAxisID: 'y1',
            borderColor: '#a99cff', pointBackgroundColor: '#a99cff', pointBorderColor: '#35276f',
            borderWidth: 2, pointRadius: pointRadius, pointHoverRadius: 6,
            borderDash: [5, 3], tension: 0.35, fill: false, spanGaps: true
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              title: function (items) {
                var i = items[0].dataIndex;
                return rows[i].date + (times[i] ? ' ' + times[i] : '');
              },
              label: function (item) {
                return item.dataset.label + '：' + item.formattedValue +
                  (item.datasetIndex === 2 ? ' 次/分' : ' mmHg');
              }
            }
          }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: {
              autoSkip: true,
              maxTicksLimit: maxTicks,
              maxRotation: 0,
              color: '#a8b0d8',
              font: { size: isNarrow ? 10 : 11 }
            }
          },
          y: {
            position: 'left',
            grid: { color: 'rgba(232,194,90,.10)' },
            ticks: { color: '#ffdd7a', font: { size: isNarrow ? 10 : 11 }, stepSize: 20 },
            title: {
              display: !isNarrow, text: '血压 mmHg', color: '#ffdd7a',
              font: { size: 10 }
            }
          },
          y1: {
            position: 'right',
            grid: { drawOnChartArea: false },
            // 心率轴范围与血压轴接近，避免两条轴的刻度错位造成误读
            suggestedMin: 40,
            suggestedMax: 160,
            ticks: {
              color: '#a99cff',
              font: { size: isNarrow ? 10 : 11 },
              stepSize: 20              // 与左轴同样的 20 步长，两轴刻度线对齐
            },
            title: {
              display: !isNarrow, text: '心率 次/分', color: '#a99cff',
              font: { size: 10 }
            }
          }
        }
      },
      plugins: [thresholdPlugin]
    });

    document.querySelectorAll('[data-series]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var idx = parseInt(btn.getAttribute('data-series'), 10);
        var visible = chart.isDatasetVisible(idx);
        chart.setDatasetVisibility(idx, !visible);
        btn.classList.toggle('active', !visible);
        chart.update();
      });
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

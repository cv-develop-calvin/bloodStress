/* 血压记录 · 前端交互：趋势曲线图、分级预览、离线缓存 */
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
      [{ v: 140, color: 'rgba(13,110,253,.45)', text: '收缩压 140' },
       { v: 90, color: 'rgba(25,135,84,.45)', text: '舒张压 90' }].forEach(function (t) {
        var py = y.getPixelForValue(t.v);
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
    if (typeof Chart === 'undefined') {
      canvas.insertAdjacentHTML('afterend',
        '<div class="alert alert-warning small mt-2">图表库未加载，请刷新页面重试。</div>');
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

    var chart = new Chart(canvas, {
      type: 'line',
      data: {
        labels: rows.map(function (r) { return r.date.slice(5); }),
        datasets: [
          {
            label: '收缩压', data: rows.map(function (r) { return r.systolic; }),
            borderColor: '#0d6efd', backgroundColor: fillGradient(canvas, 'rgba(13,110,253,COLOR)'),
            pointBackgroundColor: '#0d6efd', borderWidth: 2, pointRadius: 3,
            pointHoverRadius: 5, tension: 0.35, fill: true, spanGaps: true
          },
          {
            label: '舒张压', data: rows.map(function (r) { return r.diastolic; }),
            borderColor: '#198754', backgroundColor: fillGradient(canvas, 'rgba(25,135,84,COLOR)'),
            pointBackgroundColor: '#198754', borderWidth: 2, pointRadius: 3,
            pointHoverRadius: 5, tension: 0.35, fill: true, spanGaps: true
          },
          {
            label: '心率', data: rows.map(function (r) { return r.pulse; }),
            borderColor: '#fd7e14', pointBackgroundColor: '#fd7e14',
            borderWidth: 2, pointRadius: 3, pointHoverRadius: 5,
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
          x: { grid: { display: false }, ticks: { maxTicksLimit: 8, color: '#6c757d' } },
          y: { grid: { color: 'rgba(0,0,0,.06)' }, ticks: { color: '#6c757d' } }
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

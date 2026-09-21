/* 血常规表单：实时区间判定 + 把 OCR 文本发回后端解析并填入 */
(function () {
  'use strict';

  function td(text) { return String(text || ''); }

  function paint(input, badge) {
    var raw = input.value.trim();
    var low = parseFloat(input.dataset.low);
    var high = parseFloat(input.dataset.high);
    if (!raw) {
      badge.className = 'badge text-bg-light text-secondary';
      badge.textContent = '—';
      return;
    }
    var v = parseFloat(raw.replace(',', '.'));
    if (isNaN(v)) {
      badge.className = 'badge text-bg-light text-secondary';
      badge.textContent = '—';
      return;
    }
    if (v < low) {
      badge.className = 'badge text-bg-warning';
      badge.textContent = '偏低';
    } else if (v > high) {
      badge.className = 'badge text-bg-danger';
      badge.textContent = '偏高';
    } else {
      badge.className = 'badge text-bg-success';
      badge.textContent = '正常';
    }
  }

  document.querySelectorAll('.cbc-input').forEach(function (input) {
    var badge = document.getElementById('jd_' + input.id.slice(3));
    if (!badge) return;
    paint(input, badge);
    input.addEventListener('input', function () { paint(input, badge); });
  });

  /* ---------- OCR 文本解析 ---------- */
  function fillFromParsed(values) {
    var hit = 0;
    Object.keys(values || {}).forEach(function (key) {
      var input = document.getElementById('in_' + key);
      if (!input) return;
      input.value = values[key];
      var badge = document.getElementById('jd_' + key);
      if (badge) paint(input, badge);
      hit++;
      input.classList.add('bg-warning-subtle');
      setTimeout(function () { input.classList.remove('bg-warning-subtle'); }, 2500);
    });
    var box = document.getElementById('ocrBox');
    if (box) {
      box.classList.remove('d-none');
      box.textContent = hit
        ? '已从照片识别到 ' + hit + ' 项指标（黄色高亮），请核对后保存。'
        : '没有识别到可用指标，请手动填写或重拍更清晰的照片。';
    }
  }

  function parse(raw) {
    if (!raw || !raw.trim()) return;
    fetch('/lab/parse', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text: raw })
    })
      .then(function (r) { return r.json(); })
      .then(function (d) { fillFromParsed(d.values); })
      .catch(function () { /* 忽略 */ });
  }

  var raw = window.OCR_RAW || '';
  if (raw) parse(raw);

  var reparse = document.getElementById('reparse');
  if (reparse) {
    reparse.addEventListener('click', function () {
      var cached = sessionStorage.getItem('labOcrText');
      var text = (cached && cached.trim()) ? cached : raw;
      if (!text) {
        alert('没有可用的识别文本，请重新拍照识别。');
        return;
      }
      document.getElementById('rawText').value = text;
      parse(text);
    });
  }
})();

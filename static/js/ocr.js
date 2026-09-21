/* 化验单 OCR：优先使用在线 CDN 的 tesseract.js（chi_sim+eng）识别中文报告 */
(function () {
  'use strict';

  var camInput = document.getElementById('camInput');
  var fileInput = document.getElementById('fileInput');
  var preview = document.getElementById('preview');
  var previewBox = document.getElementById('previewBox');
  var bar = document.querySelector('#ocrProgress .progress-bar');
  var status = document.getElementById('ocrStatus');
  var textarea = document.getElementById('ocrText');
  var parseBtn = document.getElementById('parseBtn');
  var redoBtn = document.getElementById('redoBtn');

  var CDN_LIST = [
    'https://cdn.jsdelivr.net/npm/tesseract.js@5.1.0/dist/tesseract.min.js',
    'https://unpkg.com/tesseract.js@5.1.0/dist/tesseract.min.js'
  ];
  var CDN = CDN_LIST[0];
  var worker = null;

  function setStatus(msg, percent) {
    if (status) status.textContent = msg;
    if (bar && typeof percent === 'number') bar.style.width = percent + '%';
  }

  function loadScript(src) {
    return new Promise(function (resolve, reject) {
      var s = document.createElement('script');
      s.src = src;
      s.async = true;
      s.onload = resolve;
      s.onerror = function () { reject(new Error('load failed: ' + src)); };
      document.head.appendChild(s);
    });
  }

  async function ensureTesseract() {
    if (window.Tesseract) return true;
    for (var i = 0; i < CDN_LIST.length; i++) {
      try {
        await loadScript(CDN_LIST[i]);
        CDN = CDN_LIST[i];
        if (window.Tesseract) return true;
      } catch (e) { /* 尝试下一个 */ }
    }
    return false;
  }

  function showPreview(file) {
    var url = URL.createObjectURL(file);
    preview.src = url;
    previewBox.classList.remove('d-none');
    previewBox.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  async function runOcr(file) {
    showPreview(file);
    setStatus('正在加载识别引擎（首次需联网下载中文模型，约 10-20 秒）…', 5);

    var ok = await ensureTesseract();
    if (!ok) {
      setStatus('无法加载识别引擎（可能没有网络）。请手动录入，或连网后重试。', 0);
      textarea.value = '';
      parseBtn.disabled = true;
      return;
    }

    try {
      if (!worker) {
        worker = await Tesseract.createWorker('chi_sim+eng', 1, {
          workerPath: CDN.replace('tesseract.min.js', 'worker.min.js'),
          corePath: CDN.replace(/tesseract\.min\.js$/, ''),
          langPath: CDN.replace(/dist\/tesseract\.min\.js$/, 'lang/'),
          logger: function (m) {
            var p = Math.round((m.progress || 0) * 100);
            if (m.status === 'recognizing text') {
              setStatus('识别中… ' + p + '%', 40 + p * 0.6);
            } else {
              setStatus('初始化：' + m.status, 20);
            }
          }
        });
      }
      setStatus('识别中…', 40);
      var res = await worker.recognize(file);
      var text = (res && res.data && res.data.text) || '';
      textarea.value = text;
      sessionStorage.setItem('labOcrText', text);
      setStatus('识别完成，请核对下面文字后点击解析', 100);
      parseBtn.disabled = !text.trim();
    } catch (e) {
      setStatus('识别失败：' + (e && e.message ? e.message : e) + '，可手动录入。', 0);
      parseBtn.disabled = true;
    }
  }

  function onPick(event) {
    var file = event.target.files && event.target.files[0];
    if (!file) return;
    runOcr(file);
  }

  if (camInput) camInput.addEventListener('change', onPick);
  if (fileInput) fileInput.addEventListener('change', onPick);

  if (parseBtn) {
    parseBtn.addEventListener('click', function () {
      var text = textarea.value || '';
      sessionStorage.setItem('labOcrText', text);
      var form = document.createElement('form');
      form.method = 'post';
      form.action = '/lab/add';

      var fields = { date: '', time: '', hospital: '', note: '', source: 'photo', photo: '', raw_text: text };
      Object.keys(fields).forEach(function (k) {
        var i = document.createElement('input');
        i.type = 'hidden';
        i.name = k;
        i.value = fields[k];
        form.appendChild(i);
      });
      document.body.appendChild(form);
      form.submit();
    });
  }

  if (redoBtn) {
    redoBtn.addEventListener('click', function () {
      if (camInput) camInput.value = '';
      if (fileInput) fileInput.value = '';
      previewBox.classList.add('d-none');
      textarea.value = '';
      parseBtn.disabled = true;
      setStatus('准备中…', 0);
    });
  }
})();

/* 笔记表单：心情选择、字数统计、照片多选预览（带压缩后提交） */
(function () {
  'use strict';

  /* ---------- 心情选择 ---------- */
  var picker = document.getElementById('moodPicker');
  var moodInput = document.getElementById('mood');
  if (picker && moodInput) {
    picker.addEventListener('click', function (e) {
      var btn = e.target.closest('.mood-btn');
      if (!btn) return;
      var chosen = btn.getAttribute('data-mood');
      var isActive = btn.classList.contains('btn-gold');
      picker.querySelectorAll('.mood-btn').forEach(function (b) {
        b.classList.remove('btn-gold');
        b.classList.add('btn-outline-secondary');
      });
      if (isActive) {
        moodInput.value = '';
      } else {
        btn.classList.remove('btn-outline-secondary');
        btn.classList.add('btn-gold');
        moodInput.value = chosen;
      }
    });
  }

  /* ---------- 字数统计 ---------- */
  var content = document.getElementById('content');
  var counter = document.getElementById('charCount');
  if (content && counter) {
    var update = function () { counter.textContent = content.value.length + ' 字'; };
    content.addEventListener('input', update);
    update();
  }

  /* ---------- 照片选择与预览 ---------- */
  var camInput = document.getElementById('camInput');
  var fileInput = document.getElementById('fileInput');
  var photoInput = document.getElementById('photoInput');
  var grid = document.getElementById('previewGrid');
  var hint = document.getElementById('pickHint');
  if (!photoInput || !grid) return;

  var MAX = 9;
  var picked = [];            // File 对象数组

  function render() {
    grid.innerHTML = '';
    picked.forEach(function (file, idx) {
      var cell = document.createElement('div');
      cell.className = 'photo-cell';
      var img = document.createElement('img');
      img.src = URL.createObjectURL(file);
      img.alt = file.name;
      var del = document.createElement('button');
      del.type = 'button';
      del.className = 'btn btn-sm btn-danger photo-del';
      del.textContent = '×';
      del.setAttribute('aria-label', '移除');
      del.addEventListener('click', function () {
        picked.splice(idx, 1);
        render();
      });
      cell.appendChild(img);
      cell.appendChild(del);
      grid.appendChild(cell);
    });
    if (hint) hint.textContent = '已选 ' + picked.length + ' / ' + MAX + ' 张';
    syncInput();
  }

  // 把选中的文件写回真实 input，保证表单能提交
  function syncInput() {
    try {
      var dt = new DataTransfer();
      picked.forEach(function (f) { dt.items.add(f); });
      photoInput.files = dt.files;
    } catch (e) {
      // 少数旧浏览器不支持 DataTransfer，退回原生 input
    }
  }

  function addFiles(files) {
    Array.prototype.forEach.call(files || [], function (f) {
      if (picked.length >= MAX) {
        alert('最多选择 ' + MAX + ' 张照片');
        return;
      }
      if (!/^image\//.test(f.type)) return;
      picked.push(f);
    });
    render();
  }

  if (camInput) {
    camInput.addEventListener('change', function () {
      addFiles(camInput.files);
      camInput.value = '';
    });
  }
  if (fileInput) {
    fileInput.addEventListener('change', function () {
      addFiles(fileInput.files);
      fileInput.value = '';
    });
  }

  render();
})();

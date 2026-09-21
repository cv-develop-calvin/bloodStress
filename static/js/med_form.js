/* 药品表单：动态增减服药时间 */
(function () {
  'use strict';

  var list = document.getElementById('timeList');
  var btn = document.getElementById('addTime');
  if (!list || !btn) return;

  function addTime(value) {
    var wrap = document.createElement('div');
    wrap.className = 'input-group time-item';
    wrap.style.maxWidth = '160px';
    wrap.innerHTML =
      '<input type="time" class="form-control" name="times" value="' + (value || '08:00') + '">' +
      '<button type="button" class="btn btn-outline-danger" aria-label="删除">×</button>';
    wrap.querySelector('button').addEventListener('click', function () { wrap.remove(); });
    list.appendChild(wrap);
  }

  btn.addEventListener('click', function () { addTime(''); });

  var preset = (window.MED_TIMES || '').split(',').map(function (s) { return s.trim(); })
    .filter(Boolean);
  if (preset.length) {
    preset.forEach(addTime);
  } else {
    addTime('08:00');
  }
})();

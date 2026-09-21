/* 关于页：检查更新并展示 Release 信息与下载入口 */
(function () {
  'use strict';

  var btn = document.getElementById('checkUpdateBtn');
  var box = document.getElementById('updateResult');
  if (!btn || !box) return;

  function esc(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  function render(data) {
    var lines = [];
    lines.push('<div class="mb-2">当前版本：<span class="fw-semibold">v' + esc(data.current) + '</span></div>');

    if (data.has_update) {
      var rel = data.release;
      lines.push('<div class="alert alert-warning py-2 mb-2">' + esc(data.message) + '</div>');
      lines.push('<div class="mb-1">最新版本：<span class="fw-semibold">v' + esc(rel.tag) + '</span>' +
        (rel.published_at ? ' <span class="text-secondary">(' + esc(rel.published_at) + ')</span>' : '') + '</div>');
      if (rel.notes) {
        lines.push('<pre class="small bg-body-tertiary rounded p-2" style="white-space:pre-wrap;max-height:12rem;overflow:auto">' +
          esc(rel.notes).slice(0, 4000) + '</pre>');
      }
      var apk = (rel.assets || []).filter(function (a) {
        return /\.apk$/i.test(a.name || '');
      })[0];
      if (apk) {
        lines.push('<a class="btn btn-success btn-sm me-2" href="' + esc(apk.url) + '">下载 APK（' +
          Math.round((apk.size || 0) / 1048576 * 10) / 10 + ' MB）</a>');
      }
      if (rel.html_url) {
        lines.push('<a class="btn btn-outline-primary btn-sm" href="' + esc(rel.html_url) + '" target="_blank">查看 Release</a>');
      }
    } else {
      var cls = data.newer ? 'alert-info' : 'alert-success';
      lines.push('<div class="alert ' + cls + ' py-2 mb-2">' + esc(data.message) + '</div>');
      if (data.release && data.release.html_url) {
        lines.push('<a class="btn btn-outline-secondary btn-sm" href="' + esc(data.release.html_url) +
          '" target="_blank">在 GitHub 查看版本历史</a>');
      }
    }
    lines.push('<div class="mt-2 small text-secondary">仓库：' + esc(data.repo) + '</div>');
    box.innerHTML = lines.join('');
  }

  btn.addEventListener('click', function () {
    btn.disabled = true;
    btn.textContent = '检查中…';
    box.innerHTML = '<span class="text-secondary">正在连接 GitHub…</span>';
    fetch('/api/check_update', { cache: 'no-store' })
      .then(function (r) { return r.json(); })
      .then(render)
      .catch(function () {
        box.innerHTML = '<div class="alert alert-secondary py-2 mb-0">检查失败，请稍后重试。</div>';
      })
      .finally(function () {
        btn.disabled = false;
        btn.textContent = '检查更新';
      });
  });
})();

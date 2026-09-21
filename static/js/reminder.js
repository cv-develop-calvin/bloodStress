/* 用药提醒：轮询到点药、弹窗、响铃（WebAudio 合成铃声）、系统通知 */
(function () {
  'use strict';

  var seen = {};              // 已提醒过的 key，避免反复弹窗
  var notified = false;       // 是否至少提醒过一次（用于解锁音频）

  /* ---------- 铃声：WebAudio 合成，无需音频文件 ---------- */
  var ctx = null;
  function ensureCtx() {
    if (ctx) return ctx;
    var AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return null;
    ctx = new AC();
    return ctx;
  }

  function playRing(times) {
    var ac = ensureCtx();
    if (!ac) return;
    if (ac.state === 'suspended') ac.resume();
    var count = times || 2;
    for (var i = 0; i < count; i++) {
      beep(ac.currentTime + i * 0.6);
    }
  }

  function beep(t0) {
    // 双音「叮咚」
    [[880, t0, 0.18], [660, t0 + 0.22, 0.28]].forEach(function (n) {
      var osc = ctx.createOscillator();
      var gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.value = n[0];
      gain.gain.setValueAtTime(0.0001, n[1]);
      gain.gain.exponentialRampToValueAtTime(0.35, n[1] + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.0001, n[1] + n[2]);
      osc.connect(gain).connect(ctx.destination);
      osc.start(n[1]);
      osc.stop(n[1] + n[2] + 0.02);
    });
  }

  // 手机浏览器要求用户先有交互才能播放声音
  ['click', 'touchstart', 'keydown'].forEach(function (ev) {
    document.addEventListener(ev, function () {
      var ac = ensureCtx();
      if (ac && ac.state === 'suspended') ac.resume();
    }, { once: true, passive: true });
  });

  var testBtn = document.getElementById('testSound');
  if (testBtn) {
    testBtn.addEventListener('click', function () { playRing(2); notify('试听提醒铃声', '这是一次测试提醒'); });
  }
  if (navigator.vibrate) {
    var vibe = function () { try { navigator.vibrate([200, 100, 200]); } catch (e) { /* 忽略 */ } };
    testBtn && testBtn.addEventListener('click', vibe);
  }

  /* ---------- 系统通知 ---------- */
  function askPermission() {
    if (!('Notification' in window)) return;
    if (Notification.permission === 'default') {
      Notification.requestPermission().catch(function () { /* 忽略 */ });
    }
  }
  askPermission();

  function notify(title, body) {
    if (!('Notification' in window) || Notification.permission !== 'granted') return;
    try {
      new Notification(title, { body: body, icon: '/static/img/icon-192.png', tag: 'med-remind' });
    } catch (e) { /* 忽略 */ }
  }

  /* ---------- 弹窗 ---------- */
  var modalEl = document.getElementById('medRemindModal');
  var modal = (modalEl && window.bootstrap) ? new bootstrap.Modal(modalEl) : null;

  function showModal(items) {
    var body = document.getElementById('medRemindBody');
    var take = document.getElementById('medRemindTake');
    if (!body) return;
    var rows = items.map(function (it) {
      return '<div class="d-flex justify-content-between align-items-center py-1">' +
        '<span class="fw-semibold">' + it.name + '</span>' +
        '<span class="badge text-bg-warning">' + it.slot + '</span></div>' +
        '<div class="small text-secondary mb-2">' + (it.dosage || '') + (it.unit || '') + '</div>';
    }).join('');
    body.innerHTML = rows || '<p class="mb-0 text-secondary">该吃药啦</p>';
    if (take && items.length) {
      take.href = '/meds';
    }
    if (modal) modal.show();
  }

  /* ---------- 轮询后端 ---------- */
  function poll() {
    fetch('/api/pending', { cache: 'no-store' })
      .then(function (r) { return r.ok ? r.json() : []; })
      .then(function (items) {
        if (!items || !items.length) return;
        var fresh = items.filter(function (it) {
          var key = it.med_id + '@' + it.slot;
          if (seen[key]) return false;
          seen[key] = Date.now();
          return true;
        });
        if (!fresh.length) return;
        var text = fresh.map(function (it) { return it.name + ' ' + it.slot; }).join('、');
        playRing(3);
        if (navigator.vibrate) { try { navigator.vibrate([300, 150, 300]); } catch (e) { /* 忽略 */ } }
        notify('该吃药了 💊', text);
        showModal(fresh);
      })
      .catch(function () { /* 离线时忽略 */ });
  }

  var skipped = { index: true, meds: true, meds_add: true, meds_edit: true };
  if (!skipped[window.BP_PAGE]) {
    poll();
    setInterval(poll, 60000);   // 每分钟检查一次
  }
})();

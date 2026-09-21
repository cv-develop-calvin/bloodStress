/* 星座战士主题：图表配色统一、星辉动画 */
(function () {
  'use strict';

  // 让所有 Chart.js 图表使用主题色（在图表创建前生效）
  if (typeof Chart !== 'undefined') {
    Chart.defaults.color = '#a8b0d8';
    Chart.defaults.borderColor = 'rgba(232,194,90,.14)';
    Chart.defaults.font.family = 'system-ui, -apple-system, "Segoe UI", "PingFang SC", sans-serif';
  }

  // 首屏「圣衣」卡：金色边框轻微呼吸光效
  var hero = document.querySelector('.hero-card');
  if (hero && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    hero.animate(
      [
        { boxShadow: '0 12px 30px rgba(0,0,0,.55), inset 0 0 40px rgba(232,194,90,.10)' },
        { boxShadow: '0 12px 34px rgba(0,0,0,.55), inset 0 0 60px rgba(232,194,90,.22)' },
        { boxShadow: '0 12px 30px rgba(0,0,0,.55), inset 0 0 40px rgba(232,194,90,.10)' }
      ],
      { duration: 5200, iterations: Infinity, easing: 'ease-in-out' }
    );
  }
})();

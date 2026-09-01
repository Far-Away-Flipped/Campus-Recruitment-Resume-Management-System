<template>
  <div class="space-bg" aria-hidden="true">
    <!-- 移动端静态 CSS 星空（不跑 canvas，省性能） -->
    <div class="space-bg__stars-static" v-show="isMobile"></div>
    <!-- Canvas 粒子（桌面端 + 非 reduced-motion） -->
    <canvas v-show="canvasVisible" ref="canvasRef" class="space-bg__canvas"></canvas>
    <!-- 径向暗角 -->
    <div class="space-bg__vignette"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

// ---- 响应式画布状态 ----
let w = 0;
let h = 0;
let ctx = null;
let rafId = 0;
let stars = [];
let sats = [];

const STAR_COUNT = 600;
const SAT_COUNT = 30;
const STAR_COUNT_MIN = 200;
const SAT_COUNT_MIN = 12;
const LINK_DIST = 150;
// 主题色 #5FB8D6
const CYAN = '#5FB8D6';
const CYAN_ALPHA = 'rgba(95,184,214,';
const CYAN_LINK = 'rgba(95,184,214,0.2)';

// ---- 媒体查询 ----
const mqMobile = window.matchMedia('(max-width: 767px)');
const mqReduce = window.matchMedia('(prefers-reduced-motion: reduce)');
const isMobile = ref(mqMobile.matches);
const prefersReduced = ref(mqReduce.matches);
// canvas 仅在「非移动端」显示；reduced-motion 时不启动 rAF，只画一帧
const canvasVisible = ref(!mqMobile.matches);

const canvasRef = ref(null);

function seed(n) {
  stars = [];
  for (let i = 0; i < n; i++) {
    stars.push({
      x: Math.random() * w,
      y: Math.random() * h,
      size: 0.6 + Math.random() * 1.4,
      alpha: 0.35 + Math.random() * 0.65,
    });
  }
}

function seedSats(n) {
  sats = [];
  for (let i = 0; i < n; i++) {
    sats.push({
      x: Math.random() * w,
      y: Math.random() * h,
      vx: (Math.random() - 0.5) * 0.5,
      vy: (Math.random() - 0.5) * 0.5,
    });
  }
}

function drawStars() {
  for (const s of stars) {
    ctx.fillStyle = `rgba(255,255,255,${s.alpha})`;
    ctx.fillRect(s.x, s.y, s.size, s.size);
  }
}

function drawSats() {
  ctx.strokeStyle = CYAN_LINK;
  ctx.lineWidth = 0.5;
  for (let i = 0; i < sats.length; i++) {
    const a = sats[i];
    a.x += a.vx;
    a.y += a.vy;
    if (a.x < 0 || a.x > w) a.vx *= -1;
    if (a.y < 0 || a.y > h) a.vy *= -1;

    ctx.fillStyle = CYAN;
    ctx.beginPath();
    ctx.arc(a.x, a.y, 2, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = `${CYAN_ALPHA}0.15)`;
    ctx.beginPath();
    ctx.arc(a.x, a.y, 8, 0, Math.PI * 2);
    ctx.fill();

    for (let j = i + 1; j < sats.length; j++) {
      const b = sats[j];
      const dx = a.x - b.x;
      const dy = a.y - b.y;
      if (Math.sqrt(dx * dx + dy * dy) < LINK_DIST) {
        ctx.beginPath();
        ctx.moveTo(a.x, a.y);
        ctx.lineTo(b.x, b.y);
        ctx.stroke();
      }
    }
  }
}

function drawFrame() {
  if (!ctx) return;
  ctx.clearRect(0, 0, w, h);
  drawStars();
  drawSats();
}

function loop() {
  drawFrame();
  rafId = requestAnimationFrame(loop);
}

function resize() {
  if (!canvasRef.value) return;
  ctx = canvasRef.value.getContext('2d');
  w = canvasRef.value.width = window.innerWidth;
  h = canvasRef.value.height = window.innerHeight;
  const count = prefersReduced.value ? STAR_COUNT_MIN : STAR_COUNT;
  const satCount = prefersReduced.value ? SAT_COUNT_MIN : SAT_COUNT;
  seed(count);
  seedSats(satCount);
  // reduced-motion：只画一帧静态
  if (prefersReduced.value) drawFrame();
}

function stopLoop() {
  if (rafId) {
    cancelAnimationFrame(rafId);
    rafId = 0;
  }
}

function syncState() {
  const mobile = mqMobile.matches;
  isMobile.value = mobile;
  prefersReduced.value = mqReduce.matches;
  canvasVisible.value = !mobile;

  stopLoop();
  if (mobile) {
    // 移动端不渲染 canvas（v-show 已隐藏），无需画
    return;
  }
  resize();
  if (!prefersReduced.value) loop();
}

function onMobileChange() { syncState(); }
function onReduceChange() { syncState(); }

onMounted(() => {
  mqMobile.addEventListener('change', onMobileChange);
  mqReduce.addEventListener('change', onReduceChange);
  window.addEventListener('resize', resize);
  syncState();
});

onUnmounted(() => {
  mqMobile.removeEventListener('change', onMobileChange);
  mqReduce.removeEventListener('change', onReduceChange);
  window.removeEventListener('resize', resize);
  stopLoop();
});
</script>

<style scoped>
.space-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: var(--color-bg);
  overflow: hidden;
}
.space-bg__stars-static {
  position: absolute;
  inset: 0;
}
.space-bg__canvas {
  position: absolute;
  inset: 0;
}
.space-bg__vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at 50% 40%, transparent 45%, rgba(10, 14, 23, 0.65) 100%);
}
</style>

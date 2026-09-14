<template>
  <section class="dashboard-hero">
    <div class="dashboard-hero__main">
      <h1>个人中心</h1>
    </div>
    <div class="dashboard-hero__panel">
      <div class="hero-panel__label">
        快捷入口
      </div>
      <div
        v-if="isTeacher"
        class="hero-panel__item"
        @click="emit('navigate', '/teacher/review')"
      >
        <strong>待我审核</strong><span>学生申请处理 →</span>
      </div>
      <div
        v-else
        class="hero-panel__item"
        @click="emit('navigate', '/bookings')"
      >
        <strong>我的预约</strong><span>查看记录 →</span>
      </div>
      <div
        class="hero-panel__item"
        @click="emit('openQuick')"
      >
        <strong>快捷操作</strong><span>常用入口 →</span>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{ isTeacher: boolean }>()

const emit = defineEmits<{
  navigate: [to: string]
  openQuick: []
}>()
</script>

<style scoped lang="scss">
.dashboard-hero {
  position: relative; display: grid; grid-template-columns: 1.2fr 0.8fr; gap: 20px;
  padding: 32px; border-radius: 30px; color: #fff;
  background: linear-gradient(135deg, #0E6CD6, #3FB6FF 62%, #ADE2FF);
  box-shadow: var(--shadow-card); overflow: hidden;
}
.dashboard-hero::before {
  content:""; position:absolute; inset:0;
  background: radial-gradient(circle at 20% 20%, rgba(255,255,255,.16), transparent 22%),
              linear-gradient(120deg, transparent 14%, rgba(255,255,255,.08) 36%, transparent 62%);
}
.dashboard-hero::after {
  content:""; position:absolute; inset:auto -60px -60px auto;
  width:260px; height:260px; border-radius:50%;
  background: radial-gradient(circle, rgba(255,255,255,.18), rgba(255,255,255,0));
  animation: dashHalo 8s ease-in-out infinite; pointer-events:none;
}
.dashboard-hero__main, .dashboard-hero__panel { position:relative; z-index:1; }
.hero-chip { display:inline-flex; padding:5px 12px; border-radius:999px; font-size:12px; letter-spacing:.06em; background:rgba(255,255,255,.14); margin-bottom:14px; }
.dashboard-hero__main h1 { margin:12px 0 0; font-size:36px; line-height:1.18; }
.dashboard-hero__panel { display:grid; gap:12px; padding:22px; border-radius:22px; background:rgba(255,255,255,.1); border:1px solid rgba(255,255,255,.12); backdrop-filter:blur(10px); }
.hero-panel__label { font-size:13px; color:rgba(255,255,255,.64); margin-bottom:2px; }
.hero-panel__item { display:flex; justify-content:space-between; align-items:center; padding:10px 0; border-bottom:1px solid rgba(255,255,255,.1); cursor:pointer; }
.hero-panel__item:last-child { border-bottom:none; }
.hero-panel__item strong { font-size:14px; font-weight:600; }
.hero-panel__item span { font-size:12px; color:rgba(255,255,255,.6); }

@keyframes dashHalo { 0%,100%{ transform:translate3d(0,0,0) scale(1); } 50%{ transform:translate3d(-20px,-10px,0) scale(1.08); } }

@media (max-width: 900px) { .dashboard-hero { grid-template-columns: 1fr; } }
</style>

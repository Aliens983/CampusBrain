<script setup lang="ts">
/** 服务治理页顶部：标题、新增按钮、统计卡 */
defineProps<{ total: number; availableTotal: number }>()
const emit = defineEmits<{ (e: 'create'): void }>()
</script>

<template>
  <section class="admin-hero">
    <div class="admin-hero__main">
      <h1>服务治理</h1>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="emit('create')">新增服务</el-button>
      </div>
    </div>
    <div class="admin-hero__signal">
      <div class="signal-card">
        <span>服务总数</span><strong>{{ total }}</strong><small>已纳入统一管理</small>
      </div>
      <div class="signal-card">
        <span>可用服务</span><strong>{{ availableTotal }}</strong><small>当前可开放预约</small>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.admin-hero {
  position: relative;
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 20px;
  padding: 32px;
  border-radius: 30px;
  color: #fff;
  background: linear-gradient(135deg, #0f172a, #132949 55%, #3FB6FF);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.admin-hero::before {
  content: ""; position: absolute; inset: 0;
  background: radial-gradient(circle at 18% 20%, rgba(255, 255, 255, .12), transparent 18%),
              linear-gradient(140deg, transparent 14%, rgba(255, 255, 255, .08) 42%, transparent 72%);
}
.admin-hero::after {
  content: ""; position: absolute; inset: -30% -6% auto auto; width: 280px; height: 280px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(123, 208, 255, .24), rgba(123, 208, 255, 0));
  animation: adminGlow 8s ease-in-out infinite; pointer-events: none;
}
.admin-hero__main, .admin-hero__signal { position: relative; z-index: 1; }
.admin-hero h1 { margin: 12px 0 10px; font-size: 36px; line-height: 1.18; }
.hero-actions { display: flex; gap: 12px; margin-top: 22px; }
.admin-hero__signal { display: grid; gap: 12px; }
.signal-card {
  display: grid; gap: 4px; padding: 16px 18px; border-radius: 16px;
  background: rgba(255, 255, 255, .08); border: 1px solid rgba(255, 255, 255, .1);
  cursor: pointer; transition: background .2s;
}
.signal-card:hover { background: rgba(255, 255, 255, .14); }
.signal-card span { font-size: 13px; color: rgba(255, 255, 255, .64); }
.signal-card strong { font-size: 26px; font-weight: 700; }
.signal-card small { font-size: 12px; color: rgba(255, 255, 255, .5); }
@keyframes adminGlow {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(-16px, -8px, 0) scale(1.06); }
}
@media (max-width: 960px) { .admin-hero { grid-template-columns: 1fr; } }
</style>

<template>
  <section class="dashboard-hero">
    <div class="dashboard-hero__main">
      <h1>工作台</h1>
      <el-carousel
        v-if="banners.length"
        ref="carouselRef"
        :interval="3000"
        arrow="hover"
        indicator-position="none"
        class="hero-banner"
        @mousedown="onBannerDown"
        @mouseup="onBannerUp"
        @mouseleave="clearBannerDrag"
      >
        <el-carousel-item
          v-for="(img, i) in banners"
          :key="i"
        >
          <img
            :src="img"
            class="hero-banner__img"
            draggable="false"
            alt="校园轮播"
          >
        </el-carousel-item>
      </el-carousel>
    </div>

    <div class="dashboard-hero__panel">
      <div class="hero-panel__label">
        今日安排
      </div>
      <div
        v-for="item in todayBookings"
        :key="item.id"
        class="hero-panel__item"
        @click="emit('open-booking', item.id)"
      >
        <strong>{{ item.timeRange }}</strong>
        <span>{{ item.serviceName }} / {{ item.location }}</span>
      </div>
      <div
        v-if="todayBookings.length === 0"
        class="hero-panel__item"
      >
        <strong>暂无安排</strong>
        <span>今天没有预约事务</span>
      </div>
      <div class="hero-actions panel-actions">
        <el-button
          type="primary"
          size="large"
          @click="emit('navigate', '/services')"
        >
          发起预约
        </el-button>
        <el-button
          size="large"
          class="hero-action-ghost"
          @click="emit('navigate', '/bookings')"
        >
          查看我的预约
        </el-button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { BookingRecord } from '@/common/types'

defineProps<{
  banners: string[]
  todayBookings: BookingRecord[]
}>()

const emit = defineEmits<{
  (e: 'navigate', path: string): void
  (e: 'open-booking', id: number): void
}>()

const carouselRef = ref<any>(null)
let dragStartX: number | null = null
function onBannerDown(e: MouseEvent) {
  dragStartX = e.clientX
}
function onBannerUp(e: MouseEvent) {
  if (dragStartX === null) return
  const dx = e.clientX - dragStartX
  dragStartX = null
  const el = carouselRef.value
  if (!el) return
  if (dx < -50) el.next()   // 向左拖 → 下一张
  else if (dx > 50) el.prev()  // 向右拖 → 上一张
}
function clearBannerDrag() {
  dragStartX = null
}
</script>

<style scoped lang="scss">
.dashboard-hero {
  position: relative;
  display: grid;
  grid-template-columns: 1.35fr 0.65fr;
  gap: 24px;
  padding: 32px;
  border-radius: 30px;
  color: #fff;
  background: linear-gradient(135deg, #0E6CD6, #3FB6FF 62%, #ADE2FF);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.dashboard-hero__main {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
}
.hero-banner {
  flex: 1;
  min-height: 220px;
  width: 100%;
  cursor: grab;
  user-select: none;
  -webkit-user-select: none;
}
.hero-banner :deep(.el-carousel__container),
.hero-banner :deep(.el-carousel-item) {
  height: 100%;
}

.dashboard-hero::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 20%, rgba(255, 255, 255, 0.16), transparent 22%),
    linear-gradient(120deg, transparent 14%, rgba(255, 255, 255, 0.08) 36%, transparent 62%);
}

.dashboard-hero::after {
  content: "";
  position: absolute;
  inset: auto -80px -80px auto;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.22), rgba(255, 255, 255, 0));
  animation: dashboardHalo 8s ease-in-out infinite;
}

.dashboard-hero__main,
.dashboard-hero__panel {
  position: relative;
  z-index: 1;
}

.hero-chip {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.08em;
  background: rgba(255, 255, 255, 0.14);
}

.dashboard-hero__main h1 {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.3;
  margin: 12px 0 10px;
}

.dashboard-hero__main p {
  font-size: 14px;
  opacity: 0.85;
  line-height: 1.6;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.hero-banner {
  border-radius: 16px;
  overflow: hidden;
  margin-top: 14px;
  box-shadow: 0 10px 24px rgba(7, 41, 102, 0.25);
}
.hero-banner__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
  transform: scale(1.08); /* 长图取中间部分放大填满 */
}
.panel-actions {
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  margin-top: 14px;
}
.panel-actions .el-button {
  margin-left: 0;
  width: 100%;
}

/* 次要按钮：玻璃白字，适配深色 hero（避免无 type 按钮白字压浅底的"看不清"） */
.hero-actions .el-button.hero-action-ghost {
  --el-button-text-color: #fff;
  --el-button-hover-text-color: #fff;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.45);
  box-shadow: none;
}
.hero-actions .el-button.hero-action-ghost:hover {
  background: rgba(255, 255, 255, 0.28);
  border-color: rgba(255, 255, 255, 0.7);
}

.dashboard-hero__panel {
  display: grid;
  gap: 12px;
  padding: 24px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.08);
}

.hero-panel__label {
  font-size: 13px;
  opacity: 0.7;
  margin-bottom: 4px;
}

.hero-panel__item {
  padding: 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.06);
  cursor: pointer;
  transition: background 0.2s;
}

.hero-panel__item:hover {
  background: rgba(255, 255, 255, 0.12);
}

.hero-panel__item strong {
  display: block;
  font-size: 14px;
  margin-bottom: 4px;
}

.hero-panel__item span {
  font-size: 12px;
  opacity: 0.7;
}

@keyframes dashboardHalo {
  0%,100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-20px, 20px) scale(1.08); }
}
</style>

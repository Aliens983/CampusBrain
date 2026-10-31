<template>
  <section class="dash-hero">
    <div class="dash-hero__copy">
      <p class="dash-hero__eyebrow">
        HZNU · 今日校园
      </p>
      <h1>
        在校园里，<br>
        <em>预约一段好时光</em>
      </h1>
      <p class="dash-hero__lede">
        仓前与下沙的教室、设备、咨询与活动，都在这里等你。
      </p>

      <div class="hero-actions">
        <el-button
          type="primary"
          size="large"
          @click="emit('navigate', '/services')"
        >
          发起预约
        </el-button>
        <el-button
          size="large"
          class="hero-ghost"
          @click="emit('navigate', '/bookings')"
        >
          查看我的预约
        </el-button>
      </div>

      <div class="dash-hero__meta">
        <span class="dash-hero__motto">勤慎诚恕 · 博雅精进</span>
        <span class="dash-hero__date">{{ dateLabel }}</span>
      </div>
    </div>

    <div class="dash-hero__visual">
      <div class="hero-media">
        <el-carousel
          v-if="banners.length"
          ref="carouselRef"
          :interval="4500"
          arrow="hover"
          indicator-position="none"
          class="hero-carousel"
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
              class="hero-photo"
              draggable="false"
              alt="校园轮播"
            >
          </el-carousel-item>
        </el-carousel>
        <img
          v-else
          :src="cqImg"
          class="hero-photo"
          alt="杭州师范大学仓前校区校门"
          draggable="false"
        >
        <div class="hero-media__veil" />
        <span class="hero-media__caption">校园影像 · HZNU</span>
      </div>

      <div class="schedule-card">
        <div class="schedule-card__head">
          <span class="schedule-card__label">今日安排</span>
          <span class="schedule-card__date">{{ dateShort }}</span>
        </div>
        <ul class="schedule-list">
          <li
            v-for="item in todayBookings"
            :key="item.id"
            class="schedule-item"
            @click="emit('open-booking', item.id)"
          >
            <span class="schedule-item__time">{{ item.timeRange || '全天' }}</span>
            <span class="schedule-item__main">
              <strong>{{ item.serviceName }}</strong>
              <small>{{ item.location || '校园内' }}</small>
            </span>
            <span class="schedule-item__arrow">→</span>
          </li>
          <li
            v-if="todayBookings.length === 0"
            class="schedule-empty"
          >
            <strong>今日无约</strong>
            <small>去服务中心看看，或留一点时间给自己。</small>
          </li>
        </ul>
        <div class="schedule-card__foot">
          <span>HZNU</span>
          <span>仓前 · 下沙</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { CarouselInstance } from 'element-plus'
import { computed, ref } from 'vue'
import type { BookingRecord } from '@/common/types'
import cqImg from '@/assets/images/campus/cq-hero.jpg'

defineProps<{
  banners: string[]
  todayBookings: BookingRecord[]
}>()

const emit = defineEmits<{
  (e: 'navigate', path: string): void
  (e: 'open-booking', id: number): void
}>()

const now = new Date()
const dateLabel = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long',
}).format(now)
const dateShort = computed(() => {
  const d = new Date()
  return `${d.getMonth() + 1}.${String(d.getDate()).padStart(2, '0')}`
})

const carouselRef = ref<CarouselInstance | null>(null)
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
  if (dx < -50) el.next()
  else if (dx > 50) el.prev()
}
function clearBannerDrag() {
  dragStartX = null
}
</script>

<style scoped lang="scss">
.dash-hero {
  position: relative;
  display: grid;
  grid-template-columns: 1.06fr 0.94fr;
  border: 1px solid var(--line);
  border-radius: var(--radius-2xl);
  background: var(--surface);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  animation: heroIn 0.36s var(--ease-out) both;
}

@keyframes heroIn {
  from { transform: translateY(8px); }
  to { transform: translateY(0); }
}

.dash-hero__copy {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 46px 48px;
}

.dash-hero__eyebrow {
  margin: 0 0 18px;
  font-family: var(--font-latin);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.24em;
  text-transform: uppercase;
  color: var(--gold);
}

.dash-hero__copy h1 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: clamp(36px, 3.5vw, 52px);
  font-weight: 600;
  line-height: 1.26;
  letter-spacing: 0.03em;
  color: var(--ink);
}

.dash-hero__copy h1 em {
  position: relative;
  font-style: normal;
  color: var(--lake);
  z-index: 1;
}

.dash-hero__copy h1 em::after {
  content: "";
  position: absolute;
  left: -2px;
  right: -4px;
  bottom: 4px;
  height: 10px;
  background: var(--gold-faint);
  z-index: -1;
}

.dash-hero__lede {
  max-width: 400px;
  margin: 18px 0 0;
  color: var(--ink-3);
  font-size: 13.5px;
  line-height: 1.9;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 30px;
}

.hero-ghost {
  background: transparent;
  border-color: var(--line-strong);
  color: var(--ink-2);
}

.hero-ghost:hover {
  border-color: rgba(176, 138, 69, 0.5);
  color: var(--lake);
}

.dash-hero__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 38px;
  padding-top: 16px;
  border-top: 1px solid var(--line);
}

.dash-hero__motto {
  font-family: var(--font-serif);
  font-size: 11.5px;
  letter-spacing: 0.16em;
  color: var(--ink-3);
}

.dash-hero__date {
  font-family: var(--font-latin);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--ink-4);
  white-space: nowrap;
}

.dash-hero__visual {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  padding: 28px 28px 28px 0;
}

.hero-media {
  position: relative;
  flex-shrink: 0;
  height: 240px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface-2);
}

.hero-photo {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 52%;
  filter: saturate(0.82) contrast(1.02);
}

.hero-carousel,
.hero-carousel :deep(.el-carousel__container),
.hero-carousel :deep(.el-carousel__item) {
  height: 100%;
}

.hero-carousel :deep(.el-carousel__arrow) {
  background: rgba(24, 49, 59, 0.42);
  color: #f7f1e4;
}

.hero-media__veil {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(90deg, rgba(24, 49, 59, 0.2), transparent 42%),
    linear-gradient(0deg, rgba(24, 49, 59, 0.48), transparent 55%);
}

.hero-media__caption {
  position: absolute;
  top: 24px;
  left: 26px;
  display: inline-flex;
  align-items: center;
  gap: 9px;
  padding: 6px 11px;
  border: 1px solid rgba(247, 241, 228, 0.32);
  border-radius: 3px;
  background: rgba(24, 49, 59, 0.3);
  backdrop-filter: blur(8px);
  color: rgba(247, 241, 228, 0.9);
  font-family: var(--font-serif);
  font-size: 10.5px;
  letter-spacing: 0.14em;
}

@media (max-width: 620px) {
  .hero-media__caption {
    display: none;
  }
}

.schedule-card {
  position: relative;
  width: 100%;
  padding: 16px 18px 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface-2);
  box-shadow: var(--shadow-soft);
}

.schedule-card__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
}

.schedule-card__label {
  font-family: var(--font-serif);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--ink);
}

.schedule-card__date {
  font-family: var(--font-latin);
  font-size: 9.5px;
  letter-spacing: 0.12em;
  color: var(--gold);
}

.schedule-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.schedule-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-top: 1px solid var(--line);
  cursor: pointer;
  transition: transform 0.16s var(--ease-out);
}

.schedule-item:hover {
  transform: translateX(3px);
}

.schedule-item__time {
  min-width: 62px;
  font-family: var(--font-latin);
  font-size: 10.5px;
  letter-spacing: 0.04em;
  color: var(--lake);
  white-space: nowrap;
}

.schedule-item__main {
  display: grid;
  flex: 1;
  min-width: 0;
  line-height: 1.45;
}

.schedule-item__main strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink);
}

.schedule-item__main small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 2px;
  font-size: 10.5px;
  color: var(--ink-4);
}

.schedule-item__arrow {
  font-family: var(--font-latin);
  font-size: 13px;
  color: var(--gold);
  opacity: 0;
  transform: translateX(-4px);
  transition: opacity 0.16s ease, transform 0.16s var(--ease-out);
}

.schedule-item:hover .schedule-item__arrow {
  opacity: 1;
  transform: translateX(0);
}

.schedule-empty {
  display: grid;
  gap: 4px;
  padding: 18px 0 14px;
  border-top: 1px solid var(--line);
}

.schedule-empty strong {
  font-family: var(--font-serif);
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-2);
}

.schedule-empty small {
  font-size: 11px;
  line-height: 1.7;
  color: var(--ink-4);
}

.schedule-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  padding-top: 10px;
  border-top: 1px dashed var(--line);
  font-family: var(--font-latin);
  font-size: 8.5px;
  letter-spacing: 0.16em;
  color: var(--ink-4);
}

@media (max-width: 1100px) {
  .dash-hero {
    grid-template-columns: 1fr;
  }

  .dash-hero__copy {
    padding: 38px 36px 30px;
  }

  .dash-hero__visual {
    padding: 0 28px 28px;
  }

  .hero-media {
    height: 280px;
  }
}

@media (max-width: 700px) {
  .dash-hero__copy {
    padding: 28px 24px 24px;
  }

  .dash-hero__copy h1 {
    font-size: 34px;
  }

  .hero-actions {
    margin-top: 24px;
  }

  .hero-actions :deep(.el-button) {
    flex: 1 1 150px;
    margin-left: 0;
  }

  .dash-hero__meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
    margin-top: 26px;
  }

  .dash-hero__visual {
    padding: 0 18px 18px;
  }

  .hero-media {
    height: 220px;
  }
}
</style>

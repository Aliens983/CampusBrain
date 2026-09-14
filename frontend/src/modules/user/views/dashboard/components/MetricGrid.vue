<template>
  <section class="metric-grid">
    <article
      v-for="stat in stats"
      :key="stat.label"
      class="metric-card"
      @click="emit('select', stat.label)"
    >
      <span class="metric-card__label">{{ stat.label }}</span>
      <strong class="metric-card__value">{{ stat.value }}</strong>
      <span
        class="metric-card__trend status-pill"
        :class="toneClass(stat.tone)"
      >{{ stat.trend }}</span>
    </article>
  </section>
</template>

<script setup lang="ts">
import type { DashboardStat } from '@/common/types'

defineProps<{
  stats: DashboardStat[]
}>()

const emit = defineEmits<{
  (e: 'select', label: string): void
}>()

function toneClass(tone: DashboardStat['tone']) {
  return { brand: 'is-brand', success: 'is-success', warning: 'is-warning', danger: 'is-danger' }[tone]
}
</script>

<style scoped lang="scss">
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.metric-card {
  position: relative;
  display: grid;
  gap: 8px;
  padding: 22px;
  border-radius: 22px;
  background: rgba(255,255,255,.92);
  border: 1px solid var(--border-soft);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  cursor: pointer;
  transition: transform .26s ease, box-shadow .26s ease;
}

.metric-card::after {
  content: '';
  position: absolute;
  inset: auto -16px -16px auto;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(30, 152, 242, 0.06), rgba(30, 152, 242, 0));
}

.metric-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-card-hover);
}

.metric-card__label {
  font-size: 13px;
  color: var(--text-secondary);
}

.metric-card__value {
  font-size: 34px;
  font-weight: 700;
  color: var(--text-primary);
}

.metric-card__trend {
  font-size: 12px;
  width: fit-content;
}
</style>

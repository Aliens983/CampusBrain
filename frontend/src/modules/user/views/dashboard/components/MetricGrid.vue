<template>
  <section class="metric-grid">
    <article
      v-for="(stat, index) in stats"
      :key="stat.label"
      class="metric-card"
      @click="emit('select', stat.label)"
    >
      <span class="metric-card__index">{{ String(index + 1).padStart(2, '0') }}</span>
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
  gap: 6px;
  padding: 22px 22px 20px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  background: var(--surface);
  box-shadow: var(--shadow-card);
  cursor: pointer;
  overflow: hidden;
  transition:
    transform 0.2s var(--ease-out),
    box-shadow 0.2s var(--ease-out),
    border-color 0.2s ease;
}

.metric-card::after {
  content: "";
  position: absolute;
  left: 22px;
  bottom: 0;
  width: 32px;
  height: 2px;
  background: var(--gold);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.24s var(--ease-out);
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card-hover);
  border-color: rgba(176, 138, 69, 0.34);
}

.metric-card:hover::after {
  transform: scaleX(1);
}

.metric-card__index {
  position: absolute;
  top: 20px;
  right: 20px;
  font-family: var(--font-latin);
  font-size: 9px;
  letter-spacing: 0.12em;
  color: var(--gold);
  opacity: 0.72;
}

.metric-card__label {
  font-size: 12px;
  letter-spacing: 0.04em;
  color: var(--ink-3);
}

.metric-card__value {
  font-family: var(--font-serif);
  font-size: 36px;
  font-weight: 600;
  line-height: 1.15;
  letter-spacing: 0.01em;
  color: var(--ink);
}

.metric-card__trend {
  width: fit-content;
  margin-top: 4px;
  font-size: 10.5px;
}

@media (max-width: 1100px) {
  .metric-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 520px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>

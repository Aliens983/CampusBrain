<template>
  <el-card
    class="surface-card"
    shadow="never"
  >
    <template #header>
      <div class="card-head">
        <div class="card-head__title">
          <span class="card-head__index">壹</span>
          <div>
            <h3>快捷入口</h3>
            <p>常用操作一键直达，无需在菜单中逐级寻找。</p>
          </div>
        </div>
        <span class="card-head__en">Quick access</span>
      </div>
    </template>
    <div class="shortcut-grid">
      <button
        v-for="item in shortcuts"
        :key="item.title"
        class="shortcut-card"
        @click="emit('navigate', item)"
      >
        <span
          class="shortcut-card__icon"
          :class="item.tone"
        >
          <el-icon><component :is="item.icon" /></el-icon>
        </span>
        <span class="shortcut-card__copy">
          <strong>{{ item.title }}</strong>
          <small>{{ item.desc }}</small>
        </span>
        <span class="shortcut-card__arrow">→</span>
      </button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { DashboardShortcut } from '../composables'

defineProps<{
  shortcuts: DashboardShortcut[]
}>()

const emit = defineEmits<{
  (e: 'navigate', item: DashboardShortcut): void
}>()
</script>

<style scoped lang="scss">
.surface-card {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.card-head__title {
  display: flex;
  align-items: flex-start;
  gap: 11px;
}

.card-head__index {
  padding-top: 3px;
  font-family: var(--font-serif);
  font-size: 11px;
  color: var(--gold);
}

.card-head h3 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 600;
  color: var(--ink);
}

.card-head p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--ink-3);
}

.card-head__en {
  padding-top: 4px;
  font-family: var(--font-latin);
  font-size: 8px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--ink-4);
  white-space: nowrap;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.shortcut-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px 14px;
  border-radius: 10px;
  border: 1px solid var(--line);
  background: var(--surface-2);
  font-family: var(--font-sans);
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.18s var(--ease-out),
    border-color 0.18s ease,
    background-color 0.18s ease;
}

.shortcut-card:hover {
  transform: translateY(-2px);
  border-color: rgba(176, 138, 69, 0.42);
  background: var(--surface);
}

.shortcut-card__icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 8px;
  font-size: 18px;
  line-height: 1;
  flex-shrink: 0;
}

.shortcut-card__icon :deep(.el-icon) {
  font-size: 18px;
}

.tone-blue { background: rgba(63, 111, 125, 0.1); color: var(--lake); }
.tone-teal { background: rgba(95, 135, 118, 0.12); color: var(--celadon-deep); }
.tone-amber { background: rgba(176, 138, 69, 0.13); color: var(--warning-500); }
.tone-slate { background: rgba(66, 85, 92, 0.1); color: #42555c; }

.shortcut-card__copy {
  display: grid;
  min-width: 0;
  line-height: 1.4;
}

.shortcut-card__copy strong {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink);
}

.shortcut-card__copy small {
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 10.5px;
  color: var(--ink-4);
}

.shortcut-card__arrow {
  margin-left: auto;
  font-family: var(--font-latin);
  font-size: 12px;
  color: var(--gold);
  opacity: 0;
  transform: translateX(-4px);
  transition: opacity 0.16s ease, transform 0.16s var(--ease-out);
}

.shortcut-card:hover .shortcut-card__arrow {
  opacity: 1;
  transform: translateX(0);
}

@media (max-width: 1100px) {
  .shortcut-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 520px) {
  .shortcut-grid {
    grid-template-columns: 1fr;
  }

  .card-head__en {
    display: none;
  }
}
</style>

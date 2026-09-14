<template>
  <el-card class="surface-card">
    <template #header>
      <div class="card-head">
        <div>
          <h3>快捷入口</h3>
          <p>常用操作一键直达，无需在菜单中逐级寻找。</p>
        </div>
      </div>
    </template>
    <div class="shortcut-grid">
      <button
        v-for="item in shortcuts"
        :key="item.title"
        class="shortcut-card"
        @click="emit('navigate', item)"
      >
        <div class="shortcut-card__orb" />
        <div
          class="shortcut-card__icon"
          :class="item.tone"
        >
          {{ item.icon }}
        </div>
        <strong>{{ item.title }}</strong>
        <span>{{ item.desc }}</span>
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
  border-radius: 24px;
  box-shadow: var(--shadow-card);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-head h3 {
  font-size: 17px;
  font-weight: 600;
}

.card-head p {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.shortcut-card {
  position: relative;
  display: grid;
  gap: 8px;
  padding: 18px 14px;
  border-radius: 18px;
  border: 1px solid var(--border-soft);
  background: linear-gradient(180deg, #fff, #F9FCFF);
  cursor: pointer;
  transition: transform .24s ease, box-shadow .24s ease;
  overflow: hidden;
}

.shortcut-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 28px rgba(20,33,61,.1);
}

.shortcut-card__orb {
  position: absolute;
  top: -12px;
  right: -12px;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(63,182,255,.06), transparent);
}

.shortcut-card__icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 19px;
  line-height: 1;
}

.tone-blue { background: linear-gradient(135deg, #EDF5FF, #E1ECFE); color: #0284c7; }
.tone-teal { background: linear-gradient(135deg, #ccfbf1, #99f6e4); color: #0d9488; }
.tone-amber { background: linear-gradient(135deg, #fef3c7, #fde68a); color: #d97706; }
.tone-slate { background: linear-gradient(135deg, #f1f5f9, #e2e8f0); color: #475569; }

.shortcut-card strong {
  font-size: 13px;
  font-weight: 600;
}

.shortcut-card span {
  font-size: 11px;
  color: var(--text-secondary);
}
</style>

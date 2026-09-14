<template>
  <el-card class="surface-card">
    <template #header>
      <div class="card-head">
        <div>
          <h3>待处理反馈</h3>
          <p>今天最值得优先关注的事项。</p>
        </div>
      </div>
    </template>
    <div class="todo-stack">
      <div
        v-for="item in items"
        :key="item.title"
        class="todo-card"
        @click="emit('navigate', item.path)"
      >
        <div class="todo-card__main">
          <strong>{{ item.title }}</strong>
          <p>{{ item.desc }}</p>
        </div>
        <span
          class="todo-card__badge status-pill"
          :class="item.tone"
        >{{ item.badge }}</span>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { DashboardTodo } from '../composables'

defineProps<{
  items: DashboardTodo[]
}>()

const emit = defineEmits<{
  (e: 'navigate', path: string): void
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

.todo-stack {
  display: grid;
  gap: 12px;
}

.todo-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid var(--border-soft);
  background: linear-gradient(180deg, #fff, #F9FCFF);
  cursor: pointer;
  transition: transform .2s ease;
}

.todo-card:hover {
  transform: translateX(4px);
}

.todo-card__main {
  flex: 1;
}

.todo-card__main strong {
  font-size: 13px;
  font-weight: 600;
}

.todo-card__main p {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.todo-card__badge {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 999px;
}
</style>

<template>
  <el-card
    class="surface-card"
    shadow="never"
  >
    <template #header>
      <div class="card-head">
        <div class="card-head__title">
          <span class="card-head__index">叁</span>
          <div>
            <h3>待处理反馈</h3>
            <p>今天最值得优先关注的事项。</p>
          </div>
        </div>
        <span class="card-head__en">To-do</span>
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
}

.todo-stack {
  display: grid;
}

.todo-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 15px 0;
  border-top: 1px solid var(--line);
  cursor: pointer;
  transition: transform 0.16s var(--ease-out);
}

.todo-card:first-child {
  border-top: 0;
  padding-top: 4px;
}

.todo-card:hover {
  transform: translateX(3px);
}

.todo-card__main {
  flex: 1;
  min-width: 0;
}

.todo-card__main strong {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink);
}

.todo-card__main p {
  margin-top: 3px;
  font-size: 11px;
  line-height: 1.65;
  color: var(--ink-4);
}

.todo-card__badge {
  flex-shrink: 0;
  font-size: 10px;
  padding: 3px 8px;
}
</style>

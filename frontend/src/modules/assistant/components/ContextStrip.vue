<script setup lang="ts">
/** 多轮上下文提示条：展示 AI 当前记住的预约条件 */
defineProps<{ chips: Array<{ key: string; text: string }> }>()
const emit = defineEmits<{ (e: 'reset'): void }>()
</script>

<template>
  <div v-if="chips.length" class="ctx-strip">
    <span class="ctx-strip__label">本次对话已记住</span>
    <el-tag v-for="chip in chips" :key="chip.key" size="small" effect="plain" type="info">
      {{ chip.text }}
    </el-tag>
    <el-button link size="small" class="ctx-strip__reset" @click="emit('reset')">清空记忆</el-button>
  </div>
</template>

<style scoped>
.ctx-strip {
  flex-shrink: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 9px 24px;
  border-bottom: 1px solid rgba(37, 99, 155, .09);
  background: rgba(240, 249, 255, .72);
}
.ctx-strip__label { color: var(--text-tertiary); font-size: 12px; }
.ctx-strip__reset { margin-left: auto; color: var(--text-tertiary); }
</style>

<script setup lang="ts">
/**
 * 输入区：问题输入 + 发送 + 反馈按钮。
 * 反馈必须带 Authorization，已由父组件经 fetchRaw 统一处理（此处只上报意图）。
 */
const props = defineProps<{
  /** 输入内容，双向绑定 */
  modelValue: string
  streaming: boolean
  canFeedback: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'send'): void
  (e: 'feedback', type: 'like' | 'dislike'): void
}>()
</script>

<template>
  <footer class="chat-composer">
    <div class="qa-input">
      <el-input
        :model-value="props.modelValue"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 5 }"
        resize="none"
        placeholder="输入你的问题，按 Enter 发送"
        :disabled="props.streaming"
        clearable
        @update:model-value="emit('update:modelValue', $event as string)"
        @keydown.enter.exact.prevent="emit('send')"
      />
      <el-button
        type="primary"
        class="qa-input__btn"
        :loading="props.streaming"
        @click="emit('send')"
      >
        {{ props.streaming ? '生成中…' : '发送' }}
      </el-button>
    </div>
    <div class="composer-hint">
      <span>回答由知识库与实时数据生成，请核对重要信息</span>
      <template v-if="props.canFeedback">
        <el-button text size="small" @click="emit('feedback', 'like')">有帮助</el-button>
        <el-button text size="small" @click="emit('feedback', 'dislike')">没帮助</el-button>
      </template>
    </div>
  </footer>
</template>

<style scoped>
.chat-composer {
  flex-shrink: 0;
  padding: 16px clamp(18px, 7vw, 110px) 18px;
  border-top: 1px solid rgba(37, 99, 155, .09);
  background: rgba(255, 255, 255, .96);
}
.qa-input { display: flex; gap: 12px; }
.qa-input .el-input { flex: 1; }
.qa-input .el-textarea :deep(.el-textarea__inner) {
  min-height: 44px !important;
  padding: 12px 14px;
  border-radius: 12px;
  box-shadow: 0 0 0 1px rgba(63, 182, 255, .16) inset;
}
.qa-input__btn {
  flex-shrink: 0;
  min-width: 96px;
  margin: 0;
  border-radius: 12px;
  font-weight: 600;
}
.composer-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 7px;
  color: var(--text-tertiary);
  font-size: 11px;
}
.composer-hint > span { margin-right: auto; }
.composer-hint .el-button { padding: 2px 5px; }
@media (max-width: 720px) {
  .chat-composer { padding: 12px 14px; }
  .composer-hint > span { display: none; }
}
</style>

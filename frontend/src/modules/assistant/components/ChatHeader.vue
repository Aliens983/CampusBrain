<script setup lang="ts">
import type { SessionMeta } from '../composables'

/** 头部：身份标识 + 历史会话切换 + 新对话 */
const props = defineProps<{
  sessions: SessionMeta[]
  currentSessionId: string
  sessionLabel: (s: SessionMeta) => string
  formatSessionDate: (value: string) => string
}>()

const emit = defineEmits<{
  (e: 'switch', id: string): void
  (e: 'new'): void
}>()
</script>

<template>
  <header class="chat-head">
    <div class="chat-head__identity">
      <div class="chat-head__avatar">问</div>
      <div>
        <div class="chat-head__title">AI 助手</div>
        <div class="chat-head__subtitle">基于知识库与实时预约数据回答问题</div>
      </div>
    </div>
    <div class="chat-head__actions">
      <el-select
        :model-value="props.currentSessionId"
        class="chat-head__select"
        placeholder="查看历史对话"
        filterable
        @change="emit('switch', $event as string)"
      >
        <el-option v-if="!props.sessions.length" disabled label="暂无历史对话" value="__empty__" />
        <el-option v-for="s in props.sessions" :key="s.id" :label="props.sessionLabel(s)" :value="s.id">
          <div class="session-option">
            <span>{{ s.title || '未命名对话' }}</span>
            <small>{{ props.formatSessionDate(s.updatedAt) }}</small>
          </div>
        </el-option>
      </el-select>
      <el-button class="new-session-button" @click="emit('new')">
        <span class="new-session-button__icon">+</span>新对话
      </el-button>
    </div>
  </header>
</template>

<style scoped>
.chat-head {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--line);
  background: var(--surface);
}

.chat-head__identity,
.chat-head__actions {
  display: flex;
  align-items: center;
}

.chat-head__identity {
  gap: 12px;
}

.chat-head__actions {
  gap: 10px;
}

.chat-head__avatar {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border: 1px solid var(--lake);
  border-radius: var(--radius-md);
  background: var(--lake);
  color: #f7f1e4;
  font-family: var(--font-serif);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0;
}

.chat-head__title {
  font-family: var(--font-serif);
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--ink);
}

.chat-head__subtitle {
  margin-top: 3px;
  color: var(--ink-4);
  font-size: 11.5px;
}

.chat-head__select {
  width: 260px;
}

.session-option {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.session-option span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-option small {
  flex-shrink: 0;
  color: var(--ink-4);
  font-family: var(--font-latin);
  font-size: 10px;
}

.new-session-button {
  height: 36px;
  border-radius: var(--radius-sm);
}

.new-session-button__icon {
  margin-right: 5px;
  font-size: 17px;
  line-height: 1;
}

@media (max-width: 720px) {
  .chat-head {
    align-items: flex-start;
    flex-direction: column;
    padding: 14px 16px;
  }

  .chat-head__actions {
    width: 100%;
  }

  .chat-head__select {
    flex: 1;
    width: auto;
  }
}
</style>

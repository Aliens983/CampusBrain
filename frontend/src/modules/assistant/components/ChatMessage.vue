<script setup lang="ts">
import { computed } from 'vue'
import type { ChatMsg } from '../composables'
import { renderMarkdown } from '../utils/markdown'

/** 单条消息：正文 + 打字指示器 + 动作回执 + 待确认卡片 */
const props = defineProps<{
  message: ChatMsg
  /** 是否正在生成（用于最后一条的打字提示） */
  streaming: boolean
  /** 该卡片是否为当前可操作的那一张 */
  active: boolean
}>()

const emit = defineEmits<{
  (e: 'reply', text: string): void
}>()

/** AI 正文按 Markdown 渲染（表格/加粗/列表）；用户消息保持纯文本 */
const renderedContent = computed(() =>
  props.message.role === 'assistant' ? renderMarkdown(props.message.content) : '',
)
</script>

<template>
  <div class="chat-msg" :class="props.message.role">
    <div class="chat-msg__label">{{ props.message.role === 'user' ? '我' : 'AI' }}</div>
    <div class="chat-msg__content">
      <div class="chat-msg__meta">{{ props.message.role === 'user' ? '你' : 'CampusBrain AI' }}</div>
      <div class="chat-msg__bubble">
        <div
          v-if="props.message.content && props.message.role === 'assistant'"
          class="chat-msg__text md-body"
          v-html="renderedContent"
        />
        <span v-else-if="props.message.content" class="chat-msg__text">{{ props.message.content }}</span>
        <span v-else-if="props.streaming" class="chat-msg__typing">AI 正在检索知识库并生成答案…</span>
        <span v-else class="chat-msg__typing">…</span>

        <!-- 预约动作结果（确认/取消执行后回执） -->
        <div v-if="props.message.action" class="action-badge">
          <span class="action-badge__dot" />{{ props.message.action.message }}
        </div>

        <!-- 待确认卡片：预约或取消前必须经用户点按钮或回复确认 -->
        <div v-if="props.message.confirm && props.active" class="confirm-card">
          <div class="confirm-card__head">
            <span class="confirm-card__title">
              {{ props.message.confirm.action === 'CANCEL' ? '取消确认' : '预约确认' }}
            </span>
            <el-tag size="small" :type="props.message.confirm.needAudit ? 'warning' : 'success'">
              {{
                props.message.confirm.action === 'CANCEL'
                  ? '取消后不可恢复'
                  : (props.message.confirm.needAudit ? '需审核' : '即时通过')
              }}
            </el-tag>
          </div>
          <div class="confirm-card__summary">{{ props.message.confirm.summary }}</div>
          <div class="confirm-card__actions">
            <el-button
              size="small"
              type="primary"
              :disabled="props.streaming"
              @click="emit('reply', '确认')"
            >
              {{ props.message.confirm.action === 'CANCEL' ? '确认取消' : '确认预约' }}
            </el-button>
            <el-button size="small" :disabled="props.streaming" @click="emit('reply', '取消')">
              再想想
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-msg {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  max-width: 900px;
  width: 100%;
  margin: 0 auto;
}

.chat-msg.user {
  flex-direction: row-reverse;
}

.chat-msg__label {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: var(--paper-2);
  color: var(--lake);
  font-family: var(--font-serif);
  font-size: 11px;
  font-weight: 600;
}

.chat-msg.user .chat-msg__label {
  background: rgba(95, 135, 118, 0.14);
  color: var(--celadon-deep);
}

.chat-msg__content {
  max-width: min(82%, 760px);
}

.chat-msg.user .chat-msg__content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.chat-msg__meta {
  margin: 1px 0 5px;
  color: var(--ink-4);
  font-size: 10.5px;
  letter-spacing: 0.04em;
}

.chat-msg__bubble {
  max-width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--line);
  border-radius: 4px 12px 12px 12px;
  background: var(--surface);
  box-shadow: var(--shadow-soft);
  line-height: 1.8;
}

.chat-msg.user .chat-msg__bubble {
  border-color: var(--lake);
  border-radius: 12px 4px 12px 12px;
  background: var(--lake);
  color: #f7f1e4;
  box-shadow: none;
}

.chat-msg__text {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
}

.chat-msg__typing {
  color: var(--ink-4);
  font-size: 12px;
}

/* ===== Markdown 正文（AI 回复） ===== */
.md-body {
  white-space: normal;
  word-break: break-word;
  line-height: 1.75;
}

.md-body :first-child { margin-top: 0; }
.md-body :last-child { margin-bottom: 0; }
.md-body p { margin: 6px 0; }

.md-body strong {
  color: var(--lake-deep);
  font-weight: 600;
}

.md-body ul,
.md-body ol {
  margin: 6px 0;
  padding-left: 22px;
}

.md-body li { margin: 3px 0; }

.md-body h1,
.md-body h2,
.md-body h3,
.md-body h4 {
  margin: 12px 0 6px;
  line-height: 1.4;
  color: var(--ink);
  font-family: var(--font-serif);
}

.md-body h1 { font-size: 18px; }
.md-body h2 { font-size: 16px; }
.md-body h3,
.md-body h4 { font-size: 15px; }

.md-body a {
  color: var(--lake);
  text-decoration: underline;
}

.md-body blockquote {
  margin: 8px 0;
  padding: 6px 12px;
  border-left: 2px solid var(--gold);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  background: var(--surface-2);
  color: var(--ink-3);
}

.md-body code {
  padding: 2px 6px;
  border-radius: 3px;
  background: var(--surface-3);
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
}

.md-body pre {
  margin: 8px 0;
  padding: 10px 12px;
  overflow-x: auto;
  border-radius: var(--radius-sm);
  background: #1b2f36;
}

.md-body pre code {
  padding: 0;
  background: transparent;
  color: #e8e0cf;
}

.md-body table {
  display: block;
  max-width: 100%;
  margin: 10px 0;
  overflow-x: auto;
  border-collapse: collapse;
  font-size: 12.5px;
}

.md-body th,
.md-body td {
  padding: 6px 12px;
  border: 1px solid var(--line);
  text-align: left;
  white-space: nowrap;
}

.md-body th {
  background: var(--surface-2);
  color: var(--ink);
  font-weight: 600;
}

.md-body tr:nth-child(even) td {
  background: rgba(250, 246, 236, 0.7);
}

.md-body hr {
  margin: 12px 0;
  border: none;
  border-top: 1px solid var(--line);
}

.action-badge {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 10px;
  padding: 8px 12px;
  border: 1px solid rgba(95, 135, 118, 0.2);
  border-radius: var(--radius-sm);
  background: rgba(95, 135, 118, 0.1);
  color: var(--celadon-deep);
  font-size: 12.5px;
  font-weight: 600;
}

.action-badge__dot {
  width: 7px;
  height: 7px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--celadon);
}

.confirm-card {
  margin-top: 12px;
  padding: 12px 14px;
  border: 1px solid rgba(176, 138, 69, 0.42);
  border-radius: var(--radius-md);
  background: var(--surface-2);
}

.confirm-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.confirm-card__title {
  font-family: var(--font-serif);
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
}

.confirm-card__summary {
  color: var(--ink-3);
  font-size: 12.5px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.confirm-card__actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

@media (max-width: 720px) {
  .chat-msg__content {
    max-width: 88%;
  }
}
</style>

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
.chat-msg.user { flex-direction: row-reverse; }
.chat-msg__label {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  background: linear-gradient(135deg, #0e6cd6, #3fb6ff);
}
.chat-msg.user .chat-msg__label { background: linear-gradient(135deg, #2f8f46, #59c66b); }
.chat-msg__content { max-width: min(82%, 760px); }
.chat-msg.user .chat-msg__content { display: flex; flex-direction: column; align-items: flex-end; }
.chat-msg__meta { margin: 1px 0 5px; color: var(--text-tertiary); font-size: 11px; }
.chat-msg__bubble {
  max-width: 100%;
  padding: 12px 16px;
  border-radius: 6px 16px 16px 16px;
  background: #f2f6fb;
  line-height: 1.8;
}
.chat-msg.user .chat-msg__bubble { border-radius: 16px 6px 16px 16px; background: #e4f7ec; }
.chat-msg__text { white-space: pre-wrap; word-break: break-word; font-size: 14px; }
.chat-msg__typing { color: #7aa7cf; font-size: 13px; }

/* ===== Markdown 正文（AI 回复） ===== */
.md-body {
  white-space: normal;
  word-break: break-word;
  line-height: 1.75;
}
.md-body :first-child { margin-top: 0; }
.md-body :last-child { margin-bottom: 0; }
.md-body p { margin: 6px 0; }
.md-body strong { color: #14466e; }
.md-body ul,
.md-body ol { margin: 6px 0; padding-left: 22px; }
.md-body li { margin: 3px 0; }
.md-body h1,
.md-body h2,
.md-body h3,
.md-body h4 { margin: 12px 0 6px; line-height: 1.4; color: #14466e; }
.md-body h1 { font-size: 18px; }
.md-body h2 { font-size: 16px; }
.md-body h3,
.md-body h4 { font-size: 15px; }
.md-body a { color: #0e6cd6; text-decoration: underline; }
.md-body blockquote {
  margin: 8px 0;
  padding: 4px 12px;
  border-left: 3px solid #9cc7ef;
  color: #4a6278;
  background: rgba(156, 199, 239, .12);
  border-radius: 0 6px 6px 0;
}
.md-body code {
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(14, 108, 214, .08);
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12.5px;
}
.md-body pre {
  margin: 8px 0;
  padding: 10px 12px;
  border-radius: 8px;
  background: #0f2437;
  overflow-x: auto;
}
.md-body pre code { padding: 0; background: transparent; color: #d7e8f7; }
/* 表格：余量/可约清单的主要展示形态 */
.md-body table {
  display: block;
  max-width: 100%;
  margin: 10px 0;
  border-collapse: collapse;
  overflow-x: auto;
  font-size: 13px;
}
.md-body th,
.md-body td {
  padding: 6px 12px;
  border: 1px solid #cfe0f0;
  text-align: left;
  white-space: nowrap;
}
.md-body th { background: #e7f1fb; color: #14466e; font-weight: 700; }
.md-body tr:nth-child(even) td { background: rgba(231, 241, 251, .45); }
.md-body hr { margin: 12px 0; border: none; border-top: 1px solid #d6e4f2; }

/* 预约动作回执 */
.action-badge {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 10px;
  padding: 8px 12px;
  border-radius: 10px;
  color: #1d6f3c;
  background: #e9f8ef;
  font-size: 13px;
  font-weight: 600;
}
.action-badge__dot { width: 7px; height: 7px; border-radius: 50%; background: #35a35f; flex-shrink: 0; }

/* 待确认卡片 */
.confirm-card {
  margin-top: 12px;
  padding: 12px 14px;
  border: 1px solid rgba(63, 182, 255, .28);
  border-radius: 12px;
  background: #f6fbff;
}
.confirm-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}
.confirm-card__title { font-size: 13px; font-weight: 700; color: #1c5f8f; }
.confirm-card__summary {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.confirm-card__actions { display: flex; gap: 8px; margin-top: 10px; }

@media (max-width: 720px) {
  .chat-msg__content { max-width: 88%; }
}
</style>

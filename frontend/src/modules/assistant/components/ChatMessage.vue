<script setup lang="ts">
import type { ChatMsg } from '../composables'

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
</script>

<template>
  <div class="chat-msg" :class="props.message.role">
    <div class="chat-msg__label">{{ props.message.role === 'user' ? '我' : 'AI' }}</div>
    <div class="chat-msg__content">
      <div class="chat-msg__meta">{{ props.message.role === 'user' ? '你' : 'CampusBrain AI' }}</div>
      <div class="chat-msg__bubble">
        <span v-if="props.message.content" class="chat-msg__text">{{ props.message.content }}</span>
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

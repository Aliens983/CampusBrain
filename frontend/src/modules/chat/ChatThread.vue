<template>
  <div class="thread">
    <div class="thread__head">
      <button
        class="back-btn"
        @click="router.back()"
      >
        <el-icon><ArrowLeft /></el-icon>
        <span>会话列表</span>
      </button>
      <div class="thread__title">
        <span class="thread__dot">💬</span>
        <strong>{{ peerName || '咨询沟通' }}</strong>
        <el-tag
          v-if="peerRole"
          size="small"
          type="info"
          effect="plain"
        >
          {{ peerRole === 'teacher' ? '咨询教师' : '学生' }}
        </el-tag>
      </div>
    </div>

    <div
      ref="bodyRef"
      class="thread__body"
    >
      <el-empty
        v-if="!loading && messages.length === 0"
        description="还没有消息，打个招呼吧 👋"
      />
      <div
        v-for="m in messages"
        :key="m.id"
        class="bubble-row"
        :class="m.isMine ? 'is-mine' : ''"
      >
        <div class="bubble">
          <p>{{ m.content }}</p>
          <span class="bubble__time">{{ fmtTime(m.createdAt) }}</span>
        </div>
      </div>
      <div
        v-if="loading"
        v-loading="loading"
        class="thread__loading"
      />
    </div>

    <div class="thread__composer">
      <el-input
        v-model="text"
        type="textarea"
        :rows="2"
        resize="none"
        maxlength="2000"
        placeholder="输入消息，Enter 发送 / Shift+Enter 换行"
        @keydown.enter.exact.prevent="send"
      />
      <el-button
        type="primary"
        :disabled="!text.trim() || sending"
        :loading="sending"
        class="thread__send"
        @click="send"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  fetchChatMessages,
  fetchChatConversations,
  sendChatMessage,
  refreshChatUnread,
  type ChatMessage,
} from '@/common/consultChat'

const route = useRoute()
const router = useRouter()

const conversationId = computed(() => Number(route.params.id))
const bodyRef = ref<HTMLElement | null>(null)
const messages = ref<ChatMessage[]>([])
const peerName = ref('')
const peerRole = ref<'student' | 'teacher' | ''>('')
const text = ref('')
const loading = ref(false)
const sending = ref(false)

let timer: number | undefined

onMounted(async () => {
  loading.value = true
  await Promise.all([loadMeta(), loadInitial()])
  loading.value = false
  scrollBottom()
  timer = window.setInterval(poll, 3000)
  refreshChatUnread()
})
onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})

async function loadMeta() {
  try {
    const list = await fetchChatConversations()
    const conv = list.find(c => c.id === conversationId.value)
    if (conv) {
      peerName.value = conv.peerName
      peerRole.value = conv.peerRole
    }
  } catch {
    /* 名称拿不到时用空态即可 */
  }
}

async function loadInitial() {
  try {
    messages.value = await fetchChatMessages(conversationId.value)
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '加载会话失败')
  }
}

async function poll() {
  try {
    const last = messages.value[messages.value.length - 1]?.id
    const fresh = await fetchChatMessages(conversationId.value, last)
    if (fresh.length) {
      messages.value.push(...fresh)
      scrollBottom()
      refreshChatUnread()
    }
  } catch {
    /* 轮询失败静默，下轮再试 */
  }
}

async function send() {
  const content = text.value.trim()
  if (!content || sending.value) return
  sending.value = true
  try {
    const msg = await sendChatMessage(conversationId.value, content)
    messages.value.push(msg)
    text.value = ''
    scrollBottom()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '发送失败')
  } finally {
    sending.value = false
  }
}

function scrollBottom() {
  nextTick(() => {
    const el = bodyRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function fmtTime(t?: string) {
  if (!t) return ''
  return t.replace('T', ' ').slice(5, 16)
}
</script>

<style scoped lang="scss">
.thread {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 150px);
  min-height: 480px;
  border: 1px solid var(--border-soft);
  border-radius: 20px;
  background: #fff;
  overflow: hidden;
  box-shadow: var(--shadow-card);
}

.thread__head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-soft);
  background: linear-gradient(180deg, #f7fbff, #fff);
}
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: none;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  padding: 6px 10px;
  border-radius: 10px;
}
.back-btn:hover {
  background: rgba(63, 182, 255, 0.1);
  color: var(--primary);
}
.thread__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
}
.thread__dot {
  font-size: 16px;
}

.thread__body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background:
    radial-gradient(circle at 10% 10%, rgba(63, 182, 255, 0.05), transparent 30%),
    radial-gradient(circle at 90% 90%, rgba(14, 108, 214, 0.04), transparent 30%);
}
.thread__loading {
  min-height: 60px;
}

.bubble-row {
  display: flex;
}
.bubble-row.is-mine {
  justify-content: flex-end;
}
.bubble {
  max-width: 68%;
  padding: 10px 14px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid var(--border-soft);
  box-shadow: 0 2px 6px rgba(20, 40, 80, 0.05);
  position: relative;
}
.bubble-row.is-mine .bubble {
  background: linear-gradient(135deg, #0e6cd6, #3fb6ff);
  color: #fff;
  border: none;
}
.bubble p {
  margin: 0 0 4px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.bubble__time {
  font-size: 11px;
  opacity: 0.55;
  display: block;
  text-align: right;
}

.thread__composer {
  display: flex;
  gap: 10px;
  padding: 14px;
  border-top: 1px solid var(--border-soft);
  background: #fff;
}
.thread__composer :deep(.el-textarea__inner) {
  border-radius: 12px;
}
.thread__send {
  align-self: flex-end;
  border-radius: 12px;
  min-width: 90px;
}
</style>

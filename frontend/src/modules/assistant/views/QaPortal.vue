<template>
  <div class="qa-portal">
    <!-- 知识库文档 -->
    <el-card
      shadow="never"
      class="section-card"
    >
      <template #header>
        <div class="card-header">
          <span>📄 知识库文档</span>
          <el-button
            size="small"
            @click="refreshDocuments"
          >
            刷新
          </el-button>
        </div>
      </template>

      <div
        v-if="isAdmin"
        class="upload-row"
      >
        <input
          ref="fileInput"
          type="file"
          multiple
          accept=".md,.markdown,.pdf,.txt,.xlsx,.xls"
          style="display: none"
          @change="onFileSelected"
        >
        <el-button
          size="small"
          type="primary"
          :loading="uploading"
          @click="triggerUpload"
        >
          上传文档
        </el-button>
        <span class="upload-tip">支持 PDF / Markdown / TXT / Excel（.pdf .md .txt .xlsx .xls，仅管理员可上传）</span>
      </div>

      <el-table
        v-if="documents.length"
        :data="documents"
        size="small"
        class="doc-table"
      >
        <el-table-column
          prop="title"
          label="文档"
          min-width="180"
        />
        <el-table-column
          label="类型"
          width="90"
        >
          <template #default="{ row }">
            {{ row.fileType || '—' }}
          </template>
        </el-table-column>
        <el-table-column
          label="状态"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="statusType(row.status)"
            >
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isAdmin"
          label="操作"
          width="80"
        >
          <template #default="{ row }">
            <el-button
              size="small"
              type="danger"
              link
              @click="deleteDocument(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty
        v-else
        description="暂无文档，上传后即可基于文档问答"
        :image-size="60"
      />
    </el-card>

    <!-- AI 问答（会话持久化：同一会话跨刷新保留，多轮续聊可回看） -->
    <el-card
      shadow="never"
      class="section-card"
    >
      <template #header>
        <div class="chat-head">
          <span class="chat-head__title">🤖 AI 助手</span>
          <el-select
            v-model="currentSessionId"
            size="small"
            class="chat-head__select"
            placeholder="历史会话"
            @change="onSwitchSession"
          >
            <el-option
              v-for="s in sessions"
              :key="s.id"
              :label="s.title || '未命名对话'"
              :value="s.id"
            />
          </el-select>
          <el-button
            size="small"
            plain
            @click="startNewSession"
          >
            新会话
          </el-button>
        </div>
      </template>

      <div class="qa-input">
        <el-input
          v-model="query"
          size="large"
          placeholder="问我预约相关的问题，例如：可以预约哪些服务？"
          :disabled="streaming"
          clearable
          @keyup.enter="askQuestion"
        />
        <el-button
          type="primary"
          size="large"
          class="qa-input__btn"
          :loading="streaming"
          @click="askQuestion"
        >
          {{ streaming ? '生成中…' : '提问' }}
        </el-button>
      </div>

      <div class="chat-body">
        <template v-if="messages.length">
          <div
            v-for="(m, idx) in messages"
            :key="idx"
            class="chat-msg"
            :class="m.role"
          >
            <div class="chat-msg__label">
              {{ m.role === 'user' ? '我' : 'AI' }}
            </div>
            <div class="chat-msg__bubble">
              <span
                v-if="m.content"
                class="chat-msg__text"
              >{{ m.content }}</span>
              <span
                v-else-if="streaming && idx === messages.length - 1"
                class="chat-msg__typing"
              >AI 正在检索知识库并生成答案…</span>
              <span
                v-else
                class="chat-msg__typing"
              >…</span>
            </div>
          </div>
        </template>
        <el-empty
          v-else-if="!streaming"
          description="输入问题，AI 将基于文档与实时预约数据回答；对话会自动保存，刷新或重进仍可查看。"
          :image-size="80"
        />
      </div>

      <div
        v-if="canFeedback"
        class="feedback-row"
      >
        <span class="feedback-hint">这条回答有帮助吗？</span>
        <el-button
          size="small"
          @click="recordFeedback('like')"
        >
          👍 有帮助
        </el-button>
        <el-button
          size="small"
          @click="recordFeedback('dislike')"
        >
          👎 没帮助
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/common/stores/user'

interface DocumentItem {
  id: number
  title: string
  fileType?: string
  fileSize?: number
  status?: string
  chunkCount?: number
}

interface SessionMeta {
  id: string
  title: string
  updatedAt: string
}

interface ChatMsg {
  /** 后端对话消息ID（assistant 持久化后才有，供反馈） */
  id?: number
  role: 'user' | 'assistant'
  content: string
}

const userStore = useUserStore()
const isAdmin = computed(() => ['admin', 'super_admin'].includes(userStore.userInfo?.role || ''))
const query = ref('')
const messages = ref<ChatMsg[]>([])
const streaming = ref(false)
const documents = ref<DocumentItem[]>([])
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const BASE = '/api/v1/kb'

const uid = computed(() => String(userStore.userInfo?.id ?? 'anon'))
const sessions = ref<SessionMeta[]>([])
const currentSessionId = ref('')

function sessionsKey() {
  return `campusbrain:kb_sessions:${uid.value}`
}
function currentKey() {
  return `campusbrain:kb_current:${uid.value}`
}

function uuid(): string {
  return crypto.randomUUID ? crypto.randomUUID() : `s-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

function loadSessionsFromStorage(): SessionMeta[] {
  try {
    const raw = localStorage.getItem(sessionsKey())
    return raw ? (JSON.parse(raw) as SessionMeta[]) : []
  } catch {
    return []
  }
}
function persistSessions() {
  localStorage.setItem(sessionsKey(), JSON.stringify(sessions.value))
}
function persistCurrent() {
  localStorage.setItem(currentKey(), currentSessionId.value)
}

function authHeaders(): Record<string, string> {
  return { Authorization: `Bearer ${userStore.token}` }
}

async function fetchRaw(path: string, init?: RequestInit) {
  const resp = await fetch(`${BASE}${path}`, { headers: authHeaders(), ...init })
  const result = await resp.json()
  if (result.code === 0 || result.code === 200) return result.data
  throw new Error(result.message || '请求失败')
}

function ensureSession() {
  // 恢复/创建一个当前会话（跨刷新沿用）
  const storedCurrent = localStorage.getItem(currentKey())
  const all = loadSessionsFromStorage()
  if (storedCurrent && all.some(s => s.id === storedCurrent)) {
    currentSessionId.value = storedCurrent
    sessions.value = all
    return
  }
  const id = uuid()
  currentSessionId.value = id
  sessions.value = [{ id, title: '新对话', updatedAt: new Date().toISOString() }, ...all]
  persistSessions()
  persistCurrent()
}

function touchSessionTitle(title: string) {
  const found = sessions.value.find(s => s.id === currentSessionId.value)
  if (found) {
    if (found.title === '新对话') found.title = title.slice(0, 30)
    found.updatedAt = new Date().toISOString()
  }
  persistSessions()
}

async function loadHistory(sessionId: string) {
  messages.value = []
  if (!sessionId) return
  try {
    const rows = (await fetchRaw(`/qa/conversation/${sessionId}`)) as Array<{
      id?: number
      role?: string
      content?: string
    }>
    ;(rows || []).forEach(r => {
      if (r.role === 'user' || r.role === 'assistant') {
        messages.value.push({
          id: r.id,
          role: r.role as 'user' | 'assistant',
          content: r.content || '',
        })
      }
    })
  } catch (e) {
    console.error('加载会话历史失败', e)
  }
}

function onSwitchSession(id: string) {
  if (streaming.value) return
  currentSessionId.value = id
  persistCurrent()
  loadHistory(id)
}

function startNewSession() {
  if (streaming.value) return
  const id = uuid()
  currentSessionId.value = id
  sessions.value = [{ id, title: '新对话', updatedAt: new Date().toISOString() }, ...sessions.value]
  persistSessions()
  persistCurrent()
  messages.value = []
  query.value = ''
}

function askQuestion() {
  const q = query.value.trim()
  if (!q || streaming.value) return

  streaming.value = true
  // 同一会话内连续提问（延续上下文），并自动落库、可回看
  messages.value.push({ role: 'user', content: q })
  const aiMsg: ChatMsg = { role: 'assistant', content: '' }
  messages.value.push(aiMsg)
  query.value = ''

  const params = new URLSearchParams({
    query: q,
    sessionId: currentSessionId.value,
    token: userStore.token,
  })
  const es = new EventSource(`${BASE}/qa/ask/stream?${params.toString()}`)

  es.addEventListener('messageId', (event) => {
    const id = Number((event as MessageEvent).data)
    if (id) aiMsg.id = id
  })

  es.onmessage = (event) => {
    if (event.data === '[DONE]') {
      streaming.value = false
      es.close()
      touchSessionTitle(q)
      if (!aiMsg.content) aiMsg.content = '（本次未生成内容，请换个问法试试）'
      return
    }
    if (event.data.startsWith('[ERROR]')) {
      aiMsg.content += `\n\n${event.data.replace('[ERROR] ', '')}`
      streaming.value = false
      es.close()
      return
    }
    aiMsg.content += event.data
  }

  es.onerror = () => {
    if (!aiMsg.content) aiMsg.content = '连接失败，请确认后端服务已启动。'
    streaming.value = false
    es.close()
  }
}

const lastAssistantMsgId = computed(() => {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    const m = messages.value[i]
    if (m.role === 'assistant') return m.id ?? null
  }
  return null
})
const canFeedback = computed(() => !streaming.value && lastAssistantMsgId.value != null)

async function recordFeedback(type: 'like' | 'dislike') {
  const msgId = lastAssistantMsgId.value
  if (msgId == null) {
    ElMessage.warning('请先发起一次问答再反馈')
    return
  }
  try {
    await fetchRaw('/qa/feedback', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId: currentSessionId.value, messageId: msgId, feedback: type }),
    })
    ElMessage.success(type === 'like' ? '感谢你的点赞 👍' : '已记录，我们会继续改进 🙏')
  } catch (e) {
    ElMessage.error((e as Error).message || '反馈提交失败，请重试')
  }
}

// ---------------- 文档管理 ----------------
async function refreshDocuments() {
  try {
    documents.value = await fetchRaw('/documents')
  } catch (e) {
    console.error('获取文档失败', e)
  }
}

function triggerUpload() {
  fileInput.value?.click()
}

function onFileSelected(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files) {
    for (const file of Array.from(target.files)) uploadFile(file)
  }
  target.value = ''
}

async function uploadFile(file: File) {
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const resp = await fetch(`${BASE}/documents/upload`, {
      method: 'POST',
      headers: authHeaders(),
      body: formData,
    })
    const result = await resp.json()
    if (result.code === 0 || result.code === 200) {
      ElMessage.success('上传成功')
      setTimeout(refreshDocuments, 800)
    } else {
      ElMessage.error(result.message || '上传失败')
    }
  } catch {
    ElMessage.error('上传失败，请重试')
  } finally {
    uploading.value = false
  }
}

async function deleteDocument(doc: DocumentItem) {
  try {
    await fetchRaw(`/documents/${doc.id}`, { method: 'DELETE' })
    ElMessage.success('已删除')
    refreshDocuments()
  } catch (e) {
    ElMessage.error((e as Error).message || '删除失败')
  }
}

function statusType(status?: string): 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    READY: 'success',
    UPLOADED: 'info',
    PARSING: 'warning',
    CHUNKING: 'warning',
    EMBEDDING: 'warning',
    FAILED: 'danger',
  }
  return map[status || ''] || 'info'
}

function statusLabel(status?: string): string {
  const map: Record<string, string> = {
    READY: '就绪',
    UPLOADED: '已上传',
    PARSING: '解析中',
    CHUNKING: '分块中',
    EMBEDDING: '向量化中',
    FAILED: '失败',
  }
  return map[status || ''] || status || '未知'
}

onMounted(async () => {
  refreshDocuments()
  ensureSession()
  await loadHistory(currentSessionId.value)
})
</script>

<style scoped>
.qa-portal {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.section-card {
  border-radius: 10px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.upload-tip {
  font-size: 12px;
  color: #999;
}
.doc-table {
  width: 100%;
}
.qa-input {
  display: flex;
  gap: 12px;
}
.qa-input .el-input {
  flex: 1;
}
.qa-input__btn {
  flex-shrink: 0;
  min-width: 128px;
  margin: 0;
  font-weight: 600;
}
.chat-head {
  display: flex;
  align-items: center;
  gap: 12px;
}
.chat-head__title {
  font-weight: 600;
}
.chat-head__select {
  width: 220px;
  margin-left: auto;
}
.chat-body {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 55vh;
  overflow-y: auto;
  padding-right: 4px;
}
.chat-msg {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.chat-msg.user {
  flex-direction: row-reverse;
}
.chat-msg__label {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  background: linear-gradient(135deg, #0e6cd6, #3fb6ff);
}
.chat-msg.user .chat-msg__label {
  background: linear-gradient(135deg, #2f8f46, #59c66b);
}
.chat-msg__bubble {
  max-width: 82%;
  padding: 10px 14px;
  border-radius: 12px;
  background: #f2f6fb;
  line-height: 1.8;
}
.chat-msg.user .chat-msg__bubble {
  background: #e4f7ec;
}
.chat-msg__text {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
}
.chat-msg__typing {
  color: #7aa7cf;
  font-size: 13px;
}
.feedback-row {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.feedback-hint {
  color: #888;
  font-size: 12px;
}
</style>

<template>
  <div class="qa-portal">
    <section v-if="isAdmin" class="knowledge-strip">
      <div class="knowledge-strip__title"><span class="strip-icon">KB</span><strong>知识库文档</strong><span class="doc-count">{{ documents.length }} 份</span></div>
      <div class="knowledge-strip__actions">
        <input ref="fileInput" type="file" multiple accept=".md,.markdown,.pdf,.txt,.xlsx,.xls" hidden @change="onFileSelected">
        <span class="knowledge-strip__hint">支持 PDF、Markdown、TXT、Excel</span>
        <el-button text @click="documentsVisible = true">文档管理</el-button>
        <el-button text @click="refreshDocuments">刷新</el-button>
        <el-button type="primary" :loading="uploading" @click="triggerUpload">上传文档</el-button>
      </div>
    </section>

    <el-dialog v-model="documentsVisible" title="知识库文档管理" width="720px">
      <el-table v-if="documents.length" :data="documents" size="small">
        <el-table-column prop="title" label="文档" min-width="220" />
        <el-table-column prop="fileType" label="类型" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button type="danger" link @click="deleteDocument(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无知识库文档" :image-size="70" />
    </el-dialog>

    <section class="chat-workspace">
      <header class="chat-head">
        <div class="chat-head__identity">
          <div class="chat-head__avatar">AI</div>
          <div><div class="chat-head__title">AI 助手</div><div class="chat-head__subtitle">基于知识库与实时预约数据回答问题</div></div>
        </div>
        <div class="chat-head__actions">
          <el-select v-model="currentSessionId" class="chat-head__select" placeholder="查看历史对话" filterable @change="onSwitchSession">
            <el-option v-if="!sessions.length" disabled label="暂无历史对话" value="__empty__" />
            <el-option v-for="s in sessions" :key="s.id" :label="sessionLabel(s)" :value="s.id">
              <div class="session-option"><span>{{ s.title || '未命名对话' }}</span><small>{{ formatSessionDate(s.updatedAt) }}</small></div>
            </el-option>
          </el-select>
          <el-button class="new-session-button" @click="startNewSession"><span class="new-session-button__icon">+</span>新对话</el-button>
        </div>
      </header>

      <div
        ref="chatBodyRef"
        class="chat-body"
        @scroll="onBodyScroll"
      >
        <template v-if="messages.length">
          <div v-for="(m, idx) in messages" :key="idx" class="chat-msg" :class="m.role">
            <div class="chat-msg__label">{{ m.role === 'user' ? '我' : 'AI' }}</div>
            <div class="chat-msg__content">
              <div class="chat-msg__meta">{{ m.role === 'user' ? '你' : 'CampusBrain AI' }}</div>
              <div class="chat-msg__bubble">
                <span v-if="m.content" class="chat-msg__text">{{ m.content }}</span>
                <span v-else-if="streaming && idx === messages.length - 1" class="chat-msg__typing">AI 正在检索知识库并生成答案…</span>
                <span v-else class="chat-msg__typing">…</span>
              </div>
            </div>
          </div>
        </template>
        <div v-else class="chat-empty">
          <div class="chat-empty__icon">AI</div>
          <h2>今天想了解什么？</h2>
          <p>你可以询问校园服务、预约规则，也可以直接查询可用时段。</p>
          <div class="chat-empty__suggestions">
            <button @click="query = '可以预约哪些校园服务？'">可以预约哪些校园服务？</button>
            <button @click="query = '如何查询教室的可用时段？'">如何查询教室的可用时段？</button>
          </div>
        </div>
      </div>

      <footer class="chat-composer">
        <div class="qa-input">
          <el-input v-model="query" type="textarea" :autosize="{ minRows: 1, maxRows: 5 }" resize="none" placeholder="输入你的问题，按 Enter 发送" :disabled="streaming" clearable @keydown.enter.exact.prevent="askQuestion" />
          <el-button type="primary" class="qa-input__btn" :loading="streaming" @click="askQuestion">{{ streaming ? '生成中…' : '发送' }}</el-button>
        </div>
        <div class="composer-hint"><span>回答由知识库与实时数据生成，请核对重要信息</span><template v-if="canFeedback"><el-button text size="small" @click="recordFeedback('like')">有帮助</el-button><el-button text size="small" @click="recordFeedback('dislike')">没帮助</el-button></template></div>
      </footer>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/common/stores/user'

interface DocumentItem { id: number; title: string; fileType?: string; status?: string }
interface SessionMeta { id: string; title: string; updatedAt: string }
interface ChatMsg { id?: number; role: 'user' | 'assistant'; content: string }

const userStore = useUserStore()
const isAdmin = computed(() => ['admin', 'super_admin'].includes(userStore.userInfo?.role || ''))
const query = ref('')
const messages = ref<ChatMsg[]>([])
const streaming = ref(false)
const documents = ref<DocumentItem[]>([])
const documentsVisible = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const BASE = '/api/v1/kb'
const uid = computed(() => String(userStore.userInfo?.id ?? 'anon'))
const sessions = ref<SessionMeta[]>([])
const currentSessionId = ref('')

// ===== 聊天区自动滚动 =====
// 真实滚动容器（.chat-body 自身 overflow-y:auto）
const chatBodyRef = ref<HTMLElement | null>(null)
// 用户是否贴着底部阅读：上滑查看历史时暂停自动跟随，回到底部后恢复
const stickToBottom = ref(true)
let scrollFrame = 0

function doScrollToBottom() {
  const el = chatBodyRef.value
  if (el) el.scrollTop = el.scrollHeight
}
/** 立即滚到底部（发消息 / 切换会话 / 流结束时用） */
function scrollToBottom() {
  stickToBottom.value = true
  if (scrollFrame) {
    cancelAnimationFrame(scrollFrame)
    scrollFrame = 0
  }
  void nextTick(doScrollToBottom)
}
/** 高频增量时按帧合并滚动，同一帧最多滚一次 */
function scheduleScroll() {
  if (!stickToBottom.value || scrollFrame) return
  scrollFrame = requestAnimationFrame(() => {
    scrollFrame = 0
    doScrollToBottom()
  })
}
function onBodyScroll() {
  const el = chatBodyRef.value
  if (!el) return
  // 距底 60px 内视为“贴着底部”
  stickToBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight < 60
}

function sessionsKey() { return `campusbrain:kb_sessions:${uid.value}` }
function currentKey() { return `campusbrain:kb_current:${uid.value}` }
function uuid() { return crypto.randomUUID ? crypto.randomUUID() : `s-${Date.now()}-${Math.random().toString(36).slice(2, 10)}` }
function authHeaders(): Record<string, string> { return { Authorization: `Bearer ${userStore.token}` } }
function loadSessionsFromStorage(): SessionMeta[] { try { const raw = localStorage.getItem(sessionsKey()); return raw ? JSON.parse(raw) as SessionMeta[] : [] } catch { return [] } }
function persistSessions() { localStorage.setItem(sessionsKey(), JSON.stringify(sessions.value)) }
function persistCurrent() { localStorage.setItem(currentKey(), currentSessionId.value) }

async function fetchRaw(path: string, init?: RequestInit) {
  const resp = await fetch(`${BASE}${path}`, { headers: authHeaders(), ...init })
  const result = await resp.json()
  if (result.code === 0 || result.code === 200) return result.data
  throw new Error(result.message || '请求失败')
}
function ensureSession() {
  const current = localStorage.getItem(currentKey())
  const all = loadSessionsFromStorage().filter(s => s.title !== '新对话')
  sessions.value = all
  currentSessionId.value = current && all.some(s => s.id === current) ? current : ''
  persistSessions()
}
function registerActive(title: string) {
  const found = sessions.value.find(s => s.id === currentSessionId.value)
  if (!found) sessions.value = [{ id: currentSessionId.value, title: title.slice(0, 30), updatedAt: new Date().toISOString() }, ...sessions.value]
  else { found.title = found.title || title.slice(0, 30); found.updatedAt = new Date().toISOString() }
  persistSessions(); persistCurrent()
}
function touchSessionTitle(title: string) { const found = sessions.value.find(s => s.id === currentSessionId.value); if (found) { found.title = found.title || title.slice(0, 30); found.updatedAt = new Date().toISOString() }; persistSessions() }
function sessionLabel(s: SessionMeta) { return `${s.title || '未命名对话'} · ${formatSessionDate(s.updatedAt)}` }
function formatSessionDate(value: string) { const date = new Date(value); if (Number.isNaN(date.getTime())) return '刚刚'; const now = new Date(); return date.toDateString() === now.toDateString() ? date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }) }

async function loadHistory(id: string) {
  messages.value = []
  if (!id) return
  try { const rows = await fetchRaw(`/qa/conversation/${id}`) as Array<{ id?: number; role?: string; content?: string }>; for (const row of rows || []) if (row.role === 'user' || row.role === 'assistant') messages.value.push({ id: row.id, role: row.role, content: row.content || '' }) } catch (e) { console.error('加载会话历史失败', e) }
  scrollToBottom()
}
function onSwitchSession(id: string) { if (streaming.value || id === '__empty__') return; currentSessionId.value = id; persistCurrent(); loadHistory(id) }
function startNewSession() { if (streaming.value) return; currentSessionId.value = ''; messages.value = []; query.value = ''; persistCurrent() }
function askQuestion() {
  const q = query.value.trim(); if (!q || streaming.value) return
  if (!currentSessionId.value) currentSessionId.value = uuid(); registerActive(q); streaming.value = true
  messages.value.push({ role: 'user', content: q }); const aiMsg: ChatMsg = { role: 'assistant', content: '' }; messages.value.push(aiMsg); query.value = ''
  scrollToBottom()
  const params = new URLSearchParams({ query: q, sessionId: currentSessionId.value, token: userStore.token }); const es = new EventSource(`${BASE}/qa/ask/stream?${params.toString()}`)
  es.addEventListener('messageId', event => { const id = Number((event as MessageEvent).data); if (id) aiMsg.id = id })
  es.onmessage = event => { if (event.data === '[DONE]') { streaming.value = false; es.close(); touchSessionTitle(q); if (!aiMsg.content) aiMsg.content = '（本次未生成内容，请换个问法试试）'; scrollToBottom(); return }; if (event.data.startsWith('[ERROR]')) { aiMsg.content += `\n\n${event.data.replace('[ERROR] ', '')}`; streaming.value = false; es.close(); scrollToBottom(); return }; aiMsg.content += event.data; scheduleScroll() }
  es.onerror = () => { if (!aiMsg.content) aiMsg.content = '连接失败，请确认后端服务已启动。'; streaming.value = false; es.close(); scrollToBottom() }
}
const lastAssistantMsgId = computed(() => { for (let i = messages.value.length - 1; i >= 0; i--) if (messages.value[i].role === 'assistant') return messages.value[i].id ?? null; return null })
const canFeedback = computed(() => !streaming.value && lastAssistantMsgId.value != null)
async function recordFeedback(type: 'like' | 'dislike') { const id = lastAssistantMsgId.value; if (id == null) return; try { await fetchRaw('/qa/feedback', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ sessionId: currentSessionId.value, messageId: id, feedback: type }) }); ElMessage.success(type === 'like' ? '感谢你的点赞' : '已记录反馈') } catch (e) { ElMessage.error((e as Error).message || '反馈提交失败，请重试') } }

async function refreshDocuments() { try { documents.value = await fetchRaw('/documents') } catch (e) { console.error('获取文档失败', e) } }
async function deleteDocument(doc: DocumentItem) { try { await fetchRaw(`/documents/${doc.id}`, { method: 'DELETE' }); ElMessage.success('已删除'); await refreshDocuments() } catch (e) { ElMessage.error((e as Error).message || '删除失败') } }
function statusType(status?: string): 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = { READY: 'success', UPLOADED: 'info', PARSING: 'warning', CHUNKING: 'warning', EMBEDDING: 'warning', FAILED: 'danger' }
  return map[status || ''] || 'info'
}
function statusLabel(status?: string): string {
  const map: Record<string, string> = { READY: '就绪', UPLOADED: '已上传', PARSING: '解析中', CHUNKING: '分块中', EMBEDDING: '向量化中', FAILED: '失败' }
  return map[status || ''] || status || '未知'
}
function triggerUpload() { fileInput.value?.click() }
function onFileSelected(event: Event) { const target = event.target as HTMLInputElement; if (target.files) for (const file of Array.from(target.files)) uploadFile(file); target.value = '' }
async function uploadFile(file: File) { uploading.value = true; try { const formData = new FormData(); formData.append('file', file); const resp = await fetch(`${BASE}/documents/upload`, { method: 'POST', headers: authHeaders(), body: formData }); const result = await resp.json(); if (result.code === 0 || result.code === 200) { ElMessage.success('上传成功'); setTimeout(refreshDocuments, 800) } else ElMessage.error(result.message || '上传失败') } catch { ElMessage.error('上传失败，请重试') } finally { uploading.value = false } }

onMounted(async () => { if (isAdmin.value) refreshDocuments(); ensureSession(); await loadHistory(currentSessionId.value) })
onUnmounted(() => {
  // 离开页面：取消待执行的滚动帧
  if (scrollFrame) cancelAnimationFrame(scrollFrame)
})
</script>

<style scoped>
.qa-portal { height: calc(100vh - 126px); min-height: 560px; display: flex; flex-direction: column; gap: 12px; }
.knowledge-strip { flex: 0 0 auto; display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 10px 14px; border: 1px solid rgba(63,182,255,.16); border-radius: 14px; background: rgba(255,255,255,.82); }
.knowledge-strip__title,.knowledge-strip__actions { display: flex; align-items: center; gap: 10px; }
.strip-icon,.chat-head__avatar,.chat-empty__icon { display: grid; place-items: center; color: #fff; font-weight: 800; background: linear-gradient(135deg,#1167b1,#3fb6ff); box-shadow: 0 8px 18px rgba(30,152,242,.2); }
.strip-icon { width: 28px; height: 28px; border-radius: 8px; font-size: 10px; }
.doc-count { padding: 3px 8px; border-radius: 999px; color: #2673a8; background: #eef8ff; font-size: 12px; }
.knowledge-strip__hint { color: var(--text-tertiary); font-size: 12px; }
.chat-workspace { min-height: 0; flex: 1; display: flex; flex-direction: column; overflow: hidden; border: 1px solid rgba(37,99,155,.1); border-radius: 14px; background: rgba(255,255,255,.9); box-shadow: 0 18px 50px rgba(30,75,115,.1); }
.chat-head { flex-shrink: 0; display: flex; align-items: center; justify-content: space-between; gap: 18px; padding: 18px 24px; border-bottom: 1px solid rgba(37,99,155,.09); background: rgba(248,252,255,.82); }
.chat-head__identity,.chat-head__actions { display: flex; align-items: center; }.chat-head__identity { gap: 12px; }.chat-head__actions { gap: 10px; }
.chat-head__avatar { width: 40px; height: 40px; border-radius: 12px; font-size: 13px; }.chat-head__title { font-size: 16px; font-weight: 700; }.chat-head__subtitle { margin-top: 3px; color: var(--text-tertiary); font-size: 12px; }
.chat-head__select { width: 260px; }.chat-head__select :deep(.el-input__wrapper) { border-radius: 10px; box-shadow: 0 0 0 1px rgba(63,182,255,.16) inset; }
.session-option { display: flex; justify-content: space-between; gap: 12px; width: 100%; }.session-option span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.session-option small { flex-shrink: 0; color: var(--text-tertiary); }
.new-session-button { height: 36px; border-radius: 10px; font-weight: 600; }.new-session-button__icon { margin-right: 5px; font-size: 18px; line-height: 1; }
.chat-body { flex: 1; min-height: 0; display: flex; flex-direction: column; gap: 14px; overflow-y: auto; padding: 28px clamp(18px,7vw,110px); background: linear-gradient(180deg,rgba(249,252,255,.78),rgba(242,248,255,.5)); }
.chat-msg { display: flex; gap: 10px; align-items: flex-start; max-width: 900px; width: 100%; margin: 0 auto; }.chat-msg.user { flex-direction: row-reverse; }.chat-msg__label { flex-shrink: 0; width: 30px; height: 30px; display: grid; place-items: center; border-radius: 50%; color: #fff; font-size: 12px; font-weight: 700; background: linear-gradient(135deg,#0e6cd6,#3fb6ff); }.chat-msg.user .chat-msg__label { background: linear-gradient(135deg,#2f8f46,#59c66b); }
.chat-msg__content { max-width: min(82%,760px); }.chat-msg.user .chat-msg__content { display: flex; flex-direction: column; align-items: flex-end; }.chat-msg__meta { margin: 1px 0 5px; color: var(--text-tertiary); font-size: 11px; }.chat-msg__bubble { max-width: 100%; padding: 12px 16px; border-radius: 6px 16px 16px 16px; background: #f2f6fb; line-height: 1.8; }.chat-msg.user .chat-msg__bubble { border-radius: 16px 6px 16px 16px; background: #e4f7ec; }.chat-msg__text { white-space: pre-wrap; word-break: break-word; font-size: 14px; }.chat-msg__typing { color: #7aa7cf; font-size: 13px; }
.chat-empty { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 36px 18px; text-align: center; }.chat-empty__icon { width: 58px; height: 58px; margin-bottom: 18px; border-radius: 18px; font-size: 16px; }.chat-empty h2 { margin: 0 0 8px; font-size: 22px; }.chat-empty p { margin: 0; color: var(--text-secondary); font-size: 13px; }.chat-empty__suggestions { display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; margin-top: 22px; }.chat-empty__suggestions button { padding: 9px 14px; border: 1px solid rgba(63,182,255,.2); border-radius: 10px; color: #2673a8; background: rgba(255,255,255,.75); cursor: pointer; transition: .2s; }.chat-empty__suggestions button:hover { border-color: #3fb6ff; background: #fff; transform: translateY(-1px); }
.chat-composer { flex-shrink: 0; padding: 16px clamp(18px,7vw,110px) 18px; border-top: 1px solid rgba(37,99,155,.09); background: rgba(255,255,255,.96); }.qa-input { display: flex; gap: 12px; }.qa-input .el-input { flex: 1; }.qa-input .el-textarea :deep(.el-textarea__inner) { min-height: 44px !important; padding: 12px 14px; border-radius: 12px; box-shadow: 0 0 0 1px rgba(63,182,255,.16) inset; }.qa-input__btn { flex-shrink: 0; min-width: 96px; margin: 0; border-radius: 12px; font-weight: 600; }.composer-hint { display: flex; align-items: center; gap: 4px; margin-top: 7px; color: var(--text-tertiary); font-size: 11px; }.composer-hint > span { margin-right: auto; }.composer-hint .el-button { padding: 2px 5px; }
@media (max-width:720px) { .qa-portal { height: calc(100vh - 156px); min-height: 520px; }.knowledge-strip { align-items: flex-start; flex-direction: column; gap: 8px; }.knowledge-strip__actions { width: 100%; }.knowledge-strip__hint { margin-right: auto; }.chat-head { align-items: flex-start; flex-direction: column; padding: 14px 16px; }.chat-head__actions { width: 100%; }.chat-head__select { flex: 1; width: auto; }.chat-body { padding: 20px 14px; }.chat-composer { padding: 12px 14px; }.chat-msg__content { max-width: 88%; }.composer-hint > span { display: none; } }
</style>

import { ref, computed, onMounted, onUnmounted, nextTick, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/common/stores/user'
import { API_SUCCESS_CODE } from '@/common/utils/request'
import { campusLabel, categoryLabel, loadCategoryDictionary } from '@/common/dictionary'

/** 知识库文档条目 */
export interface DocumentItem { id: number; title: string; fileType?: string; status?: string }
/** 会话列表条目（存 localStorage） */
export interface SessionMeta { id: string; title: string; updatedAt: string }
/** 多轮对话中 AI 记住的预约条件（槽位） */
export interface SlotsView { campus?: string; category?: string; date?: string; startTime?: string; endTime?: string; serviceId?: number; keyword?: string }
/** 待用户确认的动作：BOOK 预约 / CANCEL 取消 */
export interface PendingView { action: 'BOOK' | 'CANCEL'; draftId?: string; orderId?: number; summary?: string; needAudit?: boolean }
/** 动作执行回执 */
export interface ActionView { status?: string; statusText?: string; message?: string; orderId?: number }
/** 一条对话消息 */
export interface ChatMsg { id?: number; role: 'user' | 'assistant'; content: string; confirm?: PendingView; action?: ActionView }

const BASE = '/api/v1/kb'
/** 看门狗阈值：略大于后端 LLM 流式超时（90s），给正常长回答留足余量 */
const SSE_WATCHDOG_MS = 120_000

/**
 * AI 助手页的全部逻辑。
 * <p>
 * 原集中在 QaPortal.vue（401 行）内，页面既管布局又管 SSE、会话持久化与文档管理，
 * 难以定位问题。这里按职责收敛为单一 composable，页面只负责组装组件。
 */
export function useQaPortal() {
  const userStore = useUserStore()
  const isAdmin = computed(() => ['admin', 'super_admin'].includes(userStore.userInfo?.role || ''))

  const query = ref('')
  const messages = ref<ChatMsg[]>([])
  const streaming = ref(false)

  // ===== 多轮上下文 =====
  const slots = ref<SlotsView>({})
  const pending = ref<PendingView | null>(null)
  const pendingIndex = ref<number | null>(null)

  // ===== 会话列表（localStorage） =====
  const sessions = ref<SessionMeta[]>([])
  const currentSessionId = ref('')

  // ===== 知识库文档（仅管理员） =====
  const documents = ref<DocumentItem[]>([])
  const documentsVisible = ref(false)
  const uploading = ref(false)

  // ===== 聊天区自动滚动 =====
  const chatBodyRef = ref<HTMLElement | null>(null)
  // 用户上滑查看历史时暂停自动跟随，回到底部后恢复
  const stickToBottom = ref(true)
  let scrollFrame = 0

  // ===== SSE =====
  /** 保存引用是为了在组件卸载 / 超时 / 出错时能主动关闭，否则切路由后连接仍在跑 */
  let es: EventSource | null = null
  let streamWatchdog = 0

  const uid = computed(() => String(userStore.userInfo?.id ?? 'anon'))

  const slotChips = computed(() => {
    const s = slots.value
    const chips: Array<{ key: string; text: string }> = []
    if (s.campus) chips.push({ key: 'campus', text: campusLabel(s.campus) })
    if (s.category) chips.push({ key: 'category', text: categoryLabel(s.category) })
    if (s.date) chips.push({ key: 'date', text: s.date })
    if (s.startTime && s.endTime) chips.push({ key: 'time', text: `${s.startTime}-${s.endTime}` })
    else if (s.startTime) chips.push({ key: 'time', text: `${s.startTime} 起` })
    if (s.keyword) chips.push({ key: 'keyword', text: s.keyword })
    return chips
  })

  const lastAssistantMsgId = computed(() => {
    for (let i = messages.value.length - 1; i >= 0; i--) {
      if (messages.value[i].role === 'assistant') return messages.value[i].id ?? null
    }
    return null
  })
  const canFeedback = computed(() => !streaming.value && lastAssistantMsgId.value != null)

  // ==================== 滚动 ====================

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
    // 距底 60px 内视为"贴着底部"
    stickToBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight < 60
  }

  // ==================== 请求与会话持久化 ====================

  function authHeaders(): Record<string, string> {
    return { Authorization: `Bearer ${userStore.token}` }
  }

  async function fetchRaw(path: string, init?: RequestInit) {
    // headers 必须合并而不是被 init 覆盖：此前 `...init` 在 headers 之后展开，
    // 调用方传的 { 'Content-Type': ... } 会整体顶掉 authHeaders()，请求不带
    // Authorization，网关返回 401——点赞/点踩 100% 失败。
    const resp = await fetch(`${BASE}${path}`, {
      ...init,
      headers: { ...authHeaders(), ...(init?.headers ?? {}) }
    })
    const result = await resp.json()
    if (result.code === API_SUCCESS_CODE) return result.data
    throw new Error(result.message || '请求失败')
  }

  function sessionsKey() { return `campusbrain:kb_sessions:${uid.value}` }
  function currentKey() { return `campusbrain:kb_current:${uid.value}` }
  function uuid() {
    return crypto.randomUUID ? crypto.randomUUID() : `s-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
  }
  function loadSessionsFromStorage(): SessionMeta[] {
    try {
      const raw = localStorage.getItem(sessionsKey())
      return raw ? JSON.parse(raw) as SessionMeta[] : []
    } catch { return [] }
  }
  function persistSessions() { localStorage.setItem(sessionsKey(), JSON.stringify(sessions.value)) }
  function persistCurrent() { localStorage.setItem(currentKey(), currentSessionId.value) }

  function ensureSession() {
    const current = localStorage.getItem(currentKey())
    const all = loadSessionsFromStorage().filter(s => s.title !== '新对话')
    sessions.value = all
    currentSessionId.value = current && all.some(s => s.id === current) ? current : ''
    persistSessions()
  }
  function registerActive(title: string) {
    const found = sessions.value.find(s => s.id === currentSessionId.value)
    if (!found) {
      sessions.value = [{ id: currentSessionId.value, title: title.slice(0, 30), updatedAt: new Date().toISOString() }, ...sessions.value]
    } else {
      found.title = found.title || title.slice(0, 30)
      found.updatedAt = new Date().toISOString()
    }
    persistSessions(); persistCurrent()
  }
  function touchSessionTitle(title: string) {
    const found = sessions.value.find(s => s.id === currentSessionId.value)
    if (found) {
      found.title = found.title || title.slice(0, 30)
      found.updatedAt = new Date().toISOString()
    }
    persistSessions()
  }
  function formatSessionDate(value: string) {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return '刚刚'
    const now = new Date()
    return date.toDateString() === now.toDateString()
      ? date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      : date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
  }
  function sessionLabel(s: SessionMeta) {
    return `${s.title || '未命名对话'} · ${formatSessionDate(s.updatedAt)}`
  }

  async function loadHistory(id: string) {
    messages.value = []
    if (!id) return
    try {
      const rows = await fetchRaw(`/qa/conversation/${id}`) as Array<{ id?: number; role?: string; content?: string }>
      for (const row of rows || []) {
        if (row.role === 'user' || row.role === 'assistant') {
          messages.value.push({ id: row.id, role: row.role, content: row.content || '' })
        }
      }
    } catch (e) { console.error('加载会话历史失败', e) }
    scrollToBottom()
  }

  function onSwitchSession(id: string) {
    if (streaming.value || id === '__empty__') return
    currentSessionId.value = id
    persistCurrent()
    clearContext()
    loadHistory(id)
  }
  function startNewSession() {
    if (streaming.value) return
    currentSessionId.value = ''
    messages.value = []
    query.value = ''
    persistCurrent()
    clearContext()
  }
  /** 仅清本地上下文展示，不请求后端（切换/新建会话时后端会以新 sessionId 重新开始） */
  function clearContext() { slots.value = {}; pending.value = null; pendingIndex.value = null }
  async function resetContext() {
    clearContext()
    if (!currentSessionId.value) return
    try {
      await fetchRaw(`/qa/session/${currentSessionId.value}/reset`, { method: 'POST' })
      ElMessage.success('已清空本次对话记住的预约条件')
    } catch (e) { console.error('清空会话上下文失败', e) }
  }

  async function recordFeedback(type: 'like' | 'dislike') {
    const id = lastAssistantMsgId.value
    if (id == null) return
    try {
      await fetchRaw('/qa/feedback', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sessionId: currentSessionId.value, messageId: id, feedback: type })
      })
      ElMessage.success(type === 'like' ? '感谢你的点赞' : '已记录反馈')
    } catch (e) {
      ElMessage.error((e as Error).message || '反馈提交失败，请重试')
    }
  }

  // ==================== 流式问答 ====================

  /** 结束流式：关连接、清看门狗、复位状态。正常结束 / 出错 / 超时 / 卸载共用一条收尾路径 */
  function stopStream(aiMsg?: ChatMsg, title?: string, hint?: string) {
    if (streamWatchdog) { clearTimeout(streamWatchdog); streamWatchdog = 0 }
    es?.close(); es = null
    streaming.value = false
    // 只有正常收尾才刷新会话标题：异常或超时时答案不完整，
    // 让会话停留在旧标题反而更利于用户回看
    if (title) touchSessionTitle(title)
    if (aiMsg) {
      if (hint) aiMsg.content += (aiMsg.content ? '\n\n' : '') + hint
      else if (!aiMsg.content) aiMsg.content = '（连接已中断，请重试）'
    }
    scrollToBottom()
  }

  function askQuestion() {
    const q = query.value.trim()
    if (!q || streaming.value) return
    if (!currentSessionId.value) currentSessionId.value = uuid()
    registerActive(q)
    streaming.value = true
    // 新一轮提问即作废上一张确认卡片（后端也会丢弃过期草稿）
    pendingIndex.value = null
    messages.value.push({ role: 'user', content: q })
    // 必须用 reactive 包裹：ref 数组 push 进去的若是普通对象，Vue 的响应式代理只在
    // "读取元素"时才套一层，直接改这个原始对象不会触发依赖——逐个 token 的追加就不会
    // 重渲染，表现是转圈半天后整段答案突然蹦出来（常被误报为"流式卡死"）。
    const aiMsg: ChatMsg = reactive({ role: 'assistant', content: '' })
    messages.value.push(aiMsg)
    query.value = ''
    scrollToBottom()

    const params = new URLSearchParams({ query: q, sessionId: currentSessionId.value, token: userStore.token })
    es = new EventSource(`${BASE}/qa/ask/stream?${params.toString()}`)
    // 看门狗：服务端既不返回也不关闭连接时（LLM 挂起、网关丢连接），
    // streaming 会永远为 true，输入框被永久锁死，只能刷新页面。
    streamWatchdog = window.setTimeout(() => {
      ElMessage.warning('响应超时，已中断本次生成，请重试')
      stopStream(aiMsg, q, '（响应超时（120 秒无数据），已中断，请重试）')
    }, SSE_WATCHDOG_MS)

    es.addEventListener('messageId', event => {
      const id = Number((event as MessageEvent).data)
      if (id) aiMsg.id = id
    })
    // 槽位更新：展示 AI 当前记住的预约条件
    es.addEventListener('slots', event => {
      try { slots.value = JSON.parse((event as MessageEvent).data) as SlotsView } catch { /* 忽略解析失败 */ }
    })
    // 待确认动作：渲染确认卡片
    es.addEventListener('confirm', event => {
      try {
        pending.value = JSON.parse((event as MessageEvent).data) as PendingView
        aiMsg.confirm = pending.value
        pendingIndex.value = messages.value.length - 1
        scheduleScroll()
      } catch { /* 忽略解析失败 */ }
    })
    // 动作回执：确认/取消执行完成，收起卡片
    es.addEventListener('action', event => {
      try { aiMsg.action = JSON.parse((event as MessageEvent).data) as ActionView } catch { /* 忽略解析失败 */ }
      pending.value = null
      pendingIndex.value = null
      slots.value = {}
    })
    es.onmessage = event => {
      if (event.data === '[DONE]') {
        if (!aiMsg.content) aiMsg.content = '（本次未生成内容，请换个问法试试）'
        stopStream(aiMsg, q); return
      }
      if (event.data.startsWith('[ERROR]')) {
        aiMsg.content += `\n\n${event.data.replace('[ERROR] ', '')}`
        stopStream(aiMsg, q); return
      }
      aiMsg.content += event.data
      scheduleScroll()
    }
    es.onerror = () => {
      if (!aiMsg.content) aiMsg.content = '连接失败，请确认后端服务已启动。'
      stopStream(aiMsg, q)
    }
  }

  /** 点确认卡片按钮 = 以对应话术发起新一轮提问，让后端走确定性确认分支 */
  function reply(text: string) {
    if (streaming.value) return
    query.value = text
    askQuestion()
  }

  // ==================== 知识库文档（管理员） ====================

  async function refreshDocuments() {
    try { documents.value = await fetchRaw('/documents?page=0&size=100') }
    catch (e) { console.error('获取文档失败', e) }
  }
  async function deleteDocument(doc: DocumentItem) {
    try {
      await fetchRaw(`/documents/${doc.id}`, { method: 'DELETE' })
      ElMessage.success('已删除')
      await refreshDocuments()
    } catch (e) { ElMessage.error((e as Error).message || '删除失败') }
  }
  function statusType(status?: string): 'success' | 'warning' | 'danger' | 'info' {
    const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
      READY: 'success', UPLOADED: 'info', PARSING: 'warning', CHUNKING: 'warning', EMBEDDING: 'warning', FAILED: 'danger'
    }
    return map[status || ''] || 'info'
  }
  function statusLabel(status?: string): string {
    const map: Record<string, string> = {
      READY: '就绪', UPLOADED: '已上传', PARSING: '解析中', CHUNKING: '分块中', EMBEDDING: '向量化中', FAILED: '失败'
    }
    return map[status || ''] || status || '未知'
  }
  /** 由 KnowledgeStrip 选中文件后调用；隐藏的 file input 由该组件自己持有 */
  async function uploadFile(file: File) {
    uploading.value = true
    try {
      const formData = new FormData()
      formData.append('file', file)
      const resp = await fetch(`${BASE}/documents/upload`, {
        method: 'POST', headers: authHeaders(), body: formData
      })
      const result = await resp.json()
      if (result.code === API_SUCCESS_CODE) {
        ElMessage.success('上传成功')
        setTimeout(refreshDocuments, 800)
      } else {
        ElMessage.error(result.message || '上传失败')
      }
    } catch { ElMessage.error('上传失败，请重试') }
    finally { uploading.value = false }
  }

  // ==================== 生命周期 ====================

  onMounted(async () => {
    // 分类中文名以后端字典为准（全局只拉一次，失败用兜底）
    void loadCategoryDictionary()
    if (isAdmin.value) refreshDocuments()
    ensureSession()
    await loadHistory(currentSessionId.value)
  })
  onUnmounted(() => {
    if (scrollFrame) cancelAnimationFrame(scrollFrame)
    // 关闭仍在进行的 SSE：否则后端会为已离开的用户继续生成完整回答（白耗 token），
    // 且连接一直挂着占用服务端线程与浏览器连接数
    stopStream()
  })

  return {
    // 状态
    isAdmin, query, messages, streaming,
    slots, pending, pendingIndex, slotChips,
    sessions, currentSessionId,
    documents, documentsVisible, uploading,
    chatBodyRef, canFeedback,
    // 会话
    onSwitchSession, startNewSession, sessionLabel, formatSessionDate, resetContext,
    // 问答
    askQuestion, reply, onBodyScroll, recordFeedback,
    // 文档
    refreshDocuments, deleteDocument, statusType, statusLabel, uploadFile
  }
}

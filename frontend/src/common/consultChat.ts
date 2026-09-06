import { ref } from 'vue'
import request from '@/common/utils/request'

/**
 * 咨询沟通（学生 ⇄ 教师 1:1 在线留言）API
 *
 * 会话粒度：一个学生与一位咨询教师 = 一条持续会话。
 * 接收方式：打开会话时全量拉取 + 聊天页 ~3s 轮询增量（afterId）。
 */

export interface ChatConversation {
  id: number
  peerUserId: number
  /** student / teacher（对端身份） */
  peerRole: 'student' | 'teacher'
  peerName: string
  lastMessage?: string | null
  lastTime?: string | null
  unreadCount: number
}

export interface ChatMessage {
  id: number
  senderId: number
  content: string
  createdAt?: string
  isMine: boolean
}

const BASE = '/app/chat/consult'

/** 我的会话列表 */
export async function fetchChatConversations(): Promise<ChatConversation[]> {
  return (await request.get(`${BASE}/conversations`)) as ChatConversation[]
}

/** 学生：从选咨询师卡片发起会话 */
export async function openChatWithConsultant(consultantId: number): Promise<ChatConversation> {
  return (await request.post(`${BASE}/conversations/open-with-consultant`, { consultantId })) as ChatConversation
}

/** 教师：对其名下咨询档期的学生发起会话 */
export async function openChatWithStudent(studentId: number): Promise<ChatConversation> {
  return (await request.post(`${BASE}/conversations/open-with-student`, { studentId })) as ChatConversation
}

/** 学生：凭自己的咨询预约单进入会话（我的预约详情） */
export async function openChatByBooking(orderId: number): Promise<ChatConversation> {
  return (await request.post(`${BASE}/conversations/open-by-booking`, { orderId })) as ChatConversation
}

/** 拉取会话消息（afterId 为空=全量；非空=增量轮询） */
export async function fetchChatMessages(conversationId: number, afterId?: number): Promise<ChatMessage[]> {
  const q = afterId != null ? `?afterId=${afterId}` : ''
  return (await request.get(`${BASE}/conversations/${conversationId}/messages${q}`)) as ChatMessage[]
}

/** 发送消息 */
export async function sendChatMessage(conversationId: number, content: string): Promise<ChatMessage> {
  return (await request.post(`${BASE}/conversations/${conversationId}/messages`, { content })) as ChatMessage
}

// ---------------------------------------------------------------------------
// 未读红点（模块级响应式，学生端/教师端导航共用）
// ---------------------------------------------------------------------------
const unreadTotal = ref(0)

/** 各导航页绑定此 ref 显示未读 */
export function useChatUnread() {
  return unreadTotal
}

/** 拉取我的未读总数（导航挂载/聊天动作后调用） */
export async function refreshChatUnread(): Promise<void> {
  try {
    const r = (await request.get(`${BASE}/conversations/unread-count`)) as unknown
    unreadTotal.value = Number(r ?? 0)
  } catch {
    unreadTotal.value = 0
  }
}

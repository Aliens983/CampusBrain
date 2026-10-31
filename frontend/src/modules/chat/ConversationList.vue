<template>
  <div class="conversations-page">
    <section class="page-hero page-hero--single">
      <p class="hz-eyebrow">
        Consultation messages · HZNU
      </p>
      <h1 class="page-hero__title">咨询消息</h1>
      <p class="page-hero__desc">
        与「教师咨询」中咨询过的老师在线沟通。仅咨询场景开放，其余服务不提供聊天。
      </p>
    </section>

    <el-card class="list-card">
      <div
        v-loading="loading"
        class="conv-list"
      >
        <el-empty
          v-if="!loading && list.length === 0"
          description="暂无会话。去「服务中心 → 教师咨询」选一位老师留言吧"
        >
          <el-button
            type="primary"
            plain
            @click="router.push('/services')"
          >
            去找咨询老师
          </el-button>
        </el-empty>

        <button
          v-for="c in list"
          :key="c.id"
          class="conv-row"
          @click="router.push({ path: threadRoute(c.id), query: { name: c.peerName } })"
        >
          <div class="conv-row__avatar">
            {{ c.peerName?.slice(0, 1) || (c.peerRole === 'teacher' ? '师' : '生') }}
          </div>
          <div class="conv-row__body">
            <div class="conv-row__head">
              <strong>{{ c.peerName || '未知用户' }}</strong>
              <span class="conv-row__role">{{ c.peerRole === 'teacher' ? '咨询教师' : '学生' }}</span>
            </div>
            <p class="conv-row__preview">
              {{ c.lastMessage || (c.unreadCount ? `有 ${c.unreadCount} 条未读消息` : '打个招呼吧') }}
            </p>
          </div>
          <div class="conv-row__side">
            <el-badge
              v-if="c.unreadCount > 0"
              :value="c.unreadCount"
              :max="99"
            />
            <span
              v-if="c.lastTime"
              class="conv-row__time"
            >{{ fmtTime(c.lastTime) }}</span>
          </div>
        </button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/common/stores/user'
import { fetchChatConversations, refreshChatUnread, type ChatConversation } from '@/common/consultChat'

const router = useRouter()
const userStore = useUserStore()
const list = ref<ChatConversation[]>([])
const loading = ref(false)

function threadRoute(id: number) {
  return userStore.isTeacher ? `/teacher/messages/${id}` : `/chat/${id}`
}

async function load() {
  loading.value = true
  try {
    list.value = await fetchChatConversations()
  } catch (error: unknown) {
    const err = error as { message?: string }
    // 请求层已 toast HTTP 错；这里仅兜底
    if (!(err as { isAxiosError?: boolean })?.isAxiosError && err.message) console.error(err.message)
  } finally {
    loading.value = false
  }
}

function fmtTime(t: string) {
  const d = new Date(t.replace('T', ' '))
  const today = new Date()
  const sameDay = d.toDateString() === today.toDateString()
  const hh = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  if (sameDay) return hh
  return `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${hh}`
}

onMounted(async () => {
  await load()
  refreshChatUnread()
})
</script>

<style scoped lang="scss">
.conversations-page {
  display: grid;
  gap: 20px;
}

.list-card {
  border-radius: var(--radius-lg);
}

.conv-list {
  display: grid;
  gap: 8px;
  min-height: 120px;
}

.conv-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface-2);
  text-align: left;
  cursor: pointer;
  font-family: var(--font-sans);
  color: var(--ink);
  transition:
    transform 0.16s var(--ease-out),
    border-color 0.16s ease,
    background-color 0.16s ease,
    box-shadow 0.16s ease;
}

.conv-row:hover {
  transform: translateY(-1px);
  border-color: rgba(176, 138, 69, 0.42);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.conv-row__avatar {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: var(--paper-2);
  color: var(--lake);
  font-family: var(--font-serif);
  font-size: 15px;
  font-weight: 600;
}

.conv-row__body {
  display: grid;
  flex: 1;
  min-width: 0;
  gap: 3px;
}

.conv-row__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.conv-row__head strong {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink);
}

.conv-row__role {
  padding: 0 6px;
  border: 1px solid var(--line);
  border-radius: 3px;
  font-size: 9.5px;
  letter-spacing: 0.04em;
  color: var(--ink-4);
}

.conv-row__preview {
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--ink-3);
}

.conv-row__side {
  display: grid;
  justify-items: end;
  gap: 4px;
  flex-shrink: 0;
}

.conv-row__time {
  font-family: var(--font-latin);
  font-size: 10px;
  color: var(--ink-4);
}
</style>

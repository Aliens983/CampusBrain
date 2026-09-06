<template>
  <div class="conversations-page">
    <section class="page-hero">
      <h1 class="page-hero__title">咨询消息</h1>
      <p class="muted">
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
  gap: 16px;
}
.page-hero {
  padding: 20px 26px;
  border-radius: 20px;
  color: #fff;
  background: linear-gradient(135deg, #0e6cd6, #3fb6ff 60%, #ade2ff);
  box-shadow: var(--shadow-card);
}
.page-hero h1 {
  margin: 0 0 6px;
  font-size: 22px;
}
.page-hero .muted {
  color: rgba(255, 255, 255, 0.85);
  margin: 0;
  font-size: 13px;
}
.list-card {
  border-radius: 18px;
  border: 1px solid var(--border-soft);
}
.conv-list {
  min-height: 120px;
  display: grid;
  gap: 8px;
}
.conv-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--border-soft);
  border-radius: 14px;
  background: linear-gradient(180deg, #fff, #f9fcff);
  cursor: pointer;
  text-align: left;
}
.conv-row:hover {
  border-color: rgba(63, 182, 255, 0.5);
  background: #f4faff;
}
.conv-row__avatar {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  font-weight: 700;
  background: linear-gradient(135deg, #1f6fb2, #3fb6ff);
}
.conv-row__body {
  flex: 1;
  min-width: 0;
  display: grid;
  gap: 3px;
}
.conv-row__head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.conv-row__role {
  font-size: 11px;
  color: var(--text-tertiary);
  border: 1px solid var(--border-soft);
  padding: 0 6px;
  border-radius: 6px;
}
.conv-row__preview {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conv-row__side {
  display: grid;
  justify-items: end;
  gap: 4px;
  flex-shrink: 0;
}
.conv-row__time {
  font-size: 11px;
  color: var(--text-tertiary);
}
</style>

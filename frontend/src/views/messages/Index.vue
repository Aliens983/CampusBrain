<template>
  <div class="page-shell">
    <section class="page-hero">
      <div class="page-hero__main">
        <span class="page-hero__chip">消息中心</span>
        <h1 class="page-hero__title">
          审批通知、系统公告与业务提醒
        </h1>
      </div>
      <div class="page-hero__panel">
        <div class="hero-panel__label">
          消息概览
        </div>
        <div class="hero-panel__item">
          <strong>{{ messages.length }}</strong><span>全部消息</span>
        </div>
        <div class="hero-panel__item">
          <strong>{{ unreadCount }}</strong><span>未读提醒</span>
        </div>
        <div class="hero-panel__item">
          <strong>{{ messageTypes }}</strong><span>消息类型</span>
        </div>
      </div>
    </section>

    <el-card class="panel-card">
      <template #header>
        <div class="section-head">
          <h3 class="section-head__title">
            消息流
          </h3>
          <el-segmented
            v-model="filter"
            :options="filters"
          />
        </div>
      </template>
      <div class="message-list">
        <article
          v-for="item in filteredMessages"
          :key="item.id"
          class="message-item"
          :class="{ 'is-unread': item.unread }"
          @click="selectedMessage = item"
        >
          <div class="message-item__glow" />
          <div class="message-item__main">
            <div class="message-item__head">
              <strong>{{ item.title }}</strong>
              <span
                class="status-pill"
                :class="item.unread ? 'is-brand' : 'is-success'"
              >{{ item.unread ? '未读' : '已读' }}</span>
            </div>
            <p>{{ item.content }}</p>
          </div>
          <div class="message-item__side">
            <el-tag effect="plain">
              {{ typeLabel(item.type) }}
            </el-tag>
            <span>{{ item.time }}</span>
          </div>
        </article>
      </div>
    </el-card>

    <el-drawer
      v-model="messageDrawerVisible"
      title="消息详情"
      size="420px"
    >
      <template v-if="selectedMessage">
        <div class="drawer-stack">
          <div>
            <h3>{{ selectedMessage.title }}</h3>
            <p class="muted">
              {{ selectedMessage.time }} / {{ typeLabel(selectedMessage.type) }}
            </p>
          </div>
          <div class="detail-card">
            <p>{{ selectedMessage.content }}</p>
          </div>
          <div class="button-row">
            <el-button
              plain
              @click="handleMessageAction(selectedMessage.type)"
            >
              前往相关页面
            </el-button>
            <el-button type="primary">
              标记已处理
            </el-button>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { MessageItem } from '@/types'

const router = useRouter()
const filter = ref<'all' | 'approval' | 'system' | 'notice'>('all')
const selectedMessage = ref<MessageItem | null>(null)
const messages = ref<MessageItem[]>([])
const filters = [
  { label: '全部', value: 'all' },
  { label: '审核', value: 'approval' },
  { label: '系统', value: 'system' },
  { label: '公告', value: 'notice' },
]

const unreadCount = computed(() => messages.value.filter((item) => item.unread).length)
const messageTypes = computed(() => new Set(messages.value.map((item) => item.type)).size)
const filteredMessages = computed(() => (filter.value === 'all' ? messages.value : messages.value.filter((item) => item.type === filter.value)))
const messageDrawerVisible = computed({
  get: () => Boolean(selectedMessage.value),
  set: (value: boolean) => {
    if (!value) selectedMessage.value = null
  },
})

function typeLabel(type: MessageItem['type']) {
  return { approval: '审核', system: '系统', notice: '公告' }[type]
}

function handleMessageAction(type: MessageItem['type']) {
  const pathMap = { approval: '/bookings', system: '/profile', notice: '/services' }
  router.push(pathMap[type])
}
</script>

<style scoped lang="scss">
.hero-panel__label { font-size: 13px; color: rgba(255,255,255,0.64); margin-bottom: 2px; }
.hero-panel__item { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.1); }
.hero-panel__item:last-child { border-bottom: none; }
.hero-panel__item strong { font-size: 20px; font-weight: 700; }
.hero-panel__item span { font-size: 13px; color: rgba(255,255,255,0.7); }
.message-list { display: grid; gap: 16px; }
.message-item { position: relative; display: flex; justify-content: space-between; gap: 20px; padding: 20px; border-radius: 20px; border: 1px solid var(--border-soft); background: linear-gradient(180deg, #fff, #f8fbff); overflow: hidden; cursor: pointer; transition: transform .24s ease, box-shadow .24s ease, border-color .24s ease; }
.message-item:hover { transform: translateY(-4px); box-shadow: 0 18px 28px rgba(20,33,61,.1); border-color: rgba(20,88,212,.14); }
.message-item.is-unread { border-color: rgba(20,88,212,.2); }
.message-item__glow { position: absolute; top: -24px; right: -24px; width: 88px; height: 88px; border-radius: 50%; background: radial-gradient(circle, rgba(20,88,212,.1), rgba(20,88,212,0)); transition: transform .28s ease; }
.message-item:hover .message-item__glow { transform: scale(1.18); }
.message-item__main, .message-item__side { position: relative; z-index: 1; }
.message-item__main { flex: 1; }
.message-item__head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 8px; }
.message-item__main p { margin: 0; color: var(--text-secondary); line-height: 1.75; }
.message-item__side { display: grid; justify-items: end; align-content: start; gap: 8px; color: var(--text-tertiary); font-size: 12px; }
.dialog-list, .drawer-stack { display: grid; gap: 12px; }
.dialog-card, .detail-card { padding: 16px; border-radius: 18px; background: linear-gradient(180deg, #fff, #f8fbff); border: 1px solid var(--border-soft); }
.detail-card p { margin: 0; line-height: 1.8; color: var(--text-secondary); }
.button-row { display: flex; gap: 10px; }
@media (max-width: 900px) { .message-item, .button-row { flex-direction: column; } .message-item__side { justify-items: start; } }
</style>

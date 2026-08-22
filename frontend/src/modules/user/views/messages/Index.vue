<template>
  <div class="page-shell">
    <section class="page-hero">
      <div>
        <div class="status-pill is-brand">
          消息中心
        </div>
        <h1 class="page-hero__title">
          审批进度与系统通知统一收件箱
        </h1>
        <p class="page-hero__desc">
          按类型、时间轴和状态聚合展示通知，支持快速定位和标记已读。
        </p>
      </div>
    </section>
    <el-card class="panel-card">
      <template #header>
        <div class="section-head">
          <h3 class="section-head__title">
            通知列表
          </h3>
        </div>
      </template>
      <div class="messages-stack">
        <article
          v-for="item in messages"
          :key="item.id"
          class="message-item"
          :class="{ unread: item.unread }"
        >
          <div
            class="message-item__icon"
            :class="item.type"
          >
            {{ item.type === 'approval' ? '审' : item.type === 'system' ? '系' : '通' }}
          </div>
          <div class="message-item__content">
            <strong>{{ item.title }}</strong>
            <p>{{ item.content }}</p>
            <time>{{ item.time }}</time>
          </div>
        </article>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const messages = ref([
  { id: '1', type: 'approval', title: '会议室预约已通过', content: '您申请的 A栋 201 会议室 已通过审核，请准时到达。', time: '10分钟前', read: false, unread: true },
  { id: '2', type: 'system', title: '系统维护通知', content: '平台将于本周六 22:00-24:00 进行系统维护，届时部分功能暂停使用。', time: '2小时前', read: false, unread: true },
  { id: '3', type: 'notice', title: '设备借用即将到期', content: '您借用的投影仪 A1 请于明天 17:00 前归还。', time: '1天前', read: true, unread: false },
])
</script>

<style scoped lang="scss">
.messages-stack { display: grid; gap: 12px; }
.message-item { display: flex; gap: 14px; padding: 16px; border-radius: 14px; border: 1px solid var(--border-soft); background: linear-gradient(180deg, #fff, #f8fbff); transition: transform .2s ease; }
.message-item:hover { transform: translateX(4px); }
.message-item.unread { border-left: 3px solid #1458d4; }
.message-item__icon { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 14px; flex-shrink: 0; }
.approval { background: linear-gradient(135deg, #fef3c7, #fde68a); color: #d97706; }
.system { background: linear-gradient(135deg, #e0f2fe, #bae6fd); color: #0284c7; }
.notice { background: linear-gradient(135deg, #ccfbf1, #99f6e4); color: #0d9488; }
.message-item__content { flex: 1; }
.message-item__content strong { font-size: 14px; }
.message-item__content p { margin: 4px 0; color: var(--text-secondary); font-size: 13px; }
.message-item__content time { font-size: 11px; color: var(--text-secondary); }
</style>
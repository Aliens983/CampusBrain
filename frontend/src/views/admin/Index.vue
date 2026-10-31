<template>
  <div class="admin-page">
    <section class="admin-hero">
      <div class="admin-hero__main">
        <p class="hz-eyebrow">
          Admin overview · HZNU
        </p>
        <h1>管理概览</h1>
        <p class="admin-hero__desc">
          平台运行状态、待办事项与常用管理入口。
        </p>
      </div>

      <div class="admin-hero__signal">
        <div
          class="signal-card"
          @click="router.push('/admin/bookings')"
        >
          <span>待审核</span>
          <strong>{{ adminSummary.pendingBookings }}</strong>
          <small>前往预约审核处理</small>
        </div>
      </div>
    </section>

    <section class="admin-metrics">
      <article
        v-for="item in metrics"
        :key="item.label"
        class="metric-panel"
        :class="{ 'is-static': !item.path }"
        @click="item.path && router.push(item.path)"
      >
        <span>{{ item.label }}</span>
        <strong>{{ adminSummary[item.field] }}</strong>
        <small>{{ item.small }}</small>
      </article>
    </section>

    <el-card class="panel-card">
      <template #header>
        <div class="section-head">
          <h3 class="section-head__title">
            快捷操作
          </h3>
        </div>
      </template>
      <div class="quick-grid">
        <button
          v-for="item in quickActions"
          :key="item.title"
          class="quick-box"
          @click="router.push(item.path)"
        >
          <strong>{{ item.title }}</strong>
          <span>{{ item.desc }}</span>
        </button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchAdminSummary } from '@/common/campus'
import type { AdminSummary } from '@/common/types'

type MetricField = keyof Pick<AdminSummary, 'totalUsers' | 'totalServices' | 'activeBookings' | 'approvalRate'>

const router = useRouter()
const adminSummary = ref<AdminSummary>({
  totalUsers: 0,
  totalServices: 0,
  activeBookings: 0,
  approvalRate: '—',
  pendingBookings: 0,
})
onMounted(async () => {
  try {
    adminSummary.value = await fetchAdminSummary()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '获取管理数据失败')
  }
})

const metrics: Array<{ label: string; field: MetricField; small: string; path: string }> = [
  { label: '平台用户数', field: 'totalUsers', small: '已注册账号总数', path: '/admin/users' },
  { label: '服务模块数', field: 'totalServices', small: '已纳管业务域', path: '/admin/services' },
  { label: '进行中预约', field: 'activeBookings', small: '待审与进行中的申请', path: '/admin/bookings' },
  { label: '审批通过率', field: 'approvalRate', small: '全部申请通过占比', path: '' },
]

const quickActions = [
  { title: '服务治理', desc: '维护服务目录与开放状态', path: '/admin/services' },
  { title: '预约审核', desc: '集中处理待审申请', path: '/admin/bookings' },
  { title: '用户与权限', desc: '账号、角色与授权管理', path: '/admin/users' },
  { title: '系统设置', desc: '预约规则与通知策略', path: '/admin/system' },
]
</script>

<style scoped lang="scss">
.admin-page {
  display: grid;
  gap: 20px;
}

.admin-metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.metric-panel {
  position: relative;
  display: grid;
  gap: 10px;
  padding: 22px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  background: var(--surface);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.18s var(--ease-out), box-shadow 0.18s var(--ease-out);
}

.metric-panel::after {
  content: "";
  position: absolute;
  inset: auto -26px -26px auto;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(176, 138, 69, 0.07), rgba(176, 138, 69, 0));
  pointer-events: none;
}

.metric-panel:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card-hover);
}

/* 无对应落地页的指标（如通过率）不显示可点样式 */
.metric-panel.is-static {
  cursor: default;
}

.metric-panel.is-static:hover {
  transform: none;
  box-shadow: var(--shadow-card);
}

.metric-panel span,
.metric-panel small {
  color: var(--text-secondary);
}

.metric-panel strong {
  font-family: var(--font-serif);
  font-size: 34px;
  font-weight: 600;
  line-height: 1;
  color: var(--ink);
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.quick-box {
  display: grid;
  gap: 10px;
  padding: 20px 22px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--surface-2);
  font-family: var(--font-sans);
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.18s var(--ease-out),
    border-color 0.18s ease,
    background-color 0.18s ease;
}

.quick-box:hover {
  transform: translateY(-2px);
  border-color: rgba(176, 138, 69, 0.42);
  background: var(--surface);
}

.quick-box strong {
  font-family: var(--font-serif);
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
}

.quick-box span {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 1200px) {
  .admin-metrics {
    grid-template-columns: repeat(2, 1fr);
  }

  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 760px) {
  .admin-metrics {
    grid-template-columns: 1fr;
  }

  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>

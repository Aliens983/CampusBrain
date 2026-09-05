<template>
  <div class="admin-page">
    <section class="admin-hero">
      <div class="admin-hero__main">
        <h1>管理概览</h1>
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
import { fetchAdminSummary } from '@/services/campus'
import type { AdminSummary } from '@/types'

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

.admin-hero {
  position: relative;
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 20px;
  padding: 32px;
  border-radius: 30px;
  color: #fff;
  background: linear-gradient(135deg, #0f172a, #132949 55%, #7c3aed);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.admin-hero::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 18% 20%, rgba(255, 255, 255, 0.12), transparent 18%),
    linear-gradient(140deg, transparent 14%, rgba(255, 255, 255, 0.08) 42%, transparent 72%);
  pointer-events: none;
}

.admin-hero::after {
  content: "";
  position: absolute;
  inset: -30% -6% auto auto;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.24), rgba(139, 92, 246, 0));
  animation: adminGlow 8s ease-in-out infinite;
  pointer-events: none;
}

.admin-hero__main,
.admin-hero__signal {
  position: relative;
  z-index: 1;
}

.hero-chip {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.08em;
  background: rgba(255, 255, 255, 0.12);
}

.admin-hero h1 {
  margin: 12px 0 10px;
  font-size: 36px;
  line-height: 1.18;
}

.admin-hero p {
  max-width: 740px;
  margin: 0;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.82);
}

.admin-hero__signal {
  display: grid;
  gap: 12px;
  align-content: center;
}

/* 与其它管理页 hero 右侧的 signal-card 保持一致 */
.signal-card {
  display: grid;
  gap: 4px;
  padding: 16px 18px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  cursor: pointer;
  transition: background 0.2s;
}

.signal-card:hover {
  background: rgba(255, 255, 255, 0.14);
}

.signal-card span {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.64);
}

.signal-card strong {
  font-size: 26px;
  font-weight: 700;
}

.signal-card small {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
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
  border-radius: 22px;
  border: 1px solid var(--border-soft);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.26s ease, box-shadow 0.26s ease;
}

.metric-panel::after {
  content: "";
  position: absolute;
  inset: auto -26px -26px auto;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(124, 58, 237, 0.08), rgba(124, 58, 237, 0));
  pointer-events: none;
}

.metric-panel:hover {
  transform: translateY(-6px);
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
  font-size: 34px;
  line-height: 1;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.quick-box {
  display: grid;
  gap: 10px;
  text-align: left;
  padding: 20px 22px;
  border: 1px solid var(--border-soft);
  border-radius: 18px;
  background: linear-gradient(180deg, #fff, #fbf9ff);
  cursor: pointer;
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
}

.quick-box:hover {
  transform: translateY(-4px);
  border-color: rgba(124, 58, 237, 0.18);
  box-shadow: 0 16px 28px rgba(20, 33, 61, 0.1);
}

.quick-box strong {
  font-size: 15px;
  font-weight: 600;
}

.quick-box span {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

@keyframes adminGlow {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(-20px, 22px, 0) scale(1.12);
  }
}

@media (max-width: 1200px) {
  .admin-hero {
    grid-template-columns: 1fr;
  }

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

<template>
  <div class="dashboard-page">
    <section class="dashboard-hero">
      <div class="dashboard-hero__main">
        <span class="hero-chip">User Portal</span>
        <h1>今天的预约事务，在一个首页里处理清楚</h1>
        <p>
          用户端首页被重构为更正式的门户工作台，重点聚合服务入口、个人日程、审批反馈和最近预约，
          让普通用户进入系统后能更快完成查找、提交、跟进和回看。
        </p>
        <div class="hero-actions">
          <el-button
            type="primary"
            size="large"
            @click="router.push('/services')"
          >
            发起预约
          </el-button>
          <el-button
            size="large"
            plain
            @click="router.push('/bookings')"
          >
            查看我的预约
          </el-button>
        </div>
      </div>

      <div class="dashboard-hero__panel">
        <div class="hero-panel__label">
          今日安排
        </div>
        <div
          class="hero-panel__item"
          @click="router.push('/bookings/401')"
        >
          <strong>14:00 - 16:00</strong>
          <span>创新中心 A301 项目评审会，提前 15 分钟签到。</span>
        </div>
        <div
          class="hero-panel__item"
          @click="router.push('/equipment')"
        >
          <strong>18:00 前</strong>
          <span>归还 4K 投影仪，并完成借用确认闭环。</span>
        </div>
      </div>
    </section>

    <section class="metric-grid">
      <article
        v-for="stat in dashboardStats"
        :key="stat.label"
        class="metric-card"
        @click="openMetricDetail(stat.label)"
      >
        <span class="metric-card__label">{{ stat.label }}</span>
        <strong class="metric-card__value">{{ stat.value }}</strong>
        <span
          class="metric-card__trend status-pill"
          :class="toneClass(stat.tone)"
        >{{ stat.trend }}</span>
      </article>
    </section>

    <section class="dashboard-grid">
      <div class="dashboard-grid__main">
        <el-card class="surface-card">
          <template #header>
            <div class="card-head">
              <div>
                <h3>快捷入口</h3>
                <p>把最常用的预约动作放在首页第一屏，减少路径跳转。</p>
              </div>
            </div>
          </template>
          <div class="shortcut-grid">
            <button
              v-for="item in shortcuts"
              :key="item.title"
              class="shortcut-card"
              @click="router.push(item.path)"
            >
              <div class="shortcut-card__orb" />
              <div
                class="shortcut-card__icon"
                :class="item.tone"
              >
                {{ item.abbr }}
              </div>
              <strong>{{ item.title }}</strong>
              <span>{{ item.desc }}</span>
            </button>
          </div>
        </el-card>

        <el-card class="surface-card">
          <template #header>
            <div class="card-head">
              <div>
                <h3>常用服务</h3>
                <p>保留 CAS 的业务结构，同时用更像企业产品的卡片布局呈现。</p>
              </div>
              <el-button
                text
                @click="router.push('/services')"
              >
                全部服务
              </el-button>
            </div>
          </template>
          <div class="service-grid">
            <article
              v-for="service in services"
              :key="service.id"
              class="service-card"
              @click="activeService = service"
            >
              <div
                class="service-card__cover"
                :class="service.image"
              >
                {{ service.code }}
              </div>
              <div class="service-card__content">
                <div class="service-card__head">
                  <strong>{{ service.name }}</strong>
                  <el-tag :type="service.status === 'available' ? 'success' : 'warning'">
                    {{ service.status === 'available' ? '可预约' : '维护中' }}
                  </el-tag>
                </div>
                <p>{{ service.description }}</p>
                <div class="service-card__meta">
                  <span>{{ service.category }}</span>
                  <span>{{ service.priceLabel }}</span>
                </div>
              </div>
            </article>
          </div>
        </el-card>
      </div>

      <div class="dashboard-grid__side">
        <el-card class="surface-card">
          <template #header>
            <div class="card-head">
              <div>
                <h3>待处理反馈</h3>
                <p>今天最值得优先关注的事项。</p>
              </div>
            </div>
          </template>
          <div class="todo-stack">
            <div
              v-for="item in todoList"
              :key="item.title"
              class="todo-card"
              @click="router.push(item.path)"
            >
              <div class="todo-card__main">
                <strong>{{ item.title }}</strong>
                <p>{{ item.desc }}</p>
              </div>
              <span
                class="todo-card__badge status-pill"
                :class="item.tone"
              >{{ item.badge }}</span>
            </div>
          </div>
        </el-card>

        <el-card class="surface-card">
          <template #header>
            <div class="card-head">
              <div>
                <h3>最近预约</h3>
                <p>审批进度和时间安排随时回看。</p>
              </div>
            </div>
          </template>
          <div class="booking-stack">
            <div
              v-for="item in bookings"
              :key="item.id"
              class="booking-card"
              @click="router.push(`/bookings/${item.id}`)"
            >
              <div>
                <strong>{{ item.serviceName }}</strong>
                <p>{{ item.date }} {{ item.timeRange }}</p>
              </div>
              <el-tag :type="bookingTag(item.status)">
                {{ statusText(item.status) }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </div>
    </section>

    <el-dialog
      v-model="metricDialogVisible"
      :title="metricDialogTitle"
      width="560px"
    >
      <div class="detail-dialog__body">
        <div
          v-for="item in metricDialogItems"
          :key="item"
          class="detail-dialog__item"
        >
          {{ item }}
        </div>
      </div>
    </el-dialog>

    <el-drawer
      v-model="serviceDrawerVisible"
      title="服务速览"
      size="420px"
    >
      <template v-if="activeService">
        <div class="drawer-stack">
          <div
            class="cover-badge"
            :class="activeService.image"
          >
            {{ activeService.code }}
          </div>
          <div>
            <h3>{{ activeService.name }}</h3>
            <p class="muted">
              {{ activeService.description }}
            </p>
          </div>
          <div class="info-list">
            <div class="info-row">
              <span>业务类别</span><strong>{{ activeService.category }}</strong>
            </div>
            <div class="info-row">
              <span>服务范围</span><strong>{{ activeService.location }}</strong>
            </div>
            <div class="info-row">
              <span>使用说明</span><strong>{{ activeService.priceLabel }}</strong>
            </div>
          </div>
          <div class="tag-wrap">
            <el-tag
              v-for="tag in activeService.tags"
              :key="tag"
              round
            >
              {{ tag }}
            </el-tag>
          </div>
          <el-button
            type="primary"
            @click="router.push(`/service/${activeService.id}`)"
          >
            查看完整详情
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchBookingRecords, fetchServiceCards } from '@/services/campus'
import type { BookingRecord, BookingStatus, DashboardStat, ServiceCard } from '@/types'

const router = useRouter()
const metricDialogVisible = ref(false)
const metricDialogTitle = ref('')
const metricDialogItems = ref<string[]>([])
const activeService = ref<ServiceCard | null>(null)
const bookings = ref<BookingRecord[]>([])
const services = ref<ServiceCard[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const [bookingData, serviceData] = await Promise.all([fetchBookingRecords(), fetchServiceCards()])
    bookings.value = bookingData
    services.value = serviceData
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '获取数据失败')
  } finally {
    loading.value = false
  }
})

const shortcuts = [
  { title: '会议室预约', desc: '空间资源与时段申请', path: '/rooms', abbr: 'MR', tone: 'tone-blue' },
  { title: '设备借用', desc: '借还流程与库存状态', path: '/equipment', abbr: 'EQ', tone: 'tone-teal' },
  { title: '咨询服务', desc: '顾问资源与时间排班', path: '/consultation', abbr: 'CS', tone: 'tone-amber' },
  { title: '我的预约', desc: '记录查询与审批进度', path: '/bookings', abbr: 'BK', tone: 'tone-slate' },
]

const todoList = [
  { title: '设备借用待确认', desc: '4K 投影仪借用申请已提交，等待设备中心确认。', badge: '处理中', tone: 'is-warning', path: '/bookings/402' },
  { title: '咨询时段即将开放', desc: '就业指导周三新增晚间时段，建议尽快锁定。', badge: '可预约', tone: 'is-brand', path: '/consultation' },
  { title: '会议室使用提醒', desc: '今天 13:45 前到场签到，避免预约被自动释放。', badge: '即将开始', tone: 'is-danger', path: '/rooms' },
]

const serviceDrawerVisible = computed({
  get: () => Boolean(activeService.value),
  set: (value: boolean) => {
    if (!value) activeService.value = null
  },
})

const dashboardStats = computed<DashboardStat[]>(() => {
  const total = bookings.value.length
  const pending = bookings.value.filter(b => b.status === 'pending').length
  const completed = bookings.value.filter(b => b.status === 'completed').length

  return [
    { label: '本月预约量', value: String(total), trend: total > 0 ? '正增长' : '暂无数据', tone: total > 0 ? 'success' : 'warning' },
    { label: '待审核事项', value: String(pending), trend: pending > 0 ? '待处理' : '已清空', tone: pending > 0 ? 'warning' : 'success' },
    { label: '已完成', value: String(completed), trend: completed > 0 ? '已完成' : '暂无', tone: 'brand' },
    { label: '资源完单率', value: total > 0 ? `${Math.round((completed / total) * 100)}%` : '0%', trend: '本月表现', tone: 'brand' },
  ]
})

function openMetricDetail(label: string) {
  const mapping: Record<string, string[]> = {
    本月预约量: ['会议室预约 612 单', '设备借用 384 单', '咨询服务 290 单'],
    待审核事项: ['设备借用待审核 9 条', '会议室申请待审核 8 条', '咨询申请待审核 6 条'],
    资源完单率: ['会议室按时完结率 98.1%', '设备归还闭环率 95.3%', '咨询履约率 96.2%'],
    异常工单: ['会议室超时释放 2 条', '设备库存差异 3 条', '通知补发记录 2 条'],
  }
  metricDialogTitle.value = label
  metricDialogItems.value = mapping[label] || ['暂无明细']
  metricDialogVisible.value = true
}

function toneClass(tone: DashboardStat['tone']) {
  return { brand: 'is-brand', success: 'is-success', warning: 'is-warning', danger: 'is-danger' }[tone]
}

function bookingTag(status: BookingStatus) {
  return { pending: 'warning', approved: 'success', rejected: 'danger', completed: 'info', cancelled: 'info' }[status]
}

function statusText(status: BookingStatus) {
  return { pending: '待审核', approved: '已通过', rejected: '已驳回', completed: '已完成', cancelled: '已取消' }[status]
}
</script>

<style scoped lang="scss">
.dashboard-page {
  display: grid;
  gap: 20px;
}

.dashboard-hero {
  position: relative;
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 20px;
  padding: 32px;
  border-radius: 30px;
  color: #fff;
  background: linear-gradient(135deg, #0e2647, #1458d4 62%, #52a1ff);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.dashboard-hero::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 20%, rgba(255, 255, 255, 0.16), transparent 22%),
    linear-gradient(120deg, transparent 14%, rgba(255, 255, 255, 0.08) 36%, transparent 62%);
}

.dashboard-hero::after {
  content: "";
  position: absolute;
  inset: auto -80px -80px auto;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.22), rgba(255, 255, 255, 0));
  animation: dashboardHalo 8s ease-in-out infinite;
}

.dashboard-hero__main,
.dashboard-hero__panel {
  position: relative;
  z-index: 1;
}

.hero-chip {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.08em;
  background: rgba(255, 255, 255, 0.14);
}

.dashboard-hero h1 {
  margin: 16px 0 10px;
  font-size: 38px;
  line-height: 1.15;
}

.dashboard-hero p {
  max-width: 720px;
  margin: 0;
  line-height: 1.82;
  color: rgba(255, 255, 255, 0.84);
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.dashboard-hero__panel {
  display: grid;
  gap: 12px;
  padding: 22px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
}

.hero-panel__label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.hero-panel__item {
  display: grid;
  gap: 6px;
  padding: 15px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.08);
  cursor: pointer;
  transition: transform 0.24s ease, background-color 0.24s ease, border-color 0.24s ease;
  border: 1px solid transparent;
}

.hero-panel__item:hover {
  transform: translateX(6px);
  background: rgba(255, 255, 255, 0.14);
  border-color: rgba(255, 255, 255, 0.12);
}

.hero-panel__item span {
  color: rgba(255, 255, 255, 0.82);
  line-height: 1.6;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.metric-card {
  position: relative;
  display: grid;
  gap: 12px;
  padding: 22px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid var(--border-soft);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.26s ease, box-shadow 0.26s ease;
}

.metric-card::after {
  content: "";
  position: absolute;
  inset: auto -30px -30px auto;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(20, 88, 212, 0.08), rgba(20, 88, 212, 0));
}

.metric-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-card-hover);
}

.metric-card__label {
  color: var(--text-secondary);
  font-size: 13px;
}

.metric-card__value {
  font-size: 34px;
  line-height: 1;
}

.metric-card__trend {
  justify-self: start;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1.35fr 0.85fr;
  gap: 20px;
}

.dashboard-grid__main,
.dashboard-grid__side {
  display: grid;
  gap: 20px;
}

.surface-card {
  border: 1px solid var(--border-soft);
  border-radius: 24px;
  box-shadow: var(--shadow-card);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.card-head h3,
.card-head p {
  margin: 0;
}

.card-head p {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 13px;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.shortcut-card {
  position: relative;
  display: grid;
  gap: 12px;
  padding: 18px;
  text-align: left;
  border: 1px solid var(--border-soft);
  border-radius: 20px;
  background: linear-gradient(180deg, #fff, #f8fbff);
  cursor: pointer;
  overflow: hidden;
  transition: transform 0.28s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.28s ease, border-color 0.28s ease;
}

.shortcut-card:hover {
  transform: translateY(-8px);
  border-color: rgba(20, 88, 212, 0.16);
  box-shadow: 0 22px 36px rgba(20, 33, 61, 0.12);
}

.shortcut-card__orb {
  position: absolute;
  top: -24px;
  right: -24px;
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(20, 88, 212, 0.1), rgba(20, 88, 212, 0));
  transition: transform 0.28s ease;
}

.shortcut-card:hover .shortcut-card__orb {
  transform: scale(1.18);
}

.shortcut-card strong,
.shortcut-card span,
.shortcut-card__icon {
  position: relative;
  z-index: 1;
}

.shortcut-card strong {
  font-size: 16px;
}

.shortcut-card span {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.shortcut-card__icon {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  color: #fff;
  font-weight: 700;
  transition: transform 0.28s ease;
}

.shortcut-card:hover .shortcut-card__icon {
  transform: scale(1.08) rotate(-6deg);
}

.tone-blue {
  background: linear-gradient(135deg, #1458d4, #4c98ff);
}

.tone-teal {
  background: linear-gradient(135deg, #0f766e, #2dd4bf);
}

.tone-amber {
  background: linear-gradient(135deg, #b45309, #f6ad55);
}

.tone-slate {
  background: linear-gradient(135deg, #334155, #94a3b8);
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.service-card {
  overflow: hidden;
  border: 1px solid var(--border-soft);
  border-radius: 22px;
  background: linear-gradient(180deg, #fff, #f8fbff);
  cursor: pointer;
  transition: transform 0.28s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.28s ease;
}

.service-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 22px 36px rgba(20, 33, 61, 0.12);
}

.service-card__cover {
  min-height: 90px;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 700;
  position: relative;
  overflow: hidden;
}

.service-card__cover::after {
  content: "";
  position: absolute;
  inset: -40% auto auto -10%;
  width: 40%;
  height: 200%;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.28), rgba(255, 255, 255, 0));
  transform: rotate(24deg);
  animation: serviceLight 6.2s ease-in-out infinite;
}

.service-card__content {
  padding: 18px;
}

.service-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.service-card__content p {
  min-height: 44px;
  color: var(--text-secondary);
  line-height: 1.7;
}

.service-card__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--text-tertiary);
  font-size: 12px;
}

.todo-stack,
.booking-stack {
  display: grid;
  gap: 14px;
}

.todo-card,
.booking-card {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--border-soft);
  background: linear-gradient(180deg, #fff, #f8fbff);
  cursor: pointer;
  transition: transform 0.24s ease, box-shadow 0.24s ease;
}

.todo-card:hover,
.booking-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 18px 28px rgba(20, 33, 61, 0.1);
}

.todo-card p,
.booking-card p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.todo-card__badge {
  height: fit-content;
}

.detail-dialog__body,
.drawer-stack {
  display: grid;
  gap: 12px;
}

.detail-dialog__item {
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(180deg, #fff, #f8fbff);
  border: 1px solid var(--border-soft);
}

.tag-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@keyframes dashboardHalo {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(-22px, -14px, 0) scale(1.1);
  }
}

@keyframes serviceLight {
  0%,
  100% {
    transform: translateX(-10%) rotate(24deg);
  }
  50% {
    transform: translateX(180%) rotate(24deg);
  }
}

@media (max-width: 1200px) {
  .metric-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .dashboard-grid,
  .dashboard-hero {
    grid-template-columns: 1fr;
  }

  .shortcut-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 760px) {
  .metric-grid,
  .shortcut-grid,
  .service-grid {
    grid-template-columns: 1fr;
  }

  .hero-actions,
  .todo-card,
  .booking-card {
    flex-direction: column;
  }
}
</style>
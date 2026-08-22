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
          v-for="item in todayBookings"
          :key="item.id"
          class="hero-panel__item"
          @click="router.push(`/bookings/${item.id}`)"
        >
          <strong>{{ item.timeRange }}</strong>
          <span>{{ item.serviceName }} / {{ item.location }}</span>
        </div>
        <div
          v-if="todayBookings.length === 0"
          class="hero-panel__item"
        >
          <strong>暂无安排</strong>
          <span>今天没有预约事务</span>
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
              v-for="item in recentBookings"
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
import { fetchBookingRecords, fetchServiceCards } from '@/common/campus'
import type { BookingRecord, BookingStatus, DashboardStat, ServiceCard } from '@/common/types'

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

const todayBookings = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return bookings.value.filter((b: BookingRecord) => b.date === today).slice(0, 3)
})

const recentBookings = computed(() => bookings.value.slice(0, 5))

const dashboardStats = computed<DashboardStat[]>(() => {
  const total = bookings.value.length
  const pending = bookings.value.filter((b: BookingRecord) => b.status === 'pending').length
  const completed = bookings.value.filter((b: BookingRecord) => b.status === 'completed').length

  return [
    { label: '本月预约量', value: String(total), trend: total > 0 ? '正增长' : '暂无数据', tone: total > 0 ? 'success' : 'warning' },
    { label: '待审核事项', value: String(pending), trend: pending > 0 ? '待处理' : '已清空', tone: pending > 0 ? 'warning' : 'success' },
    { label: '已完成', value: String(completed), trend: completed > 0 ? '已完成' : '暂无', tone: 'brand' },
    { label: '资源完单率', value: total > 0 ? `${Math.round((completed / total) * 100)}%` : '0%', trend: '本月表现', tone: 'brand' },
  ]
})

const shortcuts = [
  { title: '会议室预约', desc: '空间资源与时段申请', path: '/rooms', abbr: 'MR', tone: 'tone-blue' },
  { title: '设备借用', desc: '借还流程与库存状态', path: '/equipment', abbr: 'EQ', tone: 'tone-teal' },
  { title: '咨询服务', desc: '顾问资源与时间排班', path: '/consultation', abbr: 'CS', tone: 'tone-amber' },
  { title: '我的预约', desc: '记录查询与审批进度', path: '/bookings', abbr: 'BK', tone: 'tone-slate' },
]

const todoList = computed(() => {
  const pendingBookings = bookings.value.filter((b: BookingRecord) => b.status === 'pending')
  return [
    { title: '待审核预约', desc: `${pendingBookings.length} 条预约申请等待审核，请及时关注。`, badge: pendingBookings.length > 0 ? '待处理' : '已清空', tone: pendingBookings.length > 0 ? 'is-warning' : 'is-success', path: '/bookings' },
    { title: '咨询时段提醒', desc: '就业指导新增晚间时段，建议尽快锁定。', badge: '可预约', tone: 'is-brand', path: '/consultation' },
  ]
})

const serviceDrawerVisible = computed({
  get: () => Boolean(activeService.value),
  set: (value: boolean) => {
    if (!value) activeService.value = null
  },
})

function openMetricDetail(label: string) {
  const mapping: Record<string, string[]> = {
    本月预约量: [`总预约 ${bookings.value.length} 单`, `已完成 ${bookings.value.filter(b => b.status === 'completed').length} 单`, `进行中 ${bookings.value.filter(b => b.status === 'approved').length} 单`],
    待审核事项: [`${bookings.value.filter(b => b.status === 'pending').length} 条待审核`, '请及时关注审批进度'],
    已完成: [`本月完成 ${bookings.value.filter(b => b.status === 'completed').length} 单`],
    资源完单率: ['根据实际预约完成情况统计', '持续优化使用体验'],
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

.dashboard-hero__main h1 {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.3;
  margin: 12px 0 10px;
}

.dashboard-hero__main p {
  font-size: 14px;
  opacity: 0.85;
  line-height: 1.6;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.dashboard-hero__panel {
  display: grid;
  gap: 12px;
  padding: 24px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.08);
}

.hero-panel__label {
  font-size: 13px;
  opacity: 0.7;
  margin-bottom: 4px;
}

.hero-panel__item {
  padding: 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.06);
  cursor: pointer;
  transition: background 0.2s;
}

.hero-panel__item:hover {
  background: rgba(255, 255, 255, 0.12);
}

.hero-panel__item strong {
  display: block;
  font-size: 14px;
  margin-bottom: 4px;
}

.hero-panel__item span {
  font-size: 12px;
  opacity: 0.7;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.metric-card {
  position: relative;
  display: grid;
  gap: 8px;
  padding: 22px;
  border-radius: 22px;
  background: rgba(255,255,255,.92);
  border: 1px solid var(--border-soft);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  cursor: pointer;
  transition: transform .26s ease, box-shadow .26s ease;
}

.metric-card::after {
  content: '';
  position: absolute;
  inset: auto -24px -24px auto;
  width: 92px;
  height: 92px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(20,88,212,.08), rgba(20,88,212,0));
}

.metric-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-card-hover);
}

.metric-card__label {
  font-size: 13px;
  color: var(--text-secondary);
}

.metric-card__value {
  font-size: 34px;
  font-weight: 700;
  color: var(--text-primary);
}

.metric-card__trend {
  font-size: 12px;
  width: fit-content;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 20px;
}

.dashboard-grid__main,
.dashboard-grid__side {
  display: grid;
  gap: 20px;
  align-content: start;
}

.surface-card {
  border-radius: 24px;
  box-shadow: var(--shadow-card);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-head h3 {
  font-size: 17px;
  font-weight: 600;
}

.card-head p {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.shortcut-card {
  position: relative;
  display: grid;
  gap: 8px;
  padding: 18px 14px;
  border-radius: 18px;
  border: 1px solid var(--border-soft);
  background: linear-gradient(180deg, #fff, #f8fbff);
  cursor: pointer;
  transition: transform .24s ease, box-shadow .24s ease;
  overflow: hidden;
}

.shortcut-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 28px rgba(20,33,61,.1);
}

.shortcut-card__orb {
  position: absolute;
  top: -12px;
  right: -12px;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(20,88,212,.06), transparent);
}

.shortcut-card__icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
}

.tone-blue { background: linear-gradient(135deg, #e0f2fe, #bae6fd); color: #0284c7; }
.tone-teal { background: linear-gradient(135deg, #ccfbf1, #99f6e4); color: #0d9488; }
.tone-amber { background: linear-gradient(135deg, #fef3c7, #fde68a); color: #d97706; }
.tone-slate { background: linear-gradient(135deg, #f1f5f9, #e2e8f0); color: #475569; }

.shortcut-card strong {
  font-size: 13px;
  font-weight: 600;
}

.shortcut-card span {
  font-size: 11px;
  color: var(--text-secondary);
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.service-card {
  display: flex;
  gap: 14px;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid var(--border-soft);
  background: linear-gradient(180deg, #fff, #f8fbff);
  cursor: pointer;
  transition: transform .24s ease, box-shadow .24s ease;
}

.service-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 28px rgba(20,33,61,.1);
}

.service-card__cover {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

.gradient-brand { background: linear-gradient(135deg, #667eea, #764ba2); }
.gradient-teal { background: linear-gradient(135deg, #11998e, #38ef7d); }
.gradient-amber { background: linear-gradient(135deg, #f093fb, #f5576c); }
.gradient-slate { background: linear-gradient(135deg, #4b6cb7, #182848); }

.service-card__content {
  flex: 1;
  min-width: 0;
}

.service-card__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.service-card__head strong {
  font-size: 14px;
  font-weight: 600;
}

.service-card__content p {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.service-card__meta {
  display: flex;
  gap: 12px;
  margin-top: 8px;
  font-size: 11px;
  color: var(--text-secondary);
}

.todo-stack {
  display: grid;
  gap: 12px;
}

.todo-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid var(--border-soft);
  background: linear-gradient(180deg, #fff, #f8fbff);
  cursor: pointer;
  transition: transform .2s ease;
}

.todo-card:hover {
  transform: translateX(4px);
}

.todo-card__main {
  flex: 1;
}

.todo-card__main strong {
  font-size: 13px;
  font-weight: 600;
}

.todo-card__main p {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.todo-card__badge {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 999px;
}

.booking-stack {
  display: grid;
  gap: 10px;
}

.booking-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid var(--border-soft);
  background: linear-gradient(180deg, #fff, #f8fbff);
  cursor: pointer;
  transition: transform .2s ease;
}

.booking-card:hover {
  transform: translateX(4px);
}

.booking-card strong {
  font-size: 13px;
  font-weight: 600;
}

.booking-card p {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.detail-dialog__body {
  display: grid;
  gap: 10px;
}

.detail-dialog__item {
  padding: 12px;
  border-radius: 10px;
  background: linear-gradient(180deg, #fff, #f8fbff);
  border: 1px solid var(--border-soft);
  font-size: 13px;
}

.drawer-stack {
  display: grid;
  gap: 16px;
}

.cover-badge {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
}

.info-list {
  display: grid;
  gap: 10px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.info-row span {
  color: var(--text-secondary);
}

.tag-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@keyframes dashboardHalo {
  0%,100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-20px, 20px) scale(1.08); }
}

.weather-widget { background: linear-gradient(180deg, #f0f9ff, #fff) !important; }
.weather-mini { display: flex; align-items: center; gap: 14px; }
.weather-mini__icon { font-size: 36px; }
.weather-mini strong { font-size: 22px; }
.weather-mini p { margin: 2px 0 0; color: var(--text-secondary); font-size: 13px; }
</style>
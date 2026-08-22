<template>
  <div class="admin-page">
    <section class="admin-hero">
      <div class="admin-hero__main">
        <span class="hero-chip">Admin Console</span>
        <h1>资源运行状态、审核压力与配置风险统一掌控</h1>
        <p>
          管理端首页采用驾驶舱式表达，突出核心指标、异常信号、审核负载和高频治理入口，
          比传统统计卡片更接近真实企业后台的运营视图。
        </p>
      </div>

      <div class="admin-hero__signal">
        <div
          class="signal-card"
          @click="openSignal('pending')"
        >
          <span>今日待审核</span>
          <strong>23</strong>
          <small>较昨日 +4</small>
        </div>
        <div
          class="signal-card"
          @click="openSignal('risk')"
        >
          <span>高风险告警</span>
          <strong>03</strong>
          <small>库存与排班需要优先处理</small>
        </div>
      </div>
    </section>

    <section class="admin-metrics">
      <article
        class="metric-panel"
        @click="openMetric('用户')"
      >
        <span>平台用户数</span>
        <strong>{{ adminSummary.totalUsers }}</strong>
        <small>校内统一身份接入</small>
      </article>
      <article
        class="metric-panel"
        @click="openMetric('服务')"
      >
        <span>服务模块数</span>
        <strong>{{ adminSummary.totalServices }}</strong>
        <small>已纳管业务域</small>
      </article>
      <article
        class="metric-panel"
        @click="openMetric('预约')"
      >
        <span>进行中预约</span>
        <strong>{{ adminSummary.activeBookings }}</strong>
        <small>当前业务活跃量</small>
      </article>
      <article
        class="metric-panel"
        @click="openMetric('通过率')"
      >
        <span>审批通过率</span>
        <strong>{{ adminSummary.approvalRate }}</strong>
        <small>近 30 天整体表现</small>
      </article>
    </section>

    <section class="admin-grid">
      <el-card class="admin-card admin-card--wide">
        <template #header>
          <div class="card-head">
            <div>
              <h3>运营重点</h3>
              <p>优先处理高影响问题，降低拥堵和资源浪费。</p>
            </div>
          </div>
        </template>
        <div class="focus-grid">
          <article
            v-for="item in focusList"
            :key="item.title"
            class="focus-box"
            @click="openFocus(item)"
          >
            <div class="focus-box__head">
              <strong>{{ item.title }}</strong>
              <span
                class="focus-box__badge"
                :class="item.tone"
              >{{ item.value }}</span>
            </div>
            <p>{{ item.desc }}</p>
          </article>
        </div>
      </el-card>

      <el-card class="admin-card">
        <template #header>
          <div class="card-head">
            <div>
              <h3>快捷操作</h3>
              <p>把后台高频动作收进同一层入口。</p>
            </div>
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

      <el-card class="admin-card">
        <template #header>
          <div class="card-head">
            <div>
              <h3>风险雷达</h3>
              <p>今天需要重点盯住的系统信号。</p>
            </div>
          </div>
        </template>
        <div class="risk-list">
          <div
            v-for="risk in riskList"
            :key="risk.title"
            class="risk-item"
            @click="openRisk(risk)"
          >
            <div>
              <strong>{{ risk.title }}</strong>
              <p>{{ risk.desc }}</p>
            </div>
            <span
              class="focus-box__badge"
              :class="risk.tone"
            >{{ risk.level }}</span>
          </div>
        </div>
      </el-card>
    </section>

    <el-dialog
      v-if="detailVisible"
      :model-value="true"
      :title="detailTitle"
      width="620px"
      @close="detailVisible = false"
    >
      <div class="dialog-list">
        <div
          v-for="item in detailItems"
          :key="item"
          class="dialog-card"
        >
          {{ item }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchAdminSummary } from '@/services/campus'
import type { AdminSummary } from '@/types'

const router = useRouter()
const detailVisible = ref(false)
const detailTitle = ref('')
const detailItems = ref<string[]>([])
const adminSummary = ref<AdminSummary>({
  totalUsers: 0,
  totalServices: 0,
  activeBookings: 0,
  approvalRate: '0%',
})
onMounted(async () => {
  try {
    adminSummary.value = await fetchAdminSummary()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '获取管理数据失败')
  }
})

const focusList = [
  { title: '会议室高峰拥堵', desc: '周三至周五 14:00 后中型会议室申请量明显集中，建议启用候补或分流机制。', value: '高峰', tone: 'tone-warning' },
  { title: '设备库存偏低', desc: '投影仪与相机借用频率持续上升，当前可借库存接近安全阈值。', value: '预警', tone: 'tone-danger' },
  { title: '咨询排班紧张', desc: '就业指导与心理支持时段接近满负荷，建议扩增晚间班次。', value: '建议', tone: 'tone-brand' },
]

const quickActions = [
  { title: '服务治理', desc: '维护服务目录与开放状态', path: '/admin/services' },
  { title: '预约审核', desc: '集中处理待审申请', path: '/admin/bookings' },
  { title: '用户与权限', desc: '账号、角色与授权管理', path: '/admin/users' },
  { title: '系统设置', desc: '预约规则与通知策略', path: '/admin/system' },
]

const riskList = [
  { title: '库存同步延迟', desc: '设备中心库存更新存在轻微延迟，建议定期刷新页面获取最新数据。', level: '中', tone: 'tone-warning' },
  { title: '高峰审核堆积', desc: '午后审核量上升较快，当前管理员处理窗口存在瓶颈。', level: '高', tone: 'tone-danger' },
  { title: '规则配置分散', desc: '预约策略仍需要进一步收敛到统一系统设置模块。', level: '中', tone: 'tone-brand' },
]

function openMetric(type: string) {
  const mapping: Record<string, string[]> = {
    用户: ['教师账号 1,286 个', '学生账号 2,401 个', '管理员账号 205 个'],
    服务: ['空间资源 8 项', '设备资源 7 项', '咨询服务 5 项', '综合服务 4 项'],
    预约: ['会议室进行中 56 条', '设备借用进行中 34 条', '咨询服务进行中 28 条'],
    通过率: ['自动通过规则占比 18%', '人工审核通过率 97.2%', '平均审批时长 2.4 小时'],
  }
  detailTitle.value = `${type}明细`
  detailItems.value = mapping[type] || ['暂无明细']
  detailVisible.value = true
}

function openSignal(type: 'pending' | 'risk') {
  detailTitle.value = type === 'pending' ? '今日待审核' : '高风险告警'
  detailItems.value =
    type === 'pending'
      ? ['设备借用待审核 9 条', '会议室申请待审核 8 条', '咨询申请待审核 6 条']
      : ['设备库存偏低', '午后审核堆积', '规则配置分散']
  detailVisible.value = true
}

function openFocus(item: { title: string; desc: string; value: string }) {
  detailTitle.value = item.title
  detailItems.value = [item.desc, `当前判断：${item.value}`]
  detailVisible.value = true
}

function openRisk(item: { title: string; desc: string; level: string }) {
  detailTitle.value = item.title
  detailItems.value = [item.desc, `风险等级：${item.level}`, '建议结合系统设置和资源治理模块继续处理。']
  detailVisible.value = true
}
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
  background: linear-gradient(135deg, #0f172a, #132949 55%, #1458d4);
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
  background: radial-gradient(circle, rgba(59, 130, 246, 0.24), rgba(59, 130, 246, 0));
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
  margin: 16px 0 10px;
  font-size: 38px;
  line-height: 1.15;
}

.admin-hero p {
  max-width: 740px;
  margin: 0;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.82);
}

.admin-hero__signal {
  display: grid;
  gap: 14px;
}

.signal-card {
  display: grid;
  gap: 8px;
  padding: 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(8px);
  cursor: pointer;
  transition: transform 0.26s ease, background-color 0.26s ease;
}

.signal-card:hover {
  transform: translateY(-5px);
  background: rgba(255, 255, 255, 0.16);
}

.signal-card span,
.signal-card small {
  color: rgba(255, 255, 255, 0.74);
}

.signal-card strong {
  font-size: 40px;
  line-height: 1;
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
  background: radial-gradient(circle, rgba(20, 88, 212, 0.08), rgba(20, 88, 212, 0));
  pointer-events: none;
}

.metric-panel:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-card-hover);
}

.metric-panel span,
.metric-panel small {
  color: var(--text-secondary);
}

.metric-panel strong {
  font-size: 34px;
  line-height: 1;
}

.admin-grid {
  display: grid;
  grid-template-columns: 1.25fr 0.75fr;
  gap: 20px;
}

.admin-card {
  border: 1px solid var(--border-soft);
  border-radius: 24px;
  box-shadow: var(--shadow-card);
}

.admin-card--wide {
  grid-row: span 2;
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

.focus-grid {
  display: grid;
  gap: 16px;
}

.focus-box {
  padding: 18px;
  border: 1px solid var(--border-soft);
  border-radius: 20px;
  background: linear-gradient(180deg, #fff, #f8fbff);
  cursor: pointer;
  transition: transform 0.26s ease, box-shadow 0.26s ease;
}

.focus-box:hover {
  transform: translateY(-6px);
  box-shadow: 0 20px 34px rgba(20, 33, 61, 0.12);
}

.focus-box__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.focus-box p {
  margin: 10px 0 0;
  color: var(--text-secondary);
  line-height: 1.75;
}

.focus-box__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 52px;
  padding: 6px 12px;
  border-radius: 999px;
  color: #fff;
  font-size: 12px;
}

.tone-warning {
  background: linear-gradient(135deg, #d97706, #f59e0b);
}

.tone-danger {
  background: linear-gradient(135deg, #dc2626, #ef4444);
}

.tone-brand {
  background: linear-gradient(135deg, #1458d4, #3b82f6);
}

.quick-grid {
  display: grid;
  gap: 12px;
}

.quick-box {
  display: grid;
  gap: 8px;
  text-align: left;
  padding: 16px 18px;
  border: 1px solid var(--border-soft);
  border-radius: 18px;
  background: linear-gradient(180deg, #fff, #f8fbff);
  cursor: pointer;
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
}

.quick-box:hover {
  transform: translateX(6px);
  border-color: rgba(20, 88, 212, 0.16);
  box-shadow: 0 16px 28px rgba(20, 33, 61, 0.1);
}

.quick-box span {
  color: var(--text-secondary);
  font-size: 13px;
}

.risk-list {
  display: grid;
  gap: 14px;
}

.risk-item {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 16px;
  border: 1px solid var(--border-soft);
  border-radius: 18px;
  background: linear-gradient(180deg, #fff, #f8fbff);
  cursor: pointer;
  transition: transform 0.24s ease, box-shadow 0.24s ease;
}

.risk-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 18px 28px rgba(20, 33, 61, 0.1);
}

.risk-item p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.dialog-list {
  display: grid;
  gap: 12px;
}

.dialog-card {
  padding: 16px;
  border-radius: 18px;
  background: linear-gradient(180deg, #fff, #f8fbff);
  border: 1px solid var(--border-soft);
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
  .admin-hero,
  .admin-grid {
    grid-template-columns: 1fr;
  }

  .admin-metrics {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 760px) {
  .admin-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
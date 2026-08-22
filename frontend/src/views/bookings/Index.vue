<template>
  <div class="page-shell">
    <section class="dashboard-hero">
      <div class="dashboard-hero__main">
        <span class="hero-chip">我的预约</span>
        <h1>统一查看个人预约状态与审批进展</h1>
        <div class="hero-actions">
          <el-button
            type="primary"
            @click="router.push('/services')"
          >
            发起新预约
          </el-button>
          <el-button
            plain
            @click="quickVisible = true"
          >
            快捷操作
          </el-button>
        </div>
      </div>
      <div class="dashboard-hero__panel">
        <div class="hero-panel__label">
          预约概览
        </div>
        <div class="hero-panel__item">
          <strong>{{ bookings.length }}</strong><span>全部预约</span>
        </div>
        <div class="hero-panel__item">
          <strong>{{ pendingCount }}</strong><span>待审核</span>
        </div>
        <div class="hero-panel__item">
          <strong>{{ approvedCount }}</strong><span>已通过</span>
        </div>
      </div>
    </section>

    <div class="section-head">
      <h3 class="section-head__title">
        预约记录
      </h3>
      <el-segmented
        v-model="filter"
        :options="filters"
      />
    </div>

    <div
      v-if="filteredBookings.length === 0"
      class="empty-state"
    >
      <div class="empty-state__icon">
        📋
      </div>
      <p class="empty-state__title">
        暂无预约记录
      </p>
      <p class="empty-state__desc">
        前往服务中心发起会议室、设备或咨询预约
      </p>
      <el-button
        type="primary"
        @click="router.push('/services')"
      >
        浏览可预约服务
      </el-button>
    </div>

    <div
      v-else
      class="booking-stack"
    >
      <article
        v-for="item in filteredBookings"
        :key="item.id"
        class="booking-card"
        :class="'booking-card--' + item.status"
        @click="router.push(`/bookings/${item.id}`)"
      >
        <span class="booking-card__bar" />
        <div class="booking-card__body">
          <div class="booking-card__main">
            <div class="booking-card__icon">
              <el-icon :size="20">
                <Calendar />
              </el-icon>
            </div>
            <div class="booking-card__info">
              <div class="booking-card__title">
                {{ item.serviceName }}
              </div>
              <div class="booking-card__row">
                <el-icon :size="14">
                  <Clock />
                </el-icon>
                <span>{{ item.date }}  {{ item.timeRange }}</span>
              </div>
              <div class="booking-card__row booking-card__row--muted">
                <el-icon :size="14">
                  <Location />
                </el-icon>
                <span>{{ item.location }}</span>
                <span class="booking-card__divider">|</span>
                <span>{{ item.bookingNo }}</span>
              </div>
            </div>
          </div>
          <div class="booking-card__aside">
            <el-tag
              :type="statusTag(item.status)"
              effect="plain"
              size="small"
            >
              {{ statusText(item.status) }}
            </el-tag>
            <el-icon class="booking-card__chevron">
              <ArrowRight />
            </el-icon>
          </div>
        </div>
      </article>
    </div>

    <el-drawer
      v-model="quickVisible"
      title="预约快捷操作"
      size="420px"
    >
      <div class="drawer-stack">
        <el-button
          type="primary"
          @click="router.push('/rooms')"
        >
          发起会议室预约
        </el-button>
        <el-button
          plain
          @click="router.push('/equipment')"
        >
          查看设备借用
        </el-button>
        <el-button
          plain
          @click="router.push('/consultation')"
        >
          查看咨询排班
        </el-button>
        <el-button
          plain
          @click="router.push('/messages')"
        >
          查看审批消息
        </el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Calendar, Clock, Location } from '@element-plus/icons-vue'
import { fetchBookingRecords } from '@/services/campus'
import type { BookingRecord, BookingStatus } from '@/types'

const router = useRouter()
const filter = ref('all')
const quickVisible = ref(false)
const bookings = ref<BookingRecord[]>([])
const loading = ref(false)
const filters = [
  { label: '全部', value: 'all' },
  { label: '待审核', value: 'pending' },
  { label: '已通过', value: 'approved' },
]

const filteredBookings = computed(() => (filter.value === 'all' ? bookings.value : bookings.value.filter((item) => item.status === filter.value)))
const pendingCount = computed(() => bookings.value.filter((item) => item.status === 'pending').length)
const approvedCount = computed(() => bookings.value.filter((item) => item.status === 'approved').length)

onMounted(async () => {
  loading.value = true
  try {
    bookings.value = await fetchBookingRecords()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '获取预约记录失败')
  } finally {
    loading.value = false
  }
})

function statusTag(status: BookingStatus) {
  return { pending: 'warning', approved: 'success', rejected: 'danger', completed: 'info', cancelled: 'info' }[status]
}

function statusText(status: BookingStatus) {
  return { pending: '待审核', approved: '已通过', rejected: '已驳回', completed: '已完成', cancelled: '已取消' }[status]
}
</script>

<style scoped lang="scss">
.dashboard-hero {
  position: relative; display: grid; grid-template-columns: 1.2fr 0.8fr; gap: 20px;
  padding: 32px; border-radius: 30px; color: #fff;
  background: linear-gradient(135deg, #0e2647, #1458d4 62%, #52a1ff);
  box-shadow: var(--shadow-card); overflow: hidden;
}
.dashboard-hero::before {
  content: ""; position: absolute; inset: 0;
  background: radial-gradient(circle at 20% 20%, rgba(255,255,255,0.16), transparent 22%),
              linear-gradient(120deg, transparent 14%, rgba(255,255,255,0.08) 36%, transparent 62%);
}
.dashboard-hero::after {
  content: ""; position: absolute; inset: auto -60px -60px auto;
  width: 260px; height: 260px; border-radius: 50%;
  background: radial-gradient(circle, rgba(255,255,255,0.18), rgba(255,255,255,0));
  animation: dashHalo 8s ease-in-out infinite; pointer-events: none;
}
.dashboard-hero__main, .dashboard-hero__panel { position: relative; z-index: 1; }
.hero-chip { display: inline-flex; padding: 5px 12px; border-radius: 999px; font-size: 12px; letter-spacing: 0.06em; background: rgba(255,255,255,0.14); margin-bottom: 14px; }
.dashboard-hero h1 { margin: 12px 0 10px; font-size: 36px; line-height: 1.18; }
.hero-actions { display: flex; gap: 12px; margin-top: 22px; }
.dashboard-hero__panel { display: grid; gap: 12px; padding: 22px; border-radius: 22px; background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.12); backdrop-filter: blur(10px); }
.hero-panel__label { font-size: 13px; color: rgba(255,255,255,0.64); margin-bottom: 2px; }
.hero-panel__item { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.1); }
.hero-panel__item:last-child { border-bottom: none; }
.hero-panel__item strong { font-size: 20px; font-weight: 700; }
.hero-panel__item span { font-size: 13px; color: rgba(255,255,255,0.7); }

.section-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 20px; }
.section-head__title { margin: 0; font-size: 18px; font-weight: 700; }

.empty-state { display: grid; place-items: center; gap: 12px; padding: 80px 20px; text-align: center; }
.empty-state__icon { font-size: 48px; }
.empty-state__title { font-size: 18px; font-weight: 600; color: var(--text-primary); margin: 0; }
.empty-state__desc { color: var(--text-secondary); margin: 0; }

.booking-stack { display: grid; gap: 12px; }
.booking-card {
  position: relative;
  display: flex; align-items: stretch;
  background: #fff;
  border: 1px solid #e8ecf1;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
  cursor: pointer;
  overflow: hidden;
  transition: box-shadow .2s ease, border-color .2s ease, transform .2s ease;
}
.booking-card:hover {
  border-color: #c8d6e5;
  box-shadow: 0 4px 20px rgba(20,33,61,.1);
  transform: translateY(-1px);
}
.booking-card__bar {
  position: absolute; left: 0; top: 0; bottom: 0;
  width: 4px;
}
.booking-card--pending .booking-card__bar { background: #f0ad4e; }
.booking-card--approved .booking-card__bar { background: #5cb85c; }
.booking-card--rejected .booking-card__bar { background: #d9534f; }
.booking-card--completed .booking-card__bar { background: #5bc0de; }
.booking-card--cancelled .booking-card__bar { background: #999; }

.booking-card__body { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 16px 20px 16px 20px; width: 100%; }
.booking-card__main { display: flex; align-items: center; gap: 14px; flex: 1; min-width: 0; }
.booking-card__icon {
  width: 40px; height: 40px; display: grid; place-items: center; flex-shrink: 0;
  border-radius: 10px; background: #eef2ff; color: #4f6ef7;
}
.booking-card__info { display: grid; gap: 4px; min-width: 0; }
.booking-card__title { font-size: 15px; font-weight: 600; color: #1e293b; line-height: 1.3; }
.booking-card__row { display: flex; align-items: center; gap: 4px; color: #64748b; font-size: 13px; }
.booking-card__row--muted { color: #94a3b8; font-size: 12px; }
.booking-card__divider { color: #cbd5e1; margin: 0 2px; }
.booking-card__aside { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; flex-shrink: 0; }
.booking-card__chevron { color: #cbd5e1; font-size: 18px; transition: transform .2s ease, color .2s ease; }
.booking-card:hover .booking-card__chevron { color: #4f6ef7; transform: translateX(4px); }

.dialog-list, .drawer-stack { display: grid; gap: 12px; }
.dialog-card { padding: 16px; border-radius: 18px; background: linear-gradient(180deg, #fff, #f8fbff); border: 1px solid var(--border-soft); }
@keyframes dashHalo { 0%,100% { transform: translate3d(0,0,0) scale(1); } 50% { transform: translate3d(-20px,-10px,0) scale(1.08); } }
@media (max-width: 900px) { .dashboard-hero { grid-template-columns: 1fr; } .booking-card { flex-direction: column; align-items: flex-start; } .booking-card__aside { flex-direction: row; align-items: center; gap: 10px; width: 100%; } }
</style>
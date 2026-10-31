<template>
  <div class="page-shell">
    <section class="dashboard-hero">
      <div class="dashboard-hero__main">
        <p class="hz-eyebrow">
          My bookings · HZNU
        </p>
        <h1>我的预约</h1>
        <p class="dashboard-hero__desc">
          查看审批进度与时间安排，随时回到你的校园日程。
        </p>
        <p class="dashboard-hero__motto">
          勤慎诚恕 · 博雅精进
        </p>
      </div>
      <div class="dashboard-hero__panel">
        <div class="hero-panel__label">
          预约概览
        </div>
        <div class="hero-panel__item">
          <strong>{{ total || bookings.length }}</strong><span>全部预约</span>
        </div>
        <div class="hero-panel__item">
          <strong>{{ pendingCount }}</strong><span>处理中</span>
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
      v-if="loading"
      class="loading-state"
    >
      <el-icon class="is-loading">
        <Loading />
      </el-icon>
      <span>加载中...</span>
    </div>
    <div
      v-else-if="filteredBookings.length === 0"
      class="empty-state"
    >
      <span>暂无预约记录</span>
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
                {{
                  item.equipmentName
                    ? item.serviceName + '（' + item.equipmentName + ' × ' + (item.quantity || 1) + '）'
                    : item.roomName
                      ? item.serviceName + '（' + item.roomName + '）'
                      : item.consultantName
                        ? item.serviceName + '（' + item.consultantName + '）'
                        : item.serviceName
                }}
              </div>
              <div class="booking-card__row">
                <el-icon :size="14">
                  <Clock />
                </el-icon>
                <span>{{ item.date }}  {{ item.timeRange }}</span>
              </div>
              <div class="booking-card__row booking-card__row--muted">
                <span
                  v-if="item.campus"
                  class="campus-chip"
                >{{ campusName(item.campus) }}</span>
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

    <div
      v-if="hasMore"
      class="load-more"
    >
      <el-button
        :loading="loadingMore"
        @click="loadMore"
      >
        加载更多（已显示 {{ bookings.length }} / {{ total }}）
      </el-button>
    </div>

  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Calendar, Clock, Location } from '@element-plus/icons-vue'
import { fetchBookingPage } from '@/common/campus'
import type { BookingRecord, BookingStatus } from '@/common/types'

const router = useRouter()
const filter = ref('all')
const bookings = ref<BookingRecord[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = 200
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = computed(() => bookings.value.length < total.value)
const filters = [
  { label: '全部', value: 'all' },
  { label: '处理中', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已完成', value: 'completed' },
]

const filteredBookings = computed(() => (filter.value === 'all' ? bookings.value : bookings.value.filter((item) => item.status === filter.value)))
const pendingCount = computed(() => bookings.value.filter((item) => item.status === 'pending').length)
const approvedCount = computed(() => bookings.value.filter((item) => item.status === 'approved').length)

onMounted(async () => {
  loading.value = true
  try {
    await loadPage(1, true)
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '获取预约记录失败')
  } finally {
    loading.value = false
  }
})

async function loadPage(page: number, replace: boolean) {
  const result = await fetchBookingPage({ pageNo: page, pageSize })
  bookings.value = replace ? result.records : [...bookings.value, ...result.records]
  total.value = result.total
  pageNo.value = page
}

async function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    await loadPage(pageNo.value + 1, false)
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '加载更多预约失败')
  } finally {
    loadingMore.value = false
  }
}

function statusTag(status: BookingStatus) {
  return { pending: 'warning', approved: 'success', rejected: 'danger', completed: 'info', cancelled: 'info' }[status]
}

function statusText(status: BookingStatus) {
  return { pending: '处理中', approved: '已通过', rejected: '已驳回', completed: '已完成', cancelled: '已取消' }[status]
}

function campusName(c?: string) {
  return c === 'cq' ? '仓前' : c === 'xs' ? '下沙' : ''
}
</script>

<style scoped lang="scss">
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 60px;
  color: var(--ink-3);
  font-size: 13px;
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 8px 0 4px;
}

.booking-stack {
  display: grid;
  gap: 12px;
}

.booking-card {
  position: relative;
  display: flex;
  align-items: stretch;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
  cursor: pointer;
  transition:
    transform 0.18s var(--ease-out),
    box-shadow 0.18s var(--ease-out),
    border-color 0.18s ease;
}

.booking-card:hover {
  transform: translateY(-2px);
  border-color: rgba(176, 138, 69, 0.42);
  box-shadow: var(--shadow-card-hover);
}

.booking-card__bar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
}

.booking-card--pending .booking-card__bar { background: var(--warning-500); }
.booking-card--approved .booking-card__bar { background: var(--success-500); }
.booking-card--rejected .booking-card__bar { background: var(--danger-500); }
.booking-card--completed .booking-card__bar { background: var(--lake); }
.booking-card--cancelled .booking-card__bar { background: var(--ink-4); }

.booking-card__body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
  padding: 16px 20px 16px 23px;
}

.booking-card__main {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  min-width: 0;
}

.booking-card__icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  border: 1px solid var(--line);
  background: var(--surface-2);
  color: var(--lake);
}

.booking-card__info {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.booking-card__title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  line-height: 1.4;
}

.booking-card__row {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--ink-3);
  font-size: 12.5px;
}

.booking-card__row--muted {
  color: var(--ink-4);
  font-size: 11.5px;
}

.booking-card__divider {
  color: var(--line-strong);
  margin: 0 2px;
}

.booking-card__aside {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.booking-card__chevron {
  color: var(--ink-4);
  font-size: 16px;
  transition: transform 0.18s var(--ease-out), color 0.18s ease;
}

.booking-card:hover .booking-card__chevron {
  color: var(--gold);
  transform: translateX(4px);
}

.campus-chip {
  padding: 1px 6px;
  border: 1px solid var(--line);
  border-radius: 3px;
  background: var(--surface-2);
  color: var(--lake);
  font-size: 10.5px;
  font-weight: 600;
}

@media (max-width: 900px) {
  .booking-card__body {
    flex-direction: column;
    align-items: flex-start;
  }

  .booking-card__aside {
    flex-direction: row;
    align-items: center;
    width: 100%;
    justify-content: space-between;
  }
}
</style>
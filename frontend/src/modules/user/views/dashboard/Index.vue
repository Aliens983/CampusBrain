<template>
  <div v-loading="loading" class="dashboard-page" element-loading-text="加载首页数据…">
    <HeroSection
      :banners="banners"
      :today-bookings="todayBookings"
      @navigate="goPath"
      @open-booking="goBooking"
    />

    <MetricGrid
      :stats="dashboardStats"
      @select="openMetricDetail"
    />

    <section class="dashboard-grid">
      <div class="dashboard-grid__main">
        <QuickShortcuts
          :shortcuts="shortcuts"
          @navigate="goShortcut"
        />

        <ServiceRecommend
          :services="services"
          @select="activeService = $event"
          @view-all="goPath('/services')"
        />
      </div>

      <div class="dashboard-grid__side">
        <TodoFeedback
          :items="todoList"
          @navigate="goPath"
        />

        <RecentBookings
          :bookings="recentBookings"
          @open-booking="goBooking"
        />
      </div>
    </section>

    <MetricDetailDialog
      v-model:visible="metricDialogVisible"
      :title="metricDialogTitle"
      :items="metricDialogItems"
    />

    <ServiceDrawer
      v-model:visible="serviceDrawerVisible"
      :service="activeService"
      @view-detail="goServiceDetail"
    />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useDashboard, type DashboardShortcut } from './composables'
import HeroSection from './components/HeroSection.vue'
import MetricGrid from './components/MetricGrid.vue'
import QuickShortcuts from './components/QuickShortcuts.vue'
import ServiceRecommend from './components/ServiceRecommend.vue'
import TodoFeedback from './components/TodoFeedback.vue'
import RecentBookings from './components/RecentBookings.vue'
import MetricDetailDialog from './components/MetricDetailDialog.vue'
import ServiceDrawer from './components/ServiceDrawer.vue'

const router = useRouter()

const {
  loading,
  banners,
  services,
  activeService,
  todayBookings,
  recentBookings,
  dashboardStats,
  shortcuts,
  todoList,
  serviceDrawerVisible,
  metricDialogVisible,
  metricDialogTitle,
  metricDialogItems,
  openMetricDetail,
} = useDashboard()

function goPath(path: string) {
  router.push(path)
}

function goBooking(id: number) {
  router.push(`/bookings/${id}`)
}

function goServiceDetail(id: number) {
  router.push(`/service/${id}`)
}

function goShortcut(item: DashboardShortcut) {
  router.push({ path: item.path, query: item.query })
}
</script>

<style scoped lang="scss">
.dashboard-page {
  display: grid;
  gap: 20px;
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

@media (max-width: 1100px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

.weather-widget { background: linear-gradient(180deg, #F1F7FF, #fff) !important; }
.weather-mini { display: flex; align-items: center; gap: 14px; }
.weather-mini__icon { font-size: 36px; }
.weather-mini strong { font-size: 22px; }
.weather-mini p { margin: 2px 0 0; color: var(--text-secondary); font-size: 13px; }
</style>

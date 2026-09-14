<template>
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
        @click="emit('open-booking', item.id)"
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
</template>

<script setup lang="ts">
import type { BookingRecord, BookingStatus } from '@/common/types'

defineProps<{
  bookings: BookingRecord[]
}>()

const emit = defineEmits<{
  (e: 'open-booking', id: number): void
}>()

function bookingTag(status: BookingStatus) {
  return { pending: 'warning', approved: 'success', rejected: 'danger', completed: 'info', cancelled: 'info' }[status]
}

function statusText(status: BookingStatus) {
  return { pending: '处理中', approved: '已通过', rejected: '已驳回', completed: '已完成', cancelled: '已取消' }[status]
}
</script>

<style scoped lang="scss">
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
  background: linear-gradient(180deg, #fff, #F9FCFF);
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
</style>

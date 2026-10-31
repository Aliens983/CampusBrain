<template>
  <el-card
    class="surface-card"
    shadow="never"
  >
    <template #header>
      <div class="card-head">
        <div class="card-head__title">
          <span class="card-head__index">肆</span>
          <div>
            <h3>最近预约</h3>
            <p>审批进度和时间安排随时回看。</p>
          </div>
        </div>
        <span class="card-head__en">Recent</span>
      </div>
    </template>
    <div class="booking-stack">
      <div
        v-for="item in bookings"
        :key="item.id"
        class="booking-card"
        @click="emit('open-booking', item.id)"
      >
        <div class="booking-card__main">
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
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.card-head__title {
  display: flex;
  align-items: flex-start;
  gap: 11px;
}

.card-head__index {
  padding-top: 3px;
  font-family: var(--font-serif);
  font-size: 11px;
  color: var(--gold);
}

.card-head h3 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 600;
  color: var(--ink);
}

.card-head p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--ink-3);
}

.card-head__en {
  padding-top: 4px;
  font-family: var(--font-latin);
  font-size: 8px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--ink-4);
}

.booking-stack {
  display: grid;
}

.booking-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 13px 0;
  border-top: 1px solid var(--line);
  cursor: pointer;
  transition: transform 0.16s var(--ease-out);
}

.booking-card:first-child {
  border-top: 0;
  padding-top: 4px;
}

.booking-card:hover {
  transform: translateX(3px);
}

.booking-card__main {
  min-width: 0;
}

.booking-card strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink);
}

.booking-card p {
  margin-top: 3px;
  font-family: var(--font-latin);
  font-size: 10px;
  letter-spacing: 0.03em;
  color: var(--ink-4);
}
</style>

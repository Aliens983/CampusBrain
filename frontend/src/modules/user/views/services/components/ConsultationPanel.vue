<template>
  <el-divider content-position="left">
    选择咨询师与时段
  </el-divider>

  <div class="consult-grid">
    <div
      v-for="c in consultants"
      :key="c.id"
      class="consult-card"
      :class="{ 'is-active': selected?.id === c.id }"
      role="button"
      tabindex="0"
      @click="emit('select', c)"
    >
      <div class="consult-card__avatar">
        {{ c.name.slice(0, 1) }}
      </div>
      <div class="consult-card__body">
        <strong>{{ c.name }}</strong>
        <p>{{ c.title }} · {{ c.department }}</p>
        <span
          v-if="c.expertise && c.expertise.length"
          class="consult-card__expertise"
        >
          {{ c.expertise.join(' / ') }}
        </span>
        <el-button
          size="small"
          type="primary"
          plain
          class="consult-card__chat"
          @click.stop="emit('chat', c)"
        >
          <el-icon><ChatDotRound /></el-icon>
          <span>在线留言</span>
        </el-button>
      </div>
      <span class="consult-card__rating">
        {{ c.rating ?? '—' }}
      </span>
    </div>
  </div>

  <div
    v-if="selected"
    class="slot-panel"
  >
    <div class="slot-panel__date">
      <el-date-picker
        :model-value="consultDate"
        type="date"
        value-format="YYYY-MM-DD"
        format="YYYY-MM-DD"
        :clearable="false"
        :disabled-date="disableConsultDate"
        :prefix-icon="Calendar"
        placeholder="选择预约日期"
        @update:model-value="onDateChange"
      />
      <span class="slot-panel__hint">可约未来 14 天内的工作日（以老师排班为准）</span>
    </div>
    <div class="slot-panel__head">
      <strong>「{{ selected.name }}」{{ consultDate }} 可约时段</strong>
      <el-icon
        v-if="loadingSlots"
        class="is-loading"
      >
        <Loading />
      </el-icon>
    </div>
    <div
      v-if="!loadingSlots && slots.length"
      class="slot-chips"
    >
      <button
        v-for="s in slots"
        :key="s.slotId"
        class="slot-chip"
        :class="{ 'is-active': bookingSlotId === s.slotId, 'is-past': isSlotPast(s) }"
        :disabled="isSlotPast(s)"
        :title="isSlotPast(s) ? '该时段已开始或已结束' : ''"
        @click="bookingSlotId = s.slotId"
      >
        {{ s.startTime }} - {{ s.endTime }}
      </button>
    </div>
    <p
      v-else-if="!loadingSlots"
      class="muted"
    >
      该咨询师当日暂无排班，请换个日期或咨询师试试。
    </p>
    <el-button
      type="primary"
      size="large"
      class="slot-submit"
      :disabled="!bookingSlotId"
      :loading="submitting"
      @click="emit('submit')"
    >
      {{ bookingSlotId ? '预约该时段' : '请先选择一个时段' }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Calendar, ChatDotRound, Loading } from '@element-plus/icons-vue'
import { disableConsultDate, localDate, type ConsultantLite, type SlotLite } from '../composables'

const props = defineProps<{
  consultants: ConsultantLite[]
  selected: ConsultantLite | null
  slots: SlotLite[]
  bookingSlotId: number | null
  loadingSlots: boolean
  submitting: boolean
  consultDate: string
}>()

const emit = defineEmits<{
  (e: 'select', consultant: ConsultantLite): void
  (e: 'chat', consultant: ConsultantLite): void
  (e: 'update:bookingSlotId', slotId: number): void
  (e: 'update:consultDate', date: string): void
  (e: 'submit'): void
}>()

const bookingSlotId = computed({
  get: () => props.bookingSlotId,
  set: (slotId: number) => emit('update:bookingSlotId', slotId),
})

function onDateChange(date: string | null) {
  if (date) emit('update:consultDate', date)
}

/** 今天已开始/已结束的时段不可选（未来日期不受限，与后端 BookingWindowPolicy 口径一致） */
function isSlotPast(slot: SlotLite) {
  if (props.consultDate !== localDate()) return false
  const [h, m] = slot.startTime.split(':').map(Number)
  const now = new Date()
  return h * 60 + m <= now.getHours() * 60 + now.getMinutes()
}
</script>

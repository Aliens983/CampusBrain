<template>
  <el-divider content-position="left">
    选择教室与预约时段
  </el-divider>

  <div class="consult-grid">
    <button
      v-for="r in rooms"
      :key="r.id"
      class="consult-card"
      :class="{ 'is-active': selectedRoom?.id === r.id }"
      @click="emit('select', r)"
    >
      <div class="consult-card__avatar room">
        🏫
      </div>
      <div class="consult-card__body">
        <strong>{{ r.name }}</strong>
        <p>{{ r.location || '教学楼' }} · 容纳 {{ r.seats || '-' }} 人</p>
      </div>
    </button>
  </div>
  <p
    v-if="!rooms.length"
    class="muted"
  >
    该服务暂未登记教室。
  </p>

  <div
    v-if="selectedRoom"
    class="slot-panel borrow-panel"
  >
    <div class="borrow-grid">
      <label class="borrow-full">
        <span>预约日期</span>
        <el-date-picker
          v-model="date"
          type="date"
          value-format="YYYY-MM-DD"
          :disabled-date="disablePastDate"
          placeholder="选择日期"
          size="large"
        />
      </label>
      <label>
        <span>开始时间</span>
        <el-select
          v-model="start"
          size="large"
        >
          <el-option
            v-for="h in hourOptions"
            :key="h"
            :label="h"
            :value="h"
          />
        </el-select>
      </label>
      <label>
        <span>结束时间</span>
        <el-select
          v-model="end"
          size="large"
        >
          <el-option
            v-for="h in hourOptions"
            :key="h"
            :label="h"
            :value="h"
          />
        </el-select>
      </label>
    </div>
    <el-button
      type="primary"
      size="large"
      class="slot-submit"
      :loading="submitting"
      @click="emit('submit')"
    >
      提交预约
    </el-button>
    <p class="borrow-tip">
      一间教室同一时段仅可被一人预约；审核通过后占用该时段，到点自动结束。
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { disablePastDate, hourOptions } from '../composables'
import type { RoomLite } from '../composables'

const props = defineProps<{
  rooms: RoomLite[]
  selectedRoom: RoomLite | null
  submitting: boolean
  date: string
  start: string
  end: string
}>()

const emit = defineEmits<{
  (e: 'select', item: RoomLite): void
  (e: 'update:date', value: string): void
  (e: 'update:start', value: string): void
  (e: 'update:end', value: string): void
  (e: 'submit'): void
}>()

const date = computed({
  get: () => props.date,
  set: (value: string) => emit('update:date', value),
})
const start = computed({
  get: () => props.start,
  set: (value: string) => emit('update:start', value),
})
const end = computed({
  get: () => props.end,
  set: (value: string) => emit('update:end', value),
})
</script>

<style scoped lang="scss">
.consult-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.consult-card {
  display: flex; align-items: center; gap: 12px; padding: 14px; border-radius: 16px; text-align: left;
  border: 1px solid var(--border-soft); background: linear-gradient(180deg, #fff, #F9FCFF);
  cursor: pointer; transition: transform .2s ease, border-color .2s ease, box-shadow .2s ease;
}
.consult-card:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(20,33,61,.08); }
.consult-card.is-active { border-color: #3FB6FF; background: #F6FAFF; box-shadow: 0 0 0 1px #3FB6FF; }
.consult-card:disabled { opacity: .55; cursor: not-allowed; }
.consult-card__avatar { width: 42px; height: 42px; border-radius: 50%; flex-shrink: 0; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #ADE2FF, #7BD0FF); color: #fff; font-weight: 700; }
.consult-card__avatar.room { border-radius: 12px; background: linear-gradient(135deg, #fcd34d, #d97706); font-size: 18px; }
.consult-card__body { flex: 1; min-width: 0; }
.consult-card__body strong { font-size: 14px; }
.consult-card__body p { margin: 2px 0; color: var(--text-secondary); font-size: 12px; }

.slot-panel { display: grid; gap: 12px; padding: 16px; border-radius: 16px; background: #F4FAFF; border: 1px solid var(--border-soft); }
.slot-submit { justify-self: start; margin-top: 4px; }

.borrow-panel { gap: 14px; }
.borrow-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }
.borrow-grid .borrow-full { grid-column: 1 / -1; }
.borrow-grid label { display: grid; gap: 6px; font-size: 13px; color: var(--text-secondary); }
.borrow-grid em { font-style: normal; font-size: 12px; color: var(--text-tertiary); }
.borrow-tip { margin: 0; font-size: 12px; color: var(--text-tertiary); }

@media (max-width: 700px) { .consult-grid, .borrow-grid { grid-template-columns: 1fr; } }
</style>

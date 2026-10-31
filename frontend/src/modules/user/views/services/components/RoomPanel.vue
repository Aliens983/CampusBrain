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
        <el-icon><OfficeBuilding /></el-icon>
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
import { OfficeBuilding } from '@element-plus/icons-vue'
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

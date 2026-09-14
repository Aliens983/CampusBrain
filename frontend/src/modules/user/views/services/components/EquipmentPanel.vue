<template>
  <el-divider content-position="left">
    选择设备与借用时段
  </el-divider>

  <div class="consult-grid">
    <button
      v-for="d in equipment"
      :key="d.id"
      class="consult-card"
      :class="{ 'is-active': selectedEquipment?.id === d.id }"
      :disabled="(d.availableStock || 0) <= 0"
      @click="emit('select', d)"
    >
      <div class="consult-card__avatar equipment">
        {{ d.name.slice(0, 1) }}
      </div>
      <div class="consult-card__body">
        <strong>{{ d.name }}</strong>
        <p>{{ d.location || '设备管理中心' }} · {{ d.unit || '' }}</p>
        <span class="consult-card__expertise">
          {{ d.description || d.category || '校园公共设备' }}
        </span>
      </div>
      <span
        class="equip-stock"
        :class="{ 'is-empty': (d.availableStock || 0) <= 0 }"
      >
        {{ (d.availableStock || 0) <= 0 ? '已借空' : '可借 ' + (d.availableStock || 0) + ' ' + (d.unit || '台') }}
      </span>
    </button>
  </div>

  <div
    v-if="selectedEquipment"
    class="slot-panel borrow-panel"
  >
    <div class="borrow-grid">
      <label>
        <span>数量</span>
        <el-input-number
          v-model="qty"
          :min="1"
          :max="Math.max(1, selectedEquipment.availableStock || 1)"
          size="large"
        />
        <em>最多 {{ selectedEquipment.availableStock }} {{ selectedEquipment.unit || '台' }}</em>
      </label>
      <label>
        <span>借用日期</span>
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
      提交借用申请
    </el-button>
    <p class="borrow-tip">
      借用按固定时段计；审核通过后即占用该时段库存，到点自动归还，无需手动操作。
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { disablePastDate, hourOptions } from '../composables'
import type { EquipmentLite } from '../composables'

const props = defineProps<{
  equipment: EquipmentLite[]
  selectedEquipment: EquipmentLite | null
  submitting: boolean
  qty: number
  date: string
  start: string
  end: string
}>()

const emit = defineEmits<{
  (e: 'select', item: EquipmentLite): void
  (e: 'update:qty', value: number): void
  (e: 'update:date', value: string): void
  (e: 'update:start', value: string): void
  (e: 'update:end', value: string): void
  (e: 'submit'): void
}>()

const qty = computed({
  get: () => props.qty,
  set: (value: number) => emit('update:qty', value),
})
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
.consult-card__avatar.equipment { border-radius: 12px; background: linear-gradient(135deg, #5eead4, #0d9488); }
.consult-card__body { flex: 1; min-width: 0; }
.consult-card__body strong { font-size: 14px; }
.consult-card__body p { margin: 2px 0; color: var(--text-secondary); font-size: 12px; }
.consult-card__expertise { color: var(--text-tertiary); font-size: 11px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; }
.equip-stock { font-size: 12px; font-weight: 600; color: #0d9488; flex-shrink: 0; }
.equip-stock.is-empty { color: #dc2626; }

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

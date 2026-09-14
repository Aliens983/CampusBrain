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
          💬 在线留言
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
    <div class="slot-panel__head">
      <strong>「{{ selected.name }}」今日可约时段</strong>
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
        :class="{ 'is-active': bookingSlotId === s.slotId }"
        @click="bookingSlotId = s.slotId"
      >
        {{ s.startTime }} - {{ s.endTime }}
      </button>
    </div>
    <p
      v-else-if="!loadingSlots"
      class="muted"
    >
      该咨询师今日暂无排班，可预约日期以老师排班为准。
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
import { Loading } from '@element-plus/icons-vue'
import type { ConsultantLite, SlotLite } from '../composables'

const props = defineProps<{
  consultants: ConsultantLite[]
  selected: ConsultantLite | null
  slots: SlotLite[]
  bookingSlotId: number | null
  loadingSlots: boolean
  submitting: boolean
}>()

const emit = defineEmits<{
  (e: 'select', consultant: ConsultantLite): void
  (e: 'chat', consultant: ConsultantLite): void
  (e: 'update:bookingSlotId', slotId: number): void
  (e: 'submit'): void
}>()

const bookingSlotId = computed({
  get: () => props.bookingSlotId,
  set: (slotId: number) => emit('update:bookingSlotId', slotId),
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
.consult-card__body { flex: 1; min-width: 0; }
.consult-card__body strong { font-size: 14px; }
.consult-card__body p { margin: 2px 0; color: var(--text-secondary); font-size: 12px; }
.consult-card__expertise { color: var(--text-tertiary); font-size: 11px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; }
.consult-card__rating { color: #d97706; font-size: 13px; font-weight: 700; flex-shrink: 0; }

.slot-panel { display: grid; gap: 12px; padding: 16px; border-radius: 16px; background: #F4FAFF; border: 1px solid var(--border-soft); }
.slot-panel__head { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.slot-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.slot-chip {
  padding: 8px 14px; border-radius: 999px; font-size: 13px; cursor: pointer;
  border: 1px solid #E2ECFC; background: #fff; color: #3E4C66;
  transition: all .18s ease;
}
.slot-chip:hover { border-color: #ADE2FF; color: #1E98F2; }
.slot-chip.is-active { background: linear-gradient(135deg, #7BD0FF, #1E98F2); color: #fff; border-color: transparent; }
.slot-submit { justify-self: start; margin-top: 4px; }

@media (max-width: 700px) { .consult-grid { grid-template-columns: 1fr; } }
</style>

<template>
  <div class="schedule-page">
    <header class="page-head">
      <div>
        <h1>我的排班</h1>
        <p>教师咨询固定时段 · 工作日</p>
      </div>
      <el-date-picker
        v-model="selectedDate"
        type="date"
        value-format="YYYY-MM-DD"
        :disabled-date="disableDate"
        @change="loadSchedule"
      />
    </header>

    <nav class="date-strip" aria-label="选择日期">
      <button
        v-for="day in dates"
        :key="day"
        class="date-button"
        :class="{ 'is-active': selectedDate === day, 'is-weekend': isWeekend(day) }"
        :disabled="isWeekend(day)"
        @click="selectedDate = day; loadSchedule()"
      >
        <span>{{ weekday(day) }}</span>
        <strong>{{ day.slice(5) }}</strong>
      </button>
    </nav>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="排班时段" name="schedule">
        <div v-loading="loading" class="slot-grid">
          <article v-for="slot in slots" :key="slot.slotId" class="slot-row">
            <div class="slot-time">
              <strong>{{ slot.startTime }}</strong>
              <span>{{ slot.endTime }}</span>
            </div>
            <div class="slot-info">
              <strong>{{ slot.consultantName }}</strong>
              <span>{{ selectedDate }} · {{ statusLabel(slot) }}</span>
            </div>
            <el-tag :type="statusType(slot)">{{ statusLabel(slot) }}</el-tag>
            <el-button
              v-if="canApply(slot)"
              type="primary"
              plain
              @click="openApply(slot)"
            >申请取消排班</el-button>
          </article>
          <el-empty v-if="!loading && slots.length === 0" :description="isWeekend(selectedDate) ? '周末不排班' : '该日期没有工作日排班'" />
        </div>
      </el-tab-pane>
      <el-tab-pane label="申请记录" name="requests">
        <div v-loading="requestsLoading" class="request-list">
          <article v-for="item in requests" :key="item.id" class="request-row">
            <div>
              <strong>{{ item.date }} {{ item.startTime }}–{{ item.endTime }}</strong>
              <p>{{ item.consultantName }} · {{ item.reason }}</p>
              <small v-if="item.auditRemark">审批意见：{{ item.auditRemark }}</small>
            </div>
            <el-tag :type="requestType(item.status)">{{ requestLabel(item.status) }}</el-tag>
          </article>
          <el-empty v-if="!requestsLoading && requests.length === 0" description="暂无排班调整申请记录" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="applyVisible" title="申请取消排班" width="460px">
      <p v-if="selectedSlot" class="dialog-meta">{{ selectedDate }} {{ selectedSlot.startTime }}–{{ selectedSlot.endTime }}</p>
      <el-input v-model="reason" type="textarea" :rows="4" maxlength="255" show-word-limit placeholder="请填写取消排班原因" />
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/common/utils/request'

interface ScheduleSlot {
  slotId: number
  consultantId: number
  consultantName: string
  date: string
  startTime: string
  endTime: string
  availability: number
  pendingRequestId?: number | null
}
interface CancelRequest {
  id: number
  consultantName: string
  date: string
  startTime: string
  endTime: string
  reason: string
  status: number
  auditRemark?: string
}

const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const slots = ref<ScheduleSlot[]>([])
const requests = ref<CancelRequest[]>([])
const loading = ref(false)
const requestsLoading = ref(false)
const activeTab = ref('schedule')
const applyVisible = ref(false)
const submitting = ref(false)
const selectedSlot = ref<ScheduleSlot | null>(null)
const reason = ref('')
const dates = computed(() => Array.from({ length: 14 }, (_, i) => dayjs().add(i, 'day').format('YYYY-MM-DD')))

const today = dayjs().format('YYYY-MM-DD')
function isWeekend(value: string) { const day = dayjs(value).day(); return day === 0 || day === 6 }
/** 与后端规则一致：仅未来工作日、可预约且无待审批申请时才能申请取消排班 */
function canApply(slot: ScheduleSlot) {
  return slot.availability === 1 && !slot.pendingRequestId && slot.date > today && !isWeekend(slot.date)
}
function disableDate(date: Date) {
  const value = dayjs(date).format('YYYY-MM-DD')
  return value < dates.value[0] || value > dates.value[13] || isWeekend(value)
}
function weekday(value: string) { return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][dayjs(value).day()] }
function statusLabel(slot: ScheduleSlot) {
  if (slot.pendingRequestId) return '取消排班待审批'
  return ({ 1: '可预约', 0: '已预约', '-1': '已取消' } as Record<number, string>)[slot.availability] || '不可用'
}
function statusType(slot: ScheduleSlot) {
  if (slot.pendingRequestId) return 'warning'
  return ({ 1: 'success', 0: 'info', '-1': 'danger' } as Record<number, 'success' | 'info' | 'danger'>)[slot.availability] || 'info'
}
function requestLabel(status: number) { return ['待审批', '已通过', '已拒绝'][status] || '未知' }
function requestType(status: number) { return (['warning', 'success', 'danger'][status] || 'info') as 'warning' | 'success' | 'danger' | 'info' }

async function loadSchedule() {
  loading.value = true
  try {
    slots.value = await request.get('/teacher/schedule', { params: { from: selectedDate.value, to: selectedDate.value } }) as ScheduleSlot[]
  } catch (error) { ElMessage.error((error as Error).message || '排班加载失败') }
  finally { loading.value = false }
}
async function loadRequests() {
  requestsLoading.value = true
  try { requests.value = await request.get('/teacher/schedule/requests') as CancelRequest[] }
  catch (error) { ElMessage.error((error as Error).message || '申请记录加载失败') }
  finally { requestsLoading.value = false }
}
function openApply(slot: ScheduleSlot) { selectedSlot.value = slot; reason.value = ''; applyVisible.value = true }
async function submitApply() {
  if (!selectedSlot.value || !reason.value.trim()) { ElMessage.warning('请填写取消排班原因'); return }
  submitting.value = true
  try {
    await request.post('/teacher/schedule/requests', { slotId: selectedSlot.value.slotId, reason: reason.value.trim() })
    ElMessage.success('取消排班申请已提交')
    applyVisible.value = false
    await Promise.all([loadSchedule(), loadRequests()])
  } catch (error) { ElMessage.error((error as Error).message || '提交失败') }
  finally { submitting.value = false }
}
onMounted(() => { void loadSchedule(); void loadRequests() })
</script>

<style scoped lang="scss">
.schedule-page { display: grid; gap: 18px; }
.page-head { display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 20px 24px; background: #fff; border: 1px solid #dce5ed; border-radius: 8px; }
.page-head h1 { margin: 0; font-size: 22px; color: #17324d; }
.page-head p { margin: 6px 0 0; color: #718096; font-size: 13px; }
.date-strip { display: flex; gap: 8px; overflow-x: auto; padding: 2px; }
.date-button { display: grid; gap: 5px; min-width: 74px; padding: 10px; border: 1px solid #dce5ed; border-radius: 6px; color: #526579; background: #fff; cursor: pointer; }
.date-button strong { color: #193650; }
.date-button.is-active { border-color: #16835d; background: #eaf6f1; color: #116548; }
.date-button.is-active strong { color: #116548; }
.date-button.is-weekend { opacity: 0.45; cursor: not-allowed; background: #f4f6f8; }
.slot-grid, .request-list { display: grid; gap: 10px; min-height: 160px; }
.slot-row, .request-row { display: flex; align-items: center; gap: 16px; padding: 14px 16px; background: #fff; border: 1px solid #dce5ed; border-radius: 6px; }
.slot-time { display: grid; gap: 2px; min-width: 74px; color: #163a5b; }
.slot-time span, .request-row p, .request-row small { color: #718096; font-size: 12px; }
.slot-info { display: grid; gap: 4px; flex: 1; }
.slot-info span { color: #718096; font-size: 12px; }
.request-row > div { flex: 1; }
.request-row p { margin: 5px 0; }
.dialog-meta { margin-top: 0; color: #53687c; }
@media (max-width: 640px) { .page-head { align-items: flex-start; flex-direction: column; } .slot-row { flex-wrap: wrap; } .slot-info { min-width: 45%; } }
</style>

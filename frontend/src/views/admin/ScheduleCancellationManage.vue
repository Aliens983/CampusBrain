<template>
  <div class="admin-page">
    <header class="page-head">
      <div><h1>排班调整审批</h1><p>审批教师提交的咨询时段取消排班申请</p></div>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </header>
    <section class="summary"><span>待处理申请</span><strong>{{ requests.length }}</strong></section>
    <div v-loading="loading" class="request-list">
      <article v-for="item in requests" :key="item.id" class="request-row">
        <div class="request-main">
          <div class="request-title"><strong>{{ item.teacherName || '教师' }}</strong><el-tag size="small" type="info">{{ item.consultantName }}</el-tag></div>
          <div class="request-meta"><span>{{ item.date }}</span><span>{{ item.startTime }}–{{ item.endTime }}</span><span>{{ formatTime(item.createTime) }}</span></div>
          <p>{{ item.reason }}</p>
        </div>
        <div class="actions">
          <el-button type="success" :loading="busyId === item.id" @click="audit(item, true)">通过</el-button>
          <el-button type="danger" plain :loading="busyId === item.id" @click="audit(item, false)">拒绝</el-button>
        </div>
      </article>
      <el-empty v-if="!loading && requests.length === 0" description="当前没有待审批申请" />
    </div>
  </div>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/common/utils/request'

interface ScheduleRequest {
  id: number
  teacherName?: string
  consultantName: string
  date: string
  startTime: string
  endTime: string
  reason: string
  createTime?: string
}
const requests = ref<ScheduleRequest[]>([])
const loading = ref(false)
const busyId = ref<number | null>(null)
function formatTime(value?: string) { return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '' }
async function load() {
  loading.value = true
  try { requests.value = await request.get('/admin/schedule-cancellations') as ScheduleRequest[] }
  catch (error) { ElMessage.error((error as Error).message || '待审批列表加载失败') }
  finally { loading.value = false }
}
async function audit(item: ScheduleRequest, approve: boolean) {
  let remark: string | undefined
  try {
    const result = await ElMessageBox.prompt(approve ? '审批意见（可选）' : '请填写拒绝原因', approve ? '通过取消排班申请' : '拒绝取消排班申请', {
      inputType: 'textarea', inputPlaceholder: '审批意见',
      inputValidator: (value: string) => approve || Boolean(value.trim()) || '拒绝时请填写原因',
      confirmButtonText: approve ? '确认通过' : '确认拒绝',
      type: approve ? 'success' : 'warning',
    })
    remark = result.value
  } catch { return }
  busyId.value = item.id
  try {
    const suffix = approve ? 'approve' : 'reject'
    await request.patch(`/admin/schedule-cancellations/${item.id}/${suffix}`, { remark })
    ElMessage.success(approve ? '申请已通过，该时段已取消' : '申请已拒绝')
    await load()
  } catch (error) { ElMessage.error((error as Error).message || '审批失败') }
  finally { busyId.value = null }
}
onMounted(load)
</script>

<style scoped lang="scss">
.admin-page { display: grid; gap: 16px; }
.page-head { display: flex; align-items: center; justify-content: space-between; padding: 20px 24px; background: #fff; border: 1px solid #dce5ed; border-radius: 6px; }
.page-head h1 { margin: 0; font-size: 22px; color: #17324d; }
.page-head p { margin: 6px 0 0; color: #718096; font-size: 13px; }
.summary { display: flex; align-items: center; gap: 18px; padding: 12px 16px; background: #eff7f3; color: #275a43; border-left: 3px solid #248259; }
.summary strong { font-size: 20px; }
.request-list { display: grid; gap: 10px; min-height: 180px; }
.request-row { display: flex; align-items: center; gap: 20px; padding: 16px; background: #fff; border: 1px solid #dce5ed; border-radius: 6px; }
.request-main { flex: 1; min-width: 0; }
.request-title, .request-meta, .actions { display: flex; align-items: center; gap: 10px; }
.request-title strong { color: #18344d; }
.request-meta { flex-wrap: wrap; margin-top: 9px; color: #75879a; font-size: 12px; }
.request-main p { margin: 10px 0 0; color: #40576d; white-space: pre-wrap; overflow-wrap: anywhere; }
@media (max-width: 680px) { .request-row { align-items: flex-start; flex-direction: column; } .actions { width: 100%; } }
</style>

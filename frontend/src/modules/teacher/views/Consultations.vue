<template>
  <div class="teacher-page">
    <section class="page-head">
      <h1>我的咨询</h1>
      <p>我名下咨询档期被学生预约的情况（可切换查看处理状态）。</p>
    </section>

    <el-card class="list-card">
      <template #header>
        <div class="section-head">
          <el-radio-group
            v-model="status"
            size="small"
            @change="load"
          >
            <el-radio-button
              label="all"
              value="all"
            >全部</el-radio-button>
            <el-radio-button
              label="pending"
              value="pending"
            >待我审核</el-radio-button>
            <el-radio-button
              label="approved"
              value="approved"
            >已通过</el-radio-button>
            <el-radio-button
              label="rejected"
              value="rejected"
            >已拒绝</el-radio-button>
            <el-radio-button
              label="completed"
              value="completed"
            >已完成</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <div
        v-loading="loading"
        class="item-list"
      >
        <el-empty
          v-if="!loading && list.length === 0"
          description="暂无相关预约"
        />
        <div
          v-for="b in list"
          :key="b.orderId"
          class="row"
        >
          <div class="row__main">
            <div class="row__head">
              <strong>{{ b.username || '学生' }}</strong>
              <el-tag
                :type="tagType(b.manageStatus)"
                size="small"
              >
                {{ statusText(b.manageStatus) }}
              </el-tag>
              <el-tag
                size="small"
                type="info"
              >
                {{ [campusName(b.campus), b.consultantName].filter(Boolean).join(' · ') }}
              </el-tag>
            </div>
            <div class="row__meta">
              <span>{{ b.serviceName }}</span>
              <span>{{ b.slotDate }} {{ b.startTime }}–{{ b.endTime }}</span>
              <span
                v-if="b.reason"
                class="reason"
              >备注：{{ b.reason }}</span>
            </div>
          </div>
          <div class="row__action">
            <el-button
              size="small"
              plain
              :disabled="!b.userId"
              :loading="chatBusyId === b.orderId"
              @click="chatStudent(b)"
            >
              回复
            </el-button>
            <template v-if="b.manageStatus === 0">
              <el-button
                type="primary"
                size="small"
                :loading="busy === b.orderId"
                @click="doApprove(b)"
              >
                通过
              </el-button>
              <el-button
                type="danger"
                plain
                size="small"
                :loading="busy === b.orderId"
                @click="doReject(b)"
              >
                拒绝
              </el-button>
            </template>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  approveBooking,
  campusName,
  fetchTeacherBookings,
  rejectBooking,
  type TeacherBooking,
} from '../composables'
import { openChatWithStudent } from '@/common/consultChat'

const router = useRouter()
const status = ref<'all' | 'pending' | 'approved' | 'rejected' | 'completed'>('all')
const list = ref<TeacherBooking[]>([])
const loading = ref(false)
const busy = ref<number | null>(null)
const chatBusyId = ref<number | null>(null)

function toNum(): number | undefined {
  if (status.value === 'pending') return 0
  if (status.value === 'approved') return 1
  if (status.value === 'rejected') return 2
  if (status.value === 'completed') return 4
  return undefined
}

async function load() {
  loading.value = true
  try {
    list.value = await fetchTeacherBookings(toNum())
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '获取列表失败')
  } finally {
    loading.value = false
  }
}

function statusText(s?: number): string {
  return s === 0 ? '待我审核' : s === 1 ? '已通过' : s === 2 ? '已拒绝' : s === 3 ? '已取消' : s === 4 ? '已完成' : ''
}

function tagType(s?: number): 'warning' | 'success' | 'danger' | 'info' {
  return s === 0 ? 'warning' : s === 1 ? 'success' : s === 2 ? 'danger' : 'info'
}

async function chatStudent(b: TeacherBooking) {
  if (b.userId == null) return
  chatBusyId.value = b.orderId ?? b.userId
  try {
    const conv = await openChatWithStudent(b.userId)
    router.push({ path: `/teacher/messages/${conv.id}`, query: { name: conv.peerName } })
  } catch (error: unknown) {
    const err = error as { isAxiosError?: boolean; message?: string }
    if (!err?.isAxiosError) ElMessage.error(err?.message || '发起沟通失败，请稍后重试')
  } finally {
    chatBusyId.value = null
  }
}

async function doApprove(b: TeacherBooking) {
  if (b.orderId == null) return
  busy.value = b.orderId
  try {
    await approveBooking(b.orderId)
    await load()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '操作失败')
  } finally {
    busy.value = null
  }
}

async function doReject(b: TeacherBooking) {
  if (b.orderId == null) return
  busy.value = b.orderId
  try {
    await rejectBooking(b.orderId)
    await load()
  } catch (error: unknown) {
    const err = error as { message?: string }
    if (err.message !== 'cancel') ElMessage.error(err.message || '操作失败')
  } finally {
    busy.value = null
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.teacher-page {
  display: grid;
  gap: 16px;
}

.page-head {
  padding: 22px 26px;
  border-radius: 24px;
  color: #fff;
  background: linear-gradient(135deg, #0e6cd6, #3fb6ff 60%, #ade2ff);
  box-shadow: var(--shadow-card);
}
.page-head h1 {
  margin: 0 0 6px;
  font-size: 24px;
}
.page-head p {
  margin: 0;
  color: rgba(255, 255, 255, 0.86);
  font-size: 13px;
  line-height: 1.7;
}

.list-card {
  border-radius: 18px;
  border: 1px solid var(--border-soft);
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-list {
  min-height: 120px;
  display: grid;
  gap: 10px;
}
.row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid var(--border-soft);
  background: linear-gradient(180deg, #fff, #f9fcff);
}
.row__main {
  flex: 1;
  display: grid;
  gap: 6px;
}
.row__head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.row__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 16px;
  color: var(--text-secondary);
  font-size: 13px;
}
.reason {
  color: var(--text-tertiary);
}
.row__action {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
</style>

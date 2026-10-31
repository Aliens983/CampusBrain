<template>
  <div class="teacher-page">
    <section class="page-head">
      <div>
        <p class="hz-eyebrow">
          My consultations · HZNU
        </p>
        <h1>我的咨询</h1>
        <p>我名下咨询档期被学生预约的情况（可切换查看处理状态）。</p>
      </div>
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
    // prompt 取消时 reject 的是字符串 'cancel'/'close'（无 .message），
    // 按 err.message 判断恒为真 → 误报「操作失败」
    if (error === 'cancel' || error === 'close') return
    const err = error as { message?: string }
    ElMessage.error(err?.message || '操作失败')
  } finally {
    busy.value = null
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.teacher-page {
  display: grid;
  gap: 18px;
}

.list-card {
  border-radius: var(--radius-lg);
}

.item-list {
  display: grid;
  gap: 10px;
  min-height: 120px;
}

.row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 15px 18px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface-2);
  transition: border-color 0.16s ease, background-color 0.16s ease;
}

.row:hover {
  border-color: rgba(176, 138, 69, 0.36);
  background: var(--surface);
}

.row__main {
  display: grid;
  flex: 1;
  gap: 6px;
  min-width: 0;
}

.row__head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.row__head strong {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink);
}

.row__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 16px;
  color: var(--ink-3);
  font-size: 12px;
}

.reason {
  color: var(--ink-4);
}

.row__action {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
}

@media (max-width: 700px) {
  .row {
    flex-wrap: wrap;
  }

  .row__action {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>

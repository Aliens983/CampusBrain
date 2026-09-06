<template>
  <div class="teacher-page">
    <section class="page-head">
      <h1>待我审核</h1>
      <p>学生预约我名下咨询档期的申请。通过后该时段锁定；拒绝需填原因，时段会释放给他人再约。</p>
    </section>

    <el-card class="list-card">
      <div
        v-loading="loading"
        class="item-list"
      >
        <el-empty
          v-if="!loading && list.length === 0"
          description="暂时没有待你审核的咨询申请"
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
                size="small"
                type="info"
              >
                {{ [campusName(b.campus), b.consultantName].filter(Boolean).join(' · ') }}
              </el-tag>
            </div>
            <div class="row__meta">
              <span>{{ b.serviceName || '咨询服务' }}</span>
              <span>{{ b.slotDate }} {{ b.startTime }}–{{ b.endTime }}</span>
              <span
                v-if="b.createTime"
                class="muted"
              >{{ (b.createTime || '').replace('T', ' ') }}</span>
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
const list = ref<TeacherBooking[]>([])
const loading = ref(false)
const busy = ref<number | null>(null)
const chatBusyId = ref<number | null>(null)

async function load() {
  loading.value = true
  try {
    list.value = await fetchTeacherBookings(0)
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '获取待审核列表失败')
  } finally {
    loading.value = false
  }
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
.muted {
  color: var(--text-tertiary);
}
.row__action {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
</style>

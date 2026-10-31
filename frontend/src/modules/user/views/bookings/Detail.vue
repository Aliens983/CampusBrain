<template>
  <div class="page-shell">
    <div class="detail-hero">
      <button
        class="back-btn"
        @click="router.back()"
      >
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </button>
      <div class="detail-hero__copy">
        <p class="hz-eyebrow">
          Booking detail · HZNU
        </p>
        <h1>{{ booking?.serviceName || '加载中...' }}</h1>
        <p v-if="booking">
          {{ booking.bookingNo }} · {{ booking.date }} {{ booking.timeRange }}
        </p>
      </div>
    </div>

    <el-card
      v-if="booking"
      class="panel-card"
    >
      <h2 class="detail-card__title">
        预约信息
      </h2>
      <div class="detail-grid">
        <div class="detail-row">
          <span>预约编号</span><strong>{{ booking.bookingNo }}</strong>
        </div>
        <div class="detail-row">
          <span>服务名称</span><strong>{{ booking.serviceName }}</strong>
        </div>
        <div
          v-if="booking.campus"
          class="detail-row"
        >
          <span>校区</span><strong>{{ campusName(booking.campus) }}</strong>
        </div>
        <div
          v-if="booking.consultantName"
          class="detail-row"
        >
          <span>咨询师</span><strong>{{ booking.consultantName }}</strong>
        </div>
        <div
          v-if="booking.equipmentName"
          class="detail-row"
        >
          <span>借用设备</span><strong>{{ booking.equipmentName }} × {{ booking.quantity || 1 }}</strong>
        </div>
        <div
          v-if="booking.roomName"
          class="detail-row"
        >
          <span>教室</span><strong>{{ booking.roomName }}</strong>
        </div>
        <div class="detail-row">
          <span>预约日期</span><strong>{{ booking.date }}</strong>
        </div>
        <div class="detail-row">
          <span>时段</span><strong>{{ booking.timeRange }}</strong>
        </div>
        <div class="detail-row">
          <span>地点</span><strong>{{ booking.location }}</strong>
        </div>
        <div class="detail-row">
          <span>申请人</span><strong>{{ booking.applicant }}</strong>
        </div>
        <div class="detail-row">
          <span>部门</span><strong>{{ booking.department }}</strong>
        </div>
        <div class="detail-row">
          <span>状态</span>
          <el-tag
            :type="statusTag(booking.status)"
            effect="plain"
            size="small"
          >
            {{ statusText(booking.status) }}
          </el-tag>
        </div>
        <div class="detail-row">
          <span>备注</span><strong>{{ booking.remarks || '暂无' }}</strong>
        </div>
        <div class="detail-row">
          <span>创建时间</span><strong>{{ booking.createdAt }}</strong>
        </div>
      </div>
    </el-card>

    <div
      v-if="booking && canCancel"
      class="detail-actions"
    >
      <el-button
        type="danger"
        plain
        :loading="cancelling"
        @click="cancelBooking"
      >
        取消{{ booking.type === 'activity' && booking.status === 'approved' ? '报名' : '预约' }}
      </el-button>
      <span
        v-if="booking.type === 'activity' && booking.status === 'approved'"
        class="detail-actions__tip"
      >活动为先到先得，取消后名额即时释放，可被他人再约。</span>
    </div>

    <div
      v-if="booking?.consultantName"
      class="detail-actions"
    >
      <el-button
        type="primary"
        plain
        :loading="chatBusy"
        @click="openConsultChat"
      >
        <el-icon><ChatDotRound /></el-icon>
        <span>联系咨询老师</span>
      </el-button>
      <span class="detail-actions__tip">与咨询教师在线上留言沟通，仅教师咨询开放。</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ChatDotRound } from '@element-plus/icons-vue'
import { fetchBookingRecords } from '@/common/campus'
import request from '@/common/utils/request'
import type { BookingRecord, BookingStatus } from '@/common/types'
import { openChatByBooking } from '@/common/consultChat'

const router = useRouter()
const route = useRoute()
const booking = ref<BookingRecord | null>(null)
const cancelling = ref(false)
const chatBusy = ref(false)

// 可取消：待确认(处理中)可取消；已通过的活动（先到先得）开始前可自助取消
const canCancel = computed(() => {
  const b = booking.value
  if (!b) return false
  if (b.status === 'pending') return true
  return b.status === 'approved' && b.type === 'activity'
})

onMounted(load)

async function load() {
  const id = Number(route.params.id)
  try {
    const records = await fetchBookingRecords()
    booking.value = records.find(r => r.id === id) || null
    if (!booking.value) ElMessage.warning('未找到该预约记录')
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '获取预约详情失败')
  }
}

async function openConsultChat() {
  if (!booking.value) return
  chatBusy.value = true
  try {
    const conv = await openChatByBooking(booking.value.id)
    router.push({ path: `/chat/${conv.id}`, query: { name: conv.peerName } })
  } catch (error: unknown) {
    const err = error as { isAxiosError?: boolean; message?: string }
    if (!err?.isAxiosError) ElMessage.error(err?.message || '发起沟通失败，请稍后重试')
  } finally {
    chatBusy.value = false
  }
}

async function cancelBooking() {
  if (!booking.value) return
  try {
    await ElMessageBox.confirm('确定取消该预约吗？取消后如需再约请重新申请。', '取消确认', { type: 'warning' })
  } catch {
    return
  }
  cancelling.value = true
  try {
    await request.patch(`/app/bookings/${booking.value.id}/cancel`)
    ElMessage.success('已取消')
    await load()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '取消失败')
  } finally {
    cancelling.value = false
  }
}

function statusTag(status: BookingStatus) {
  return { pending: 'warning', approved: 'success', rejected: 'danger', completed: 'info', cancelled: 'info' }[status]
}
function statusText(status: BookingStatus) {
  return { pending: '处理中', approved: '已通过', rejected: '已驳回', completed: '已完成', cancelled: '已取消' }[status]
}
function campusName(c?: string) {
  return c === 'cq' ? '仓前' : c === 'xs' ? '下沙' : ''
}
</script>

<style scoped lang="scss">
.detail-hero {
  position: relative;
  padding: 26px 32px 28px;
  border-radius: var(--radius-2xl);
  color: #f7f1e4;
  background:
    radial-gradient(ellipse 60% 90% at 90% 10%, rgba(63, 182, 255, 0.12), transparent 60%),
    linear-gradient(135deg, #16323c 0%, #21434d 58%, #2e5b68 100%);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.detail-hero::after {
  content: "";
  position: absolute;
  top: -100px;
  right: -80px;
  width: 280px;
  height: 280px;
  border: 1px solid rgba(214, 193, 154, 0.22);
  border-radius: 50%;
  box-shadow: 0 0 0 30px rgba(214, 193, 154, 0.03);
  pointer-events: none;
}

.detail-hero > * {
  position: relative;
  z-index: 1;
}

.back-btn {
  margin-bottom: 16px;
}

.detail-hero__copy .hz-eyebrow {
  color: var(--gold-soft);
  font-size: 9px;
  letter-spacing: 0.2em;
}

.detail-hero h1 {
  margin: 8px 0 6px;
  font-family: var(--font-serif);
  font-size: 30px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: #f8f2e6;
}

.detail-hero p {
  margin: 0;
  color: rgba(247, 241, 228, 0.62);
  font-size: 12.5px;
}

.detail-card__title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 6px;
  font-family: var(--font-serif);
  font-size: 19px;
  font-weight: 600;
  color: var(--ink);
}

.detail-card__title::before {
  content: "";
  width: 3px;
  height: 17px;
  border-radius: 1px;
  background: var(--gold);
}

.detail-grid {
  display: grid;
}

.detail-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 13px 0;
  border-top: 1px solid var(--line);
}

.detail-row span {
  width: 92px;
  flex-shrink: 0;
  color: var(--ink-4);
  font-size: 11.5px;
  letter-spacing: 0.04em;
}

.detail-row strong {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
}

.detail-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 2px 4px;
}

.detail-actions__tip {
  font-size: 11.5px;
  color: var(--ink-4);
}

@media (max-width: 700px) {
  .detail-hero {
    padding: 22px 24px 24px;
  }

  .detail-hero h1 {
    font-size: 24px;
  }

  .detail-row {
    align-items: flex-start;
  }

  .detail-row span {
    width: 78px;
  }

  .detail-actions {
    flex-wrap: wrap;
  }
}
</style>
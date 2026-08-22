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
      <div>
        <span class="detail-hero__chip">预约详情</span>
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
      <div class="detail-grid">
        <div class="detail-row">
          <span>预约编号</span><strong>{{ booking.bookingNo }}</strong>
        </div>
        <div class="detail-row">
          <span>服务名称</span><strong>{{ booking.serviceName }}</strong>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { fetchBookingRecords } from '@/common/campus'
import type { BookingRecord, BookingStatus } from '@/common/types'

const router = useRouter()
const route = useRoute()
const booking = ref<BookingRecord | null>(null)

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    const records = await fetchBookingRecords()
    booking.value = records.find(r => r.id === id) || null
    if (!booking.value) ElMessage.warning('未找到该预约记录')
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '获取预约详情失败')
  }
})

function statusTag(status: BookingStatus) {
  return { pending: 'warning', approved: 'success', rejected: 'danger', completed: 'info', cancelled: 'info' }[status]
}
function statusText(status: BookingStatus) {
  return { pending: '待审核', approved: '已通过', rejected: '已驳回', completed: '已完成', cancelled: '已取消' }[status]
}
</script>

<style scoped lang="scss">
.detail-hero {
  padding: 28px 32px;
  border-radius: 24px;
  background: linear-gradient(135deg, #0e2647, #1458d4 62%, #52a1ff);
  color: #fff;
}
.back-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 6px 14px; border: 1px solid rgba(255,255,255,.2); border-radius: 8px;
  background: rgba(255,255,255,.08); color: rgba(255,255,255,.9);
  font-size: 13px; cursor: pointer; margin-bottom: 20px;
  transition: background .2s, border-color .2s;
}
.back-btn:hover { background: rgba(255,255,255,.16); border-color: rgba(255,255,255,.35); }
.detail-hero__chip { display: inline-block; padding: 4px 10px; border-radius: 999px; font-size: 11px; letter-spacing: .06em; background: rgba(255,255,255,.14); margin-bottom: 10px; }
.detail-hero h1 { margin: 0 0 6px; font-size: 28px; font-weight: 700; }
.detail-hero p { margin: 0; color: rgba(255,255,255,.7); font-size: 14px; }

.detail-grid { display: grid; gap: 4px; }
.detail-row { display: flex; gap: 16px; padding: 14px 0; border-bottom: 1px solid var(--border-soft); align-items: center; }
.detail-row:last-child { border-bottom: none; }
.detail-row span { width: 90px; color: var(--text-secondary); font-size: 13px; flex-shrink: 0; }
.detail-row strong { flex: 1; font-size: 14px; }
</style>
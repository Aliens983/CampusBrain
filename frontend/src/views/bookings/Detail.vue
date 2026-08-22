<template>
  <div
    v-if="detail"
    class="page-shell"
  >
    <section class="page-hero">
      <div>
        <div class="status-pill is-brand">
          {{ detail.bookingNo }}
        </div>
        <h1 class="page-hero__title">
          {{ detail.serviceName }}
        </h1>
        <p class="page-hero__desc">
          预约详情、审批进度与状态追踪。
        </p>
      </div>
      <el-tag
        size="large"
        :type="tagType"
      >
        {{ statusLabel }}
      </el-tag>
    </section>

    <section class="detail-overview">
      <article class="overview-card">
        <span>预约日期</span>
        <strong>{{ detail.date }}</strong>
        <small>{{ detail.timeRange }}</small>
      </article>
      <article class="overview-card">
        <span>申请人</span>
        <strong>{{ detail.applicant }}</strong>
        <small>{{ detail.department }}</small>
      </article>
      <article class="overview-card">
        <span>预约地点</span>
        <strong>{{ detail.location }}</strong>
        <small>可继续扩展地图或资源位置信息</small>
      </article>
    </section>

    <section class="grid-cards">
      <el-card class="panel-card span-7">
        <template #header>
          <div class="section-head">
            <h3 class="section-head__title">
              预约信息
            </h3>
          </div>
        </template>
        <div class="info-list">
          <div class="info-row">
            <span>申请人</span><strong>{{ detail.applicant }}</strong>
          </div>
          <div class="info-row">
            <span>部门</span><strong>{{ detail.department }}</strong>
          </div>
          <div class="info-row">
            <span>地点</span><strong>{{ detail.location }}</strong>
          </div>
          <div class="info-row">
            <span>预约日期</span><strong>{{ detail.date }}</strong>
          </div>
          <div class="info-row">
            <span>预约时段</span><strong>{{ detail.timeRange }}</strong>
          </div>
          <div class="info-row">
            <span>提交时间</span><strong>{{ detail.createdAt }}</strong>
          </div>
          <div class="info-row">
            <span>用途说明</span><strong>{{ detail.remarks || '暂无' }}</strong>
          </div>
        </div>
      </el-card>

      <el-card class="panel-card span-5">
        <template #header>
          <div class="section-head">
            <h3 class="section-head__title">
              审批轨迹
            </h3>
          </div>
        </template>
        <div class="timeline-shell">
          <el-timeline>
            <el-timeline-item timestamp="提交申请">
              已进入统一预约受理队列。
            </el-timeline-item>
            <el-timeline-item timestamp="部门审核">
              根据业务类型流转到对应管理员审核环节。
            </el-timeline-item>
            <el-timeline-item timestamp="结果通知">
              审批结果将推送至消息中心，并通过站内信和邮件及时通知您。
            </el-timeline-item>
          </el-timeline>
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { bookingAPI } from '@/services/api'
import type { BookingRecord } from '@/types'

const route = useRoute()
const detail = ref<BookingRecord | null>(null)
const loading = ref(true)

onMounted(async () => {
  try {
    const id = String(route.params.id)
    const data = await bookingAPI.getBooking(id) as any
    if (data) {
      detail.value = data?.data || data
    }
  } catch {
    // 如果API调用失败，detail 保持 null
  } finally {
    loading.value = false
  }
})

const tagType = computed(() => ({ pending: 'warning', approved: 'success', rejected: 'danger', completed: 'info', cancelled: 'info' }[detail.value?.status || 'pending']))
const statusLabel = computed(() => ({ pending: '待审核', approved: '已通过', rejected: '已驳回', completed: '已完成', cancelled: '已取消' }[detail.value?.status || 'pending']))
</script>

<style scoped lang="scss">
.detail-overview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.overview-card {
  position: relative;
  display: grid;
  gap: 8px;
  padding: 22px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid var(--border-soft);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  transition: transform 0.26s ease, box-shadow 0.26s ease;
}

.overview-card::after {
  content: '';
  position: absolute;
  inset: auto -24px -24px auto;
  width: 92px;
  height: 92px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(20, 88, 212, 0.08), rgba(20, 88, 212, 0));
}

.overview-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-card-hover);
}

.overview-card span,
.overview-card small {
  color: var(--text-secondary);
}

.overview-card strong {
  font-size: 28px;
  line-height: 1.3;
}

.span-7 {
  grid-column: span 7;
}

.span-5 {
  grid-column: span 5;
}

.timeline-shell {
  padding-top: 4px;
}

@media (max-width: 1200px) {
  .detail-overview {
    grid-template-columns: 1fr;
  }

  .span-7,
  .span-5 {
    grid-column: span 12;
  }
}
</style>

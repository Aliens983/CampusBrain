<template>
  <div
    v-if="service"
    class="page-shell"
  >
    <section class="page-hero">
      <div>
        <div class="status-pill is-brand">
          {{ service.code }}
        </div>
        <h1 class="page-hero__title">
          {{ service.name }}
        </h1>
        <p class="page-hero__desc">
          {{ service.description }}
        </p>
      </div>
      <div class="hero-summary">
        <div><span>业务分类</span><strong>{{ service.category }}</strong></div>
        <div><span>覆盖区域</span><strong>{{ service.location }}</strong></div>
        <div><span>使用说明</span><strong>{{ service.priceLabel }}</strong></div>
      </div>
    </section>

    <section class="grid-cards">
      <el-card class="panel-card span-8">
        <template #header>
          <div class="section-head">
            <h3 class="section-head__title">
              模块能力说明
            </h3>
          </div>
        </template>
        <el-timeline>
          <el-timeline-item
            v-for="step in moduleSteps"
            :key="step.title"
            :timestamp="step.phase"
            placement="top"
          >
            <strong>{{ step.title }}</strong>
            <p class="muted">
              {{ step.desc }}
            </p>
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <el-card class="panel-card span-4">
        <template #header>
          <div class="section-head">
            <h3 class="section-head__title">
              服务详情
            </h3>
          </div>
        </template>
        <div class="info-list">
          <div class="info-row">
            <span>服务编号</span><strong>{{ service.code }}</strong>
          </div>
          <div class="info-row">
            <span>服务分类</span><strong>{{ service.category }}</strong>
          </div>
          <div class="info-row">
            <span>所在位置</span><strong>{{ service.location }}</strong>
          </div>
          <div class="info-row">
            <span>当前状态</span><strong>{{ service.status === 'available' ? '可预约' : '维护中' }}</strong>
          </div>
        </div>
        <el-divider />
        <div class="tag-wrap">
          <el-tag
            v-for="tag in service.tags"
            :key="tag"
            round
          >
            {{ tag }}
          </el-tag>
        </div>
        <div class="button-group">
          <el-button
            type="primary"
            class="go-btn"
            @click="jumpToModule"
          >
            进入业务页
          </el-button>
          <el-button
            plain
            class="go-btn"
            @click="showRelatedResources"
          >
            查看相关资源
          </el-button>
        </div>
      </el-card>
    </section>

    <el-dialog
      v-model="relatedVisible"
      title="相关资源"
      width="620px"
    >
      <div class="dialog-list">
        <div
          v-for="item in relatedItems"
          :key="item.title"
          class="dialog-card"
        >
          <strong>{{ item.title }}</strong>
          <p>{{ item.desc }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { roomAPI, equipmentAPI, consultationAPI } from '@/services/api'
import type { Consultant, EquipmentResource, RoomResource, ServiceCard } from '@/types'

const route = useRoute()
const router = useRouter()
const relatedVisible = ref(false)
const loading = ref(true)
const service = ref<ServiceCard | null>(null)
const rooms = ref<RoomResource[]>([])
const equipments = ref<EquipmentResource[]>([])
const consultants = ref<Consultant[]>([])

onMounted(async () => {
  try {
    const id = Number(route.params.id)
    // 并行获取所有数据
    const [serviceData, roomsData, equipmentData, consultationData] = await Promise.allSettled([
      roomAPI.getRoom(String(id)) as Promise<any>,
      roomAPI.getRooms({}) as Promise<any>,
      equipmentAPI.getEquipment({}) as Promise<any>,
      consultationAPI.getConsultants({}) as Promise<any>,
    ])

    if (serviceData.status === 'fulfilled' && serviceData.value) {
      service.value = serviceData.value?.data || serviceData.value
    }

    if (roomsData.status === 'fulfilled' && roomsData.value) {
      const list = Array.isArray(roomsData.value) ? roomsData.value : (roomsData.value?.records || roomsData.value?.data || [])
      rooms.value = Array.isArray(list) ? list : []
    }

    if (equipmentData.status === 'fulfilled' && equipmentData.value) {
      const list = Array.isArray(equipmentData.value) ? equipmentData.value : (equipmentData.value?.records || equipmentData.value?.data || [])
      equipments.value = Array.isArray(list) ? list : []
    }

    if (consultationData.status === 'fulfilled' && consultationData.value) {
      const list = Array.isArray(consultationData.value) ? consultationData.value : (consultationData.value?.records || consultationData.value?.data || [])
      consultants.value = Array.isArray(list) ? list : []
    }
  } catch {
    // fallback
  } finally {
    loading.value = false
  }
})

const moduleSteps = [
  { phase: '01', title: '服务展示', desc: '面向全校师生展示可用资源、服务能力与预约规则。' },
  { phase: '02', title: '在线预约', desc: '选择时间、填写用途后提交预约申请，即时确认。' },
  { phase: '03', title: '审批处理', desc: '管理人员在线审核申请，支持通过、驳回与意见反馈。' },
  { phase: '04', title: '消息通知', desc: '审批结果和系统提醒实时推送至消息中心。' },
]

const relatedItems = computed(() => {
  if (!service.value) return []
  if (service.value.type === 'room') {
    return rooms.value.map((item) => ({ title: item.name, desc: `${item.building} ${item.floor} / ${item.capacity} 人` }))
  }
  if (service.value.type === 'equipment') {
    return equipments.value.map((item) => ({ title: item.name, desc: `${item.location} / 可借 ${item.availableStock} ${item.unit}` }))
  }
  if (service.value.type === 'consultation') {
    return consultants.value.map((item) => ({ title: item.name, desc: `${item.title} / ${item.nextSlot}` }))
  }
  return []
})

function jumpToModule() {
  if (!service.value) return
  const pathMap: Record<string, string> = {
    room: '/rooms',
    equipment: '/equipment',
    consultation: '/consultation',
    printing: '/services',
  }
  router.push(pathMap[service.value.type] || '/services')
}

function showRelatedResources() {
  relatedVisible.value = true
}
</script>

<style scoped lang="scss">
.hero-summary { display: grid; gap: 12px; min-width: 280px; }
.hero-summary div { display: grid; gap: 6px; padding: 16px 18px; border-radius: 18px; background: rgba(255,255,255,.12); transition: transform .24s ease, background-color .24s ease; }
.hero-summary div:hover { transform: translateY(-3px); background: rgba(255,255,255,.18); }
.hero-summary span { color: rgba(255,255,255,.72); font-size: 12px; }
.span-8 { grid-column: span 8; }
.span-4 { grid-column: span 4; }
.tag-wrap { display: flex; flex-wrap: wrap; gap: 8px; }
.button-group { display: grid; gap: 12px; margin-top: 18px; }
.go-btn { width: 100%; }
.dialog-list { display: grid; gap: 12px; }
.dialog-card { padding: 16px; border-radius: 18px; background: linear-gradient(180deg, #fff, #f8fbff); border: 1px solid var(--border-soft); }
.dialog-card p { margin: 6px 0; color: var(--text-secondary); }
@media (max-width: 1200px) { .span-8, .span-4 { grid-column: span 12; } }
</style>

<template>
  <div class="page-shell">
    <section class="page-hero">
      <el-button
        text
        @click="router.back()"
      >
        ← 返回
      </el-button>
      <h1 class="page-hero__title">
        服务详情
      </h1>
    </section>

    <el-card
      v-if="service"
      class="panel-card"
    >
      <div class="service-detail">
        <div class="service-detail__cover">
          <div
            class="cover-badge"
            :class="service.image"
          >
            {{ service.code }}
          </div>
          <div>
            <h3>{{ service.name }}</h3>
            <p class="muted">
              {{ service.description }}
            </p>
          </div>
        </div>
        <div class="info-list">
          <div class="info-row">
            <span>业务类别</span><strong>{{ service.category }}</strong>
          </div>
          <div class="info-row">
            <span>服务范围</span><strong>{{ service.location }}</strong>
          </div>
          <div class="info-row">
            <span>状态</span>
            <el-tag :type="service.status === 'available' ? 'success' : 'warning'">
              {{ service.status === 'available' ? '可预约' : '维护中' }}
            </el-tag>
          </div>
        </div>

        <!-- 咨询类服务：选咨询师 + 选时段后提交（真实时段预约） -->
        <template v-if="consultants.length">
          <el-divider content-position="left">
            选择咨询师与时段
          </el-divider>

          <div class="consult-grid">
            <button
              v-for="c in consultants"
              :key="c.id"
              class="consult-card"
              :class="{ 'is-active': selected?.id === c.id }"
              @click="selectConsultant(c)"
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
              </div>
              <span class="consult-card__rating">
                {{ c.rating ?? '—' }}
              </span>
            </button>
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
              @click="submitConsultation"
            >
              {{ bookingSlotId ? '预约该时段' : '请先选择一个时段' }}
            </el-button>
          </div>
        </template>

        <!-- 普通服务：走通用预约 -->
        <el-button
          v-else
          type="primary"
          size="large"
          :loading="booking"
          @click="handleBook"
        >
          立即预约
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import request from '@/common/utils/request'
import { fetchServiceCards } from '@/common/campus'
import type { ServiceCard } from '@/common/types'

interface ConsultantLite {
  id: number
  name: string
  title: string
  department: string
  expertise?: string[]
  rating?: number
  reviews?: number
}
interface SlotLite {
  slotId: number
  startTime: string
  endTime: string
}

const router = useRouter()
const route = useRoute()
const service = ref<ServiceCard | null>(null)
const booking = ref(false)
const consultants = ref<ConsultantLite[]>([])
const selected = ref<ConsultantLite | null>(null)
const slots = ref<SlotLite[]>([])
const bookingSlotId = ref<number | null>(null)
const loadingSlots = ref(false)
const submitting = ref(false)

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    const services = await fetchServiceCards()
    service.value = services.find(s => s.id === id) || null
    if (!service.value) {
      ElMessage.warning('未找到该服务')
      return
    }
    await loadConsultants(id)
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '获取服务详情失败')
  }
})

/** 该服务是否配置了咨询师：有则走「选咨询师+时段」的真实预约 */
async function loadConsultants(serviceId: number) {
  try {
    const res = await request.get('/app/consultations', {
      params: { serviceId, pageNo: 1, pageSize: 100 },
    }) as { records?: ConsultantLite[] } | ConsultantLite[]
    const list = Array.isArray(res) ? res : res?.records || []
    consultants.value = (list || []).map(c => ({
      id: c.id,
      name: c.name || '咨询师',
      title: c.title || '',
      department: c.department || '',
      expertise: c.expertise || [],
      rating: c.rating,
      reviews: c.reviews,
    }))
  } catch {
    consultants.value = []
  }
}

function selectConsultant(c: ConsultantLite) {
  selected.value = c
  bookingSlotId.value = null
  slots.value = []
  void loadSlots(c.id)
}

async function loadSlots(consultantId: number) {
  loadingSlots.value = true
  try {
    const today = new Date().toISOString().slice(0, 10)
    const res = await request.get(`/app/consultations/${consultantId}/slots`, {
      params: { date: today },
    }) as SlotLite[] | unknown
    slots.value = (Array.isArray(res) ? res : []) as SlotLite[]
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '获取可约时段失败')
    slots.value = []
  } finally {
    loadingSlots.value = false
  }
}

async function submitConsultation() {
  if (!selected.value || !bookingSlotId.value) return
  submitting.value = true
  try {
    await request.post(`/app/consultations/${selected.value.id}/book`, {
      slotId: bookingSlotId.value,
    })
    ElMessage.success('预约成功，等待管理员审核')
    router.push('/bookings')
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '预约失败，请重试')
  } finally {
    submitting.value = false
  }
}

async function handleBook() {
  if (!service.value) return
  booking.value = true
  try {
    await request.post('/app/bookings', {
      serviceIds: [service.value.id],
    })
    ElMessage.success('预约已提交成功')
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '预约提交失败')
  } finally {
    booking.value = false
  }
}
</script>

<style scoped lang="scss">
.service-detail { display: grid; gap: 18px; max-width: 720px; }
.service-detail__cover { display: flex; align-items: center; gap: 16px; }
.service-detail__cover h3 { margin: 0 0 4px; font-size: 20px; }
.service-detail__cover .muted { margin: 0; }
.cover-badge { width: 72px; height: 72px; border-radius: 18px; display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: 700; color: #fff; flex-shrink: 0; }
.info-list { display: grid; gap: 8px; }
.info-row { display: flex; justify-content: space-between; align-items: center; font-size: 14px; padding: 6px 0; border-bottom: 1px dashed var(--border-soft); }
.info-row:last-child { border-bottom: none; }
.info-row span { color: var(--text-secondary); }

.consult-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.consult-card {
  display: flex; align-items: center; gap: 12px; padding: 14px; border-radius: 16px; text-align: left;
  border: 1px solid var(--border-soft); background: linear-gradient(180deg, #fff, #fbf9ff);
  cursor: pointer; transition: transform .2s ease, border-color .2s ease, box-shadow .2s ease;
}
.consult-card:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(20,33,61,.08); }
.consult-card.is-active { border-color: var(--brand-500, #7c3aed); background: #faf7ff; box-shadow: 0 0 0 1px var(--brand-500, #7c3aed); }
.consult-card__avatar { width: 42px; height: 42px; border-radius: 50%; flex-shrink: 0; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #a78bfa, #8b5cf6); color: #fff; font-weight: 700; }
.consult-card__body { flex: 1; min-width: 0; }
.consult-card__body strong { font-size: 14px; }
.consult-card__body p { margin: 2px 0; color: var(--text-secondary); font-size: 12px; }
.consult-card__expertise { color: var(--text-tertiary); font-size: 11px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; }
.consult-card__rating { color: #d97706; font-size: 13px; font-weight: 700; flex-shrink: 0; }

.slot-panel { display: grid; gap: 12px; padding: 16px; border-radius: 16px; background: #f8f6fc; border: 1px solid var(--border-soft); }
.slot-panel__head { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.slot-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.slot-chip {
  padding: 8px 14px; border-radius: 999px; font-size: 13px; cursor: pointer;
  border: 1px solid #ddd3ef; background: #fff; color: #43395e;
  transition: all .18s ease;
}
.slot-chip:hover { border-color: #a78bfa; color: #6d28d9; }
.slot-chip.is-active { background: linear-gradient(135deg, #8b5cf6, #6d28d9); color: #fff; border-color: transparent; }
.slot-submit { justify-self: start; margin-top: 4px; }

@media (max-width: 700px) { .consult-grid { grid-template-columns: 1fr; } }
</style>

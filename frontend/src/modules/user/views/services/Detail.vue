<template>
  <div class="page-shell">
    <section class="page-hero detail-page-hero">
      <button
        class="back-btn"
        @click="router.back()"
      >
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </button>
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

        <!-- 模式一：教师咨询 —— 选咨询师 + 选时段（已实现） -->
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

        <!-- 模式二：设备借用 —— 选设备 + 数量 + 日期/起止时段（到点自动归还） -->
        <template v-else-if="equipmentMode">
          <el-divider content-position="left">
            选择设备与借用时段
          </el-divider>

          <div class="consult-grid">
            <button
              v-for="d in equipment"
              :key="d.id"
              class="consult-card"
              :class="{ 'is-active': selectedEquipment?.id === d.id }"
              :disabled="(d.availableStock || 0) <= 0"
              @click="selectEquipment(d)"
            >
              <div class="consult-card__avatar equipment">
                {{ d.name.slice(0, 1) }}
              </div>
              <div class="consult-card__body">
                <strong>{{ d.name }}</strong>
                <p>{{ d.location || '设备管理中心' }} · {{ d.unit || '' }}</p>
                <span class="consult-card__expertise">
                  {{ d.description || d.category || '校园公共设备' }}
                </span>
              </div>
              <span
                class="equip-stock"
                :class="{ 'is-empty': (d.availableStock || 0) <= 0 }"
              >
                {{ (d.availableStock || 0) <= 0 ? '已借空' : '可借 ' + (d.availableStock || 0) + ' ' + (d.unit || '台') }}
              </span>
            </button>
          </div>

          <div
            v-if="selectedEquipment"
            class="slot-panel borrow-panel"
          >
            <div class="borrow-grid">
              <label>
                <span>数量</span>
                <el-input-number
                  v-model="borrowQty"
                  :min="1"
                  :max="Math.max(1, selectedEquipment.availableStock || 1)"
                  size="large"
                />
                <em>最多 {{ selectedEquipment.availableStock }} {{ selectedEquipment.unit || '台' }}</em>
              </label>
              <label>
                <span>借用日期</span>
                <el-date-picker
                  v-model="borrowDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  :disabled-date="disablePastDate"
                  placeholder="选择日期"
                  size="large"
                />
              </label>
              <label>
                <span>开始时间</span>
                <el-select
                  v-model="borrowStart"
                  size="large"
                >
                  <el-option
                    v-for="h in hourOptions"
                    :key="h"
                    :label="h"
                    :value="h"
                  />
                </el-select>
              </label>
              <label>
                <span>结束时间</span>
                <el-select
                  v-model="borrowEnd"
                  size="large"
                >
                  <el-option
                    v-for="h in hourOptions"
                    :key="h"
                    :label="h"
                    :value="h"
                  />
                </el-select>
              </label>
            </div>
            <el-button
              type="primary"
              size="large"
              class="slot-submit"
              :loading="submitting"
              @click="submitBorrow"
            >
              提交借用申请
            </el-button>
            <p class="borrow-tip">
              借用按固定时段计；审核通过后即占用该时段库存，到点自动归还，无需手动操作。
            </p>
          </div>
        </template>

        <!-- 模式三：普通服务 —— 通用预约 -->
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
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, ArrowLeft } from '@element-plus/icons-vue'
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
interface EquipmentLite {
  id: number
  name: string
  category?: string
  description?: string
  stock?: number
  availableStock?: number
  unit?: string
  location?: string
}

const router = useRouter()
const route = useRoute()
const service = ref<ServiceCard | null>(null)
const booking = ref(false)

// 教师咨询态
const consultants = ref<ConsultantLite[]>([])
const selected = ref<ConsultantLite | null>(null)
const slots = ref<SlotLite[]>([])
const bookingSlotId = ref<number | null>(null)
const loadingSlots = ref(false)

// 设备借用态
const equipmentMode = computed(() => Boolean(service.value && service.value.catKey === 'equipment'))
const equipment = ref<EquipmentLite[]>([])
const selectedEquipment = ref<EquipmentLite | null>(null)
const borrowQty = ref(1)
const borrowDate = ref(localDate())
const borrowStart = ref('09:00')
const borrowEnd = ref('18:00')
const hourOptions = Array.from({ length: 14 }, (_, i) => `${String(8 + i).padStart(2, '0')}:00`)

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
    if (service.value.catKey === 'equipment') {
      await loadEquipment(service.value.id)
    } else {
      await loadConsultants(service.value.id)
    }
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '获取服务详情失败')
  }
})

/** 该服务是否配置了咨询师：有则走「选咨询师+时段」 */
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

/** 设备借用：读取该服务下的真库设备目录 */
async function loadEquipment(serviceId: number) {
  try {
    const res = await request.get('/app/equipment', {
      params: { serviceId, pageNo: 1, pageSize: 100 },
    }) as { records?: EquipmentLite[] } | EquipmentLite[]
    const list = Array.isArray(res) ? res : res?.records || []
    equipment.value = (list || []).map(d => ({
      id: d.id,
      name: d.name || '设备',
      category: d.category || '',
      description: d.description || '',
      stock: d.stock,
      availableStock: d.availableStock ?? 0,
      unit: d.unit || '台',
      location: d.location || '',
    }))
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '获取设备目录失败')
    equipment.value = []
  }
}

/** 本地时区 yyyy-MM-dd（避免 toISOString 的 UTC 跨天问题） */
function localDate(d = new Date()) {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

function selectEquipment(d: EquipmentLite) {
  selectedEquipment.value = d
  borrowQty.value = 1
}

function disablePastDate(date: Date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

async function submitBorrow() {
  if (!selectedEquipment.value || !service.value) return
  if (borrowStart.value >= borrowEnd.value) {
    ElMessage.warning('结束时间需晚于开始时间')
    return
  }
  submitting.value = true
  try {
    await request.post(`/app/equipment/${selectedEquipment.value.id}/book`, {
      quantity: borrowQty.value,
      date: borrowDate.value,
      startTime: borrowStart.value,
      endTime: borrowEnd.value,
    })
    ElMessage.success('借用申请已提交，等待管理员审核')
    router.push('/bookings')
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '借用申请失败，请重试')
  } finally {
    submitting.value = false
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
    const today = localDate()
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
.detail-page-hero {
  display: flex;
  align-items: center;
  gap: 26px;
  padding: 26px 36px;
}
.detail-page-hero .back-btn { margin-right: 6px; }
/* 让返回按钮/标题浮在 hero 装饰光效之上，避免看起来“叠字” */
.detail-page-hero > * { position: relative; z-index: 1; }
.detail-page-hero .back-btn { flex: 0 0 auto; }
.detail-page-hero .page-hero__title { margin: 0; font-size: 26px; line-height: 1.2; }
.service-detail { display: grid; gap: 18px; max-width: 760px; }
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
.consult-card.is-active { border-color: #7c3aed; background: #faf7ff; box-shadow: 0 0 0 1px #7c3aed; }
.consult-card:disabled { opacity: .55; cursor: not-allowed; }
.consult-card__avatar { width: 42px; height: 42px; border-radius: 50%; flex-shrink: 0; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #a78bfa, #8b5cf6); color: #fff; font-weight: 700; }
.consult-card__avatar.equipment { border-radius: 12px; background: linear-gradient(135deg, #5eead4, #0d9488); }
.consult-card__body { flex: 1; min-width: 0; }
.consult-card__body strong { font-size: 14px; }
.consult-card__body p { margin: 2px 0; color: var(--text-secondary); font-size: 12px; }
.consult-card__expertise { color: var(--text-tertiary); font-size: 11px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; }
.consult-card__rating { color: #d97706; font-size: 13px; font-weight: 700; flex-shrink: 0; }
.equip-stock { font-size: 12px; font-weight: 600; color: #0d9488; flex-shrink: 0; }
.equip-stock.is-empty { color: #dc2626; }

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

.borrow-panel { gap: 14px; }
.borrow-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }
.borrow-grid label { display: grid; gap: 6px; font-size: 13px; color: var(--text-secondary); }
.borrow-grid em { font-style: normal; font-size: 12px; color: var(--text-tertiary); }
.borrow-tip { margin: 0; font-size: 12px; color: var(--text-tertiary); }

@media (max-width: 700px) { .consult-grid, .borrow-grid { grid-template-columns: 1fr; } }
</style>

import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/common/utils/request'
import { fetchServiceCardById } from '@/common/campus'
import type { ServiceCard } from '@/common/types'
import { openChatWithConsultant } from '@/common/consultChat'

export interface ConsultantLite {
  id: number
  name: string
  title: string
  department: string
  expertise?: string[]
  rating?: number
  reviews?: number
}
export interface SlotLite {
  slotId: number
  startTime: string
  endTime: string
}
export interface EquipmentLite {
  id: number
  name: string
  category?: string
  description?: string
  stock?: number
  availableStock?: number
  unit?: string
  location?: string
}
export interface RoomLite {
  id: number
  name: string
  location?: string
  seats?: number
}

/** /uploads/xx → /api/uploads/xx（走 vite 代理到网关） */
export function assetUrl(p?: string) {
  if (!p) return ''
  if (/^https?:/.test(p)) return p
  if (p.startsWith('/uploads')) return `/api${p}`
  return p
}

/** 本地时区 yyyy-MM-dd（避免 toISOString 的 UTC 跨天问题） */
export function localDate(d = new Date()) {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

export function disablePastDate(date: Date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

export const hourOptions = Array.from({ length: 14 }, (_, i) => `${String(8 + i).padStart(2, '0')}:00`)

export function useServiceDetail() {
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
  // 教室预约态
  const roomMode = computed(() => Boolean(service.value && service.value.catKey === 'space'))
  const rooms = ref<RoomLite[]>([])
  const selectedRoom = ref<RoomLite | null>(null)
  const borrowDate = ref(localDate())
  const borrowStart = ref('09:00')
  const borrowEnd = ref('18:00')

  const submitting = ref(false)
  const chatBusy = ref(false)

  onMounted(async () => {
    const id = Number(route.params.id)
    try {
      // 7.3.11：改走单条详情接口，不再拉整页服务列表后前端 find
      service.value = await fetchServiceCardById(id)
      if (!service.value) {
        ElMessage.warning('未找到该服务')
        return
      }
      if (service.value.catKey === 'equipment') {
        await loadEquipment(service.value.id)
      } else if (service.value.catKey === 'space') {
        await loadRooms(service.value.id)
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

  function selectEquipment(d: EquipmentLite) {
    selectedEquipment.value = d
    borrowQty.value = 1
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

  /** 教室目录：读取该服务（空闲教室）下的真库教室 */
  async function loadRooms(serviceId: number) {
    try {
      const res = await request.get('/app/rooms', { params: { serviceId } }) as RoomLite[] | unknown
      rooms.value = (Array.isArray(res) ? res : []) as RoomLite[]
    } catch (error: unknown) {
      const err = error as { message?: string }
      ElMessage.error(err.message || '获取教室列表失败')
      rooms.value = []
    }
  }

  function selectRoom(r: RoomLite) {
    selectedRoom.value = r
  }

  async function submitRoom() {
    if (!selectedRoom.value) return
    if (borrowStart.value >= borrowEnd.value) {
      ElMessage.warning('结束时间需晚于开始时间')
      return
    }
    submitting.value = true
    try {
      await request.post(`/app/rooms/${selectedRoom.value.id}/book`, {
        date: borrowDate.value,
        startTime: borrowStart.value,
        endTime: borrowEnd.value,
      })
      ElMessage.success('教室预约已提交，等待管理员审核')
      router.push('/bookings')
    } catch (error: unknown) {
      const err = error as { message?: string }
      ElMessage.error(err.message || '教室预约失败，请重试')
    } finally {
      submitting.value = false
    }
  }

  async function startConsultChat(c: ConsultantLite) {
    if (chatBusy.value) return
    chatBusy.value = true
    try {
      const conv = await openChatWithConsultant(c.id)
      router.push({ path: `/chat/${conv.id}`, query: { name: conv.peerName } })
    } catch (error: unknown) {
      const err = error as { isAxiosError?: boolean; message?: string }
      if (!err?.isAxiosError) ElMessage.error(err?.message || '发起沟通失败，请稍后重试')
    } finally {
      chatBusy.value = false
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

  return {
    service,
    booking,
    consultants,
    selected,
    slots,
    bookingSlotId,
    loadingSlots,
    equipmentMode,
    equipment,
    selectedEquipment,
    borrowQty,
    roomMode,
    rooms,
    selectedRoom,
    borrowDate,
    borrowStart,
    borrowEnd,
    submitting,
    selectConsultant,
    startConsultChat,
    submitConsultation,
    selectEquipment,
    submitBorrow,
    selectRoom,
    submitRoom,
    handleBook,
  }
}

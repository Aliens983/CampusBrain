import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchBookingRecords, fetchServiceCards } from '@/common/campus'
import request from '@/common/utils/request'
import type { BookingRecord, DashboardStat, ServiceCard } from '@/common/types'

/** 快捷入口项 */
export interface DashboardShortcut {
  title: string
  desc: string
  path: string
  query: Record<string, string>
  icon: string
  tone: string
}

/** 待处理反馈项 */
export interface DashboardTodo {
  title: string
  desc: string
  badge: string
  tone: string
  path: string
}

/** /uploads/xx → /api/uploads/xx（走 vite 代理到网关） */
export function assetUrl(p?: string) {
  if (!p) return ''
  if (/^https?:/.test(p)) return p
  if (p.startsWith('/uploads')) return `/api${p}`
  return p
}

/** /uploads/xx → /api/uploads/xx（走 vite 代理到网关） */
function bannerUrl(p: string) {
  return assetUrl(p)
}

export function useDashboard() {
  const metricDialogVisible = ref(false)
  const metricDialogTitle = ref('')
  const metricDialogItems = ref<string[]>([])
  const activeService = ref<ServiceCard | null>(null)
  const bookings = ref<BookingRecord[]>([])
  const services = ref<ServiceCard[]>([])
  const banners = ref<string[]>([])
  const loading = ref(false)

  async function loadBanners() {
    try {
      const list = await request.get('/app/carousel') as string[] | unknown
      banners.value = (Array.isArray(list) ? list : []).map(bannerUrl)
    } catch {
      banners.value = []
    }
  }

  onMounted(async () => {
    void loadBanners()
    loading.value = true
    loading.value = true
    try {
      const [bookingData, serviceData] = await Promise.all([fetchBookingRecords(), fetchServiceCards()])
      bookings.value = bookingData
      services.value = serviceData
    } catch (error: unknown) {
      const err = error as { message?: string }
      ElMessage.error(err.message || '获取数据失败')
    } finally {
      loading.value = false
    }
  })

  const todayBookings = computed(() => {
    const today = new Date().toISOString().slice(0, 10)
    return bookings.value.filter((b: BookingRecord) => b.date === today).slice(0, 3)
  })

  const recentBookings = computed(() => bookings.value.slice(0, 5))

  const dashboardStats = computed<DashboardStat[]>(() => {
    const total = bookings.value.length
    const pending = bookings.value.filter((b: BookingRecord) => b.status === 'pending').length
    const completed = bookings.value.filter((b: BookingRecord) => b.status === 'completed').length

    return [
      { label: '本月预约量', value: String(total), trend: total > 0 ? '正增长' : '暂无数据', tone: total > 0 ? 'success' : 'warning' },
      { label: '我的申请', value: String(pending), trend: pending > 0 ? '处理中' : '已处理完', tone: pending > 0 ? 'warning' : 'success' },
      { label: '已完成', value: String(completed), trend: completed > 0 ? '已完成' : '暂无', tone: 'brand' },
      { label: '资源完单率', value: total > 0 ? `${Math.round((completed / total) * 100)}%` : '0%', trend: '本月表现', tone: 'brand' },
    ]
  })

  // 按资源类别组织的预约入口（指向真实分类，配合服务中心分类 Tab）
  const shortcuts: DashboardShortcut[] = [
    { title: '教师咨询', desc: '心理咨询 / 学业辅导', path: '/services', query: { category: 'teacher' }, icon: '🧑‍🏫', tone: 'tone-blue' },
    { title: '设备借用', desc: '按时间段借用设备', path: '/services', query: { category: 'equipment' }, icon: '🖨️', tone: 'tone-teal' },
    { title: '教室空间', desc: '选教室 · 自选时段', path: '/services', query: { category: 'space' }, icon: '🏫', tone: 'tone-amber' },
    { title: '活动报名', desc: '校园活动报名', path: '/services', query: { category: 'activity' }, icon: '📣', tone: 'tone-slate' },
  ]

  const todoList = computed<DashboardTodo[]>(() => {
    const pendingBookings = bookings.value.filter((b: BookingRecord) => b.status === 'pending')
    return [
      { title: '我的申请', desc: `${pendingBookings.length} 条申请处理中（教室/设备等需老师或管理员确认）。`, badge: pendingBookings.length > 0 ? '处理中' : '已处理完', tone: pendingBookings.length > 0 ? 'is-warning' : 'is-success', path: '/bookings' },
    ]
  })

  const serviceDrawerVisible = computed({
    get: () => Boolean(activeService.value),
    set: (value: boolean) => {
      if (!value) activeService.value = null
    },
  })

  function openMetricDetail(label: string) {
    const mapping: Record<string, string[]> = {
      本月预约量: [`总预约 ${bookings.value.length} 单`, `已完成 ${bookings.value.filter(b => b.status === 'completed').length} 单`, `进行中 ${bookings.value.filter(b => b.status === 'approved').length} 单`],
      我的申请: [`${bookings.value.filter(b => b.status === 'pending').length} 条处理中`, '需咨询师或管理员确认，请留意状态变化'],
      已完成: [`本月完成 ${bookings.value.filter(b => b.status === 'completed').length} 单`],
      资源完单率: ['根据实际预约完成情况统计', '持续优化使用体验'],
    }
    metricDialogTitle.value = label
    metricDialogItems.value = mapping[label] || ['暂无明细']
    metricDialogVisible.value = true
  }

  return {
    banners,
    bookings,
    services,
    activeService,
    todayBookings,
    recentBookings,
    dashboardStats,
    shortcuts,
    todoList,
    serviceDrawerVisible,
    metricDialogVisible,
    metricDialogTitle,
    metricDialogItems,
    openMetricDetail,
  }
}

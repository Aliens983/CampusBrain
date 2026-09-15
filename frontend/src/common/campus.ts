import request from '@/common/utils/request'
import type { AdminSummary, BookingRecord, ServiceCard, UserInfo } from '@/common/types'
import { normalizeRole, normalizeUserInfo } from '@/common/utils/auth'
// 分类中文名全局唯一来源在 common/dictionary（7.3.11），此处不再私有维护一份
import { categoryLabel } from '@/common/dictionary'

type BackendService = {
  serviceId?: number
  serviceName?: string
  serviceDescribe?: string
  serviceState?: number
  categoryId?: number
  categoryCode?: string
  categoryName?: string
  /** 兼容旧契约/兜底：老后端可能仍返回 code 串 */
  category?: string
  capacity?: number
  campus?: string
  imageUrl?: string
}

type BackendBooking = {
  orderId?: number
  serviceName?: string
  serviceDescribe?: string
  username?: string
  createTime?: string
  updateTime?: string
  manageStatus?: number
  statusDescription?: string
  // 咨询时段/设备借用/教室预约 回显
  consultantName?: string
  equipmentName?: string
  quantity?: number
  roomName?: string
  campus?: string
  slotDate?: string
  startTime?: string
  endTime?: string
}

type BackendUser = {
  id?: number
  name?: string
  email?: string
  role?: string | number
  grade?: string
}

function mapAdminUser(item: BackendUser) {
  return {
    id: item.id || 0,
    username: item.name || '未知用户',
    email: item.email || '',
    phone: '',
    role: normalizeRole(item.role),
    department: item.grade || '未分配部门',
    createdAt: '',
  }
}

const serviceGradients = ['gradient-brand', 'gradient-teal', 'gradient-amber', 'gradient-slate'] as const

function getServiceType(name: string): ServiceCard['type'] {
  if (name.includes('会议') || name.includes('教室') || name.includes('场地')) return 'room'
  if (name.includes('设备') || name.includes('投影') || name.includes('相机')) return 'equipment'
  if (name.includes('咨询')) return 'consultation'
  if (name.includes('活动')) return 'activity'
  return 'printing'
}

const categoryToType: Record<string, ServiceCard['type']> = {
  teacher: 'consultation',
  equipment: 'equipment',
  space: 'room',
  activity: 'activity',
  exam: 'printing',
  other: 'printing',
}

/** 后端没返回分类编码时的兜底归类（老数据按名称推断） */
function resolveCategory(serviceName: string, raw: string | undefined, type: ServiceCard['type']): string {
  if (raw && categoryToType[raw]) return raw
  if (serviceName.includes('考试')) return 'exam'
  if (serviceName.includes('活动')) return 'activity'
  if (type === 'room') return 'space'
  if (type === 'consultation') return 'teacher'
  if (type === 'equipment') return 'equipment'
  return 'other'
}

function mapService(item: BackendService, index: number): ServiceCard {
  const serviceName = item.serviceName || `服务 ${index + 1}`
  // 优先用后端 service_category 字典返回的分类编码/中文名；老数据无则按名称兜底
  const code = item.categoryCode || resolveCategory(serviceName, item.category, getServiceType(serviceName))
  const type = code && categoryToType[code] ? categoryToType[code] : getServiceType(serviceName)
  const catKey = code

  return {
    id: Number(item.serviceId || index + 1),
    code: `CAS-${String(item.serviceId || index + 1).padStart(3, '0')}`,
    name: serviceName,
    description: item.serviceDescribe || '暂无服务说明，后续可由后台补充完整描述。',
    type,
    catKey,
    categoryId: item.categoryId,
    campus: item.campus,
    imageUrl: item.imageUrl,
    capacity: item.capacity,
    category: item.categoryName || categoryLabel(catKey) || '其他服务',
    location: item.campus === 'xs' ? '下沙校区' : '仓前校区',
    priceLabel: item.serviceState === 1 ? '可预约' : '维护中',
    status: item.serviceState === 1 ? 'available' : 'maintenance',
    tags: [type === 'room' ? '场地预约' : type === 'equipment' ? '设备借用' : type === 'consultation' ? '咨询排班' : '综合办理'],
    image: serviceGradients[index % serviceGradients.length],
  }
}

function mapBooking(item: BackendBooking, index: number): BookingRecord {
  const status =
    item.manageStatus === 1 ? 'approved' : item.manageStatus === 2 ? 'rejected' : item.manageStatus === 3 ? 'cancelled' : item.manageStatus === 4 ? 'completed' : 'pending'
  const dateText = item.createTime ? String(item.createTime).replace('T', ' ') : ''
  // 咨询时段 / 设备借用：日期时段以用户选定为准（否则按提交时间回显）
  const hasWindow = Boolean(item.consultantName || item.equipmentName || item.roomName)
  const date = hasWindow ? String(item.slotDate || '').slice(0, 10) || '待定' : dateText.slice(0, 10) || '待定'
  const timeRange = hasWindow
    ? [item.startTime, item.endTime].filter(Boolean).join(' - ') || '待分配时段'
    : item.updateTime ? `${String(item.createTime || '').slice(11, 16)} - ${String(item.updateTime).slice(11, 16)}` : '待分配时段'

  return {
    id: Number(item.orderId || index + 1),
    bookingNo: `BOOK-${String(item.orderId || index + 1).padStart(6, '0')}`,
    serviceName: item.serviceName || '未命名服务',
    consultantName: item.consultantName,
    equipmentName: item.equipmentName,
    quantity: item.quantity,
    roomName: item.roomName,
    campus: item.campus,
    type: getServiceType(item.serviceName || ''),
    applicant: item.username || '未知用户',
    department: '未分配部门',
    location: item.serviceDescribe || (item.campus === 'xs' ? '下沙校区' : '仓前校区'),
    date,
    timeRange,
    status,
    createdAt: dateText || '待定',
    remarks: item.statusDescription || item.serviceDescribe || '',
  }
}

function mapRole(role: string | number | undefined) {
  return normalizeRole(role)
}

export async function fetchUserProfile() {
  const data = (await request.get('/users/me')) as BackendUser
  return normalizeUserInfo({
    ...data,
    username: data?.name,
    name: data?.name,
    role: mapRole(data?.role),
  })
}

export async function fetchServiceCards() {
  const data = (await request.get('/app/services')) as { records?: BackendService[] } | BackendService[]
  // 后端返回 PageResult 分页结构，数据在 records 字段中
  const list = Array.isArray(data) ? data : data?.records
  if (!Array.isArray(list)) {
    throw new Error('获取服务列表失败')
  }
  return list.map(mapService)
}

/**
 * 服务详情：直接走 GET /app/services/{id} 单条接口（7.3.11）。
 * 此前详情页只能拉整页列表再前端 find，数据量大时既慢又受分页口径影响。
 */
export async function fetchServiceCardById(id: number): Promise<ServiceCard | null> {
  const data = (await request.get(`/app/services/${id}`)) as BackendService | null
  return data ? mapService(data, 0) : null
}

/**
 * 服务业务分类接口与类型定义已收敛到 common/dictionary（7.3.11），
 * 此处保留转出以兼容既有 import 路径。
 */
export { fetchServiceCategories, type ServiceCategoryOption } from '@/common/dictionary'

/** 2.2.3：管理端服务列表走 /admin/services 服务端分页（含维护中的服务），只取当前页 */
export interface AdminServicesPage {
  records: ServiceCard[]
  total: number
}

export async function fetchAdminServicesPage(params: {
  pageNo: number
  pageSize: number
  serviceName?: string
  serviceState?: number
  campus?: string
}): Promise<AdminServicesPage> {
  const query: Record<string, string | number> = {
    pageNo: params.pageNo,
    pageSize: params.pageSize,
  }
  const serviceName = params.serviceName?.trim()
  if (serviceName) query.serviceName = serviceName
  if (params.serviceState !== undefined) query.serviceState = params.serviceState
  if (params.campus) query.campus = params.campus
  const data = (await request.get('/admin/services', { params: query })) as
    | { records?: BackendService[]; total?: number }
    | BackendService[]
  if (Array.isArray(data)) {
    return { records: data.map(mapService), total: data.length }
  }
  // 服务端分页后，index 仅在当前页内用于渐变占位，不影响 id
  return {
    records: (data.records || []).map((item, index) => mapService(item, index)),
    total: Number(data.total || 0),
  }
}

export async function fetchBookingRecords() {
  const response = (await request.get('/users/me/bookings')) as { bookings?: BackendBooking[]; serviceStatusList?: BackendBooking[] }
  // 后端 UserInfoAndServicesViaMPRespVO 的字段名是 bookings
  const list = response?.bookings || response?.serviceStatusList
  if (!Array.isArray(list)) {
    throw new Error('获取预约记录失败')
  }
  return list.map(mapBooking)
}

/** 2.2.3：用户列表改为服务端分页，只拉当前页 */
export interface AdminUsersPage {
  records: UserInfo[]
  total: number
}

export async function fetchAdminUsersPage(params: { pageNo: number; pageSize: number; name?: string }): Promise<AdminUsersPage> {
  const query: Record<string, string | number> = {
    pageNo: params.pageNo,
    pageSize: params.pageSize,
  }
  const name = params.name?.trim()
  if (name) query.name = name
  const data = (await request.get('/users/list', { params: query })) as
    | { records?: BackendUser[]; total?: number }
    | BackendUser[]
  if (Array.isArray(data)) {
    // 兼容后端直接返回数组（无 total）的情况
    return { records: data.map(mapAdminUser), total: data.length }
  }
  return {
    records: (data.records || []).map(mapAdminUser),
    total: Number(data.total || 0),
  }
}

/**
 * 管理端首页统计用：用户总数与管理员数（含超级管理员）。
 * 仅取各筛选条件下的 total（pageSize=1），不再翻页拉全量用户。
 */
export async function fetchAdminUserCounts(): Promise<{ total: number; admin: number }> {
  const fetchTotal = (role?: number) =>
    request.get('/users/list', {
      params: role === undefined ? { pageNo: 1, pageSize: 1 } : { pageNo: 1, pageSize: 1, role },
    }) as Promise<{ total?: number } | BackendUser[]>

  const [all, admins, supers] = await Promise.all([fetchTotal(), fetchTotal(1), fetchTotal(2)])
  const totalOf = (res: { total?: number } | BackendUser[]) => (Array.isArray(res) ? res.length : Number(res.total || 0))
  return {
    total: totalOf(all),
    admin: totalOf(admins) + totalOf(supers),
  }
}

/**
 * 管理端预约状态计数（仅取各筛选条件下的 total，页大小 1）。
 * 2.2.3：不再用 pageSize=1000 拉全量（还会触发后端每页最多 200 的校验失败）。
 * 后端 GET /admin/bookings，manageStatus: 0待审/1通过/2拒绝/3取消/4完成。
 */
async function fetchAdminBookingCounts(): Promise<{ total: number; pending: number; approved: number }> {
  const fetchTotal = (manageStatus?: number) =>
    request.get('/admin/bookings', {
      params:
        manageStatus === undefined
          ? { pageNo: 1, pageSize: 1 }
          : { pageNo: 1, pageSize: 1, manageStatus },
    }) as Promise<{ total?: number } | Array<unknown>>

  const [all, pending, approved] = await Promise.all([fetchTotal(), fetchTotal(0), fetchTotal(1)])
  const totalOf = (res: { total?: number } | Array<unknown>) => (Array.isArray(res) ? res.length : Number(res.total || 0))
  return { total: totalOf(all), pending: totalOf(pending), approved: totalOf(approved) }
}

export async function fetchAdminSummary(): Promise<AdminSummary> {
  const fetchServiceTotal = request.get('/admin/services', { params: { pageNo: 1, pageSize: 1 } }) as Promise<
    { total?: number } | Array<unknown>
  >
  const [userCounts, servicePage, bookingCounts] = await Promise.all([
    fetchAdminUserCounts(),
    fetchServiceTotal,
    fetchAdminBookingCounts(),
  ])
  const totalServices = Array.isArray(servicePage) ? servicePage.length : Number(servicePage.total || 0)
  const { total, pending, approved } = bookingCounts

  return {
    totalUsers: userCounts.total,
    totalServices,
    activeBookings: pending + approved,
    approvalRate: total ? `${Math.round((approved / total) * 1000) / 10}%` : '—',
    pendingBookings: pending,
  }
}

export { mapService, mapBooking, mapAdminUser }
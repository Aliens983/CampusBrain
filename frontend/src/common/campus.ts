import request from '@/common/utils/request'
import type { AdminSummary, BookingRecord, ServiceCard } from '@/common/types'
import { normalizeRole, normalizeUserInfo } from '@/common/utils/auth'

type BackendService = {
  serviceId?: number
  serviceName?: string
  serviceDescribe?: string
  serviceState?: number
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
  return 'printing'
}

function mapService(item: BackendService, index: number): ServiceCard {
  const serviceName = item.serviceName || `服务 ${index + 1}`
  const type = getServiceType(serviceName)

  return {
    id: Number(item.serviceId || index + 1),
    code: `CAS-${String(item.serviceId || index + 1).padStart(3, '0')}`,
    name: serviceName,
    description: item.serviceDescribe || '暂无服务说明，后续可由后台补充完整描述。',
    type,
    category:
      type === 'room' ? '空间资源' : type === 'equipment' ? '设备资源' : type === 'consultation' ? '咨询服务' : '综合服务',
    location: '校园统一预约中心',
    priceLabel: item.serviceState === 1 ? '当前可申请' : '暂不可申请',
    status: item.serviceState === 1 ? 'available' : 'maintenance',
    tags: [type === 'room' ? '场地预约' : type === 'equipment' ? '设备借用' : type === 'consultation' ? '咨询排班' : '综合办理'],
    image: serviceGradients[index % serviceGradients.length],
  }
}

function mapBooking(item: BackendBooking, index: number): BookingRecord {
  const status =
    item.manageStatus === 1 ? 'approved' : item.manageStatus === 2 ? 'rejected' : item.manageStatus === 3 ? 'completed' : item.manageStatus === 4 ? 'cancelled' : 'pending'
  const dateText = item.createTime ? String(item.createTime).replace('T', ' ') : ''

  return {
    id: Number(item.orderId || index + 1),
    bookingNo: `BOOK-${String(item.orderId || index + 1).padStart(6, '0')}`,
    serviceName: item.serviceName || '未命名服务',
    type: getServiceType(item.serviceName || ''),
    applicant: item.username || '未知用户',
    department: '未分配部门',
    location: item.serviceDescribe || '校园统一预约中心',
    date: dateText.slice(0, 10) || '待定',
    timeRange: item.updateTime ? `${String(item.createTime || '').slice(11, 16)} - ${String(item.updateTime).slice(11, 16)}` : '待分配时段',
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

export async function fetchBookingRecords() {
  const response = (await request.get('/users/me/bookings')) as { bookings?: BackendBooking[]; serviceStatusList?: BackendBooking[] }
  // 后端 UserInfoAndServicesViaMPRespVO 的字段名是 bookings
  const list = response?.bookings || response?.serviceStatusList
  if (!Array.isArray(list)) {
    throw new Error('获取预约记录失败')
  }
  return list.map(mapBooking)
}

export async function fetchAdminUsers() {
  const data = (await request.get('/users/list')) as { records?: BackendUser[] } | BackendUser[]
  // 后端返回 PageResult 分页结构，数据在 records 字段中
  const list = Array.isArray(data) ? data : data?.records
  if (!Array.isArray(list)) {
    throw new Error('获取用户列表失败')
  }
  return list.map(mapAdminUser)
}

export async function fetchAdminSummary(): Promise<AdminSummary> {
  const [users, services, bookings] = await Promise.all([fetchAdminUsers(), fetchServiceCards(), fetchBookingRecords()])
  const approvedCount = bookings.filter((item) => item.status === 'approved').length
  const total = bookings.length || 1

  return {
    totalUsers: users.length,
    totalServices: services.length,
    activeBookings: bookings.filter((item) => item.status === 'pending' || item.status === 'approved').length,
    approvalRate: `${Math.round((approvedCount / total) * 1000) / 10}%`,
  }
}

export { mapService, mapBooking, mapAdminUser }
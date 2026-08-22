import request from '@/common/utils/request'
import type { AdminSummary, BookingRecord, ServiceCard, UserInfo } from '@/types'
import { normalizeRole, normalizeUserInfo } from '@/utils/auth'

type ApiService = {
  serviceId?: number
  serviceName?: string
  serviceDescribe?: string
  serviceState?: number
}

type ApiBooking = {
  orderId?: number
  serviceName?: string
  serviceDescribe?: string
  username?: string
  createTime?: string
  updateTime?: string
  manageStatus?: number
  statusDescription?: string
}

type ApiUser = {
  name?: string
  email?: string
  role?: string | number
}

const gradients = ['gradient-brand', 'gradient-teal', 'gradient-amber', 'gradient-slate'] as const

function getServiceType(name: string): ServiceCard['type'] {
  if (name.includes('会议') || name.includes('教室') || name.includes('场地')) return 'room'
  if (name.includes('设备') || name.includes('投影') || name.includes('相机')) return 'equipment'
  if (name.includes('咨询')) return 'consultation'
  return 'printing'
}

function mapUserRole(role: string | number | undefined): UserInfo['role'] {
  if (role === 2 || role === '2') return 'super_admin'
  if (role === 1 || role === '1') return 'admin'
  return normalizeRole(role)
}

function mapService(item: ApiService, index: number): ServiceCard {
  const serviceName = item.serviceName || `服务 ${index + 1}`
  const type = getServiceType(serviceName)

  return {
    id: Number(item.serviceId || index + 1),
    code: `CAS-${String(item.serviceId || index + 1).padStart(3, '0')}`,
    name: serviceName,
    description: item.serviceDescribe || '暂无服务说明',
    type,
    category:
      type === 'room' ? '空间资源' : type === 'equipment' ? '设备资源' : type === 'consultation' ? '咨询服务' : '综合服务',
    location: '校园统一预约中心',
    priceLabel: item.serviceState === 1 ? '当前可申请' : '暂不可申请',
    status: item.serviceState === 1 ? 'available' : 'maintenance',
    tags: [type === 'room' ? '场地预约' : type === 'equipment' ? '设备借用' : type === 'consultation' ? '咨询排班' : '综合办理', '校园服务'],
    image: gradients[index % gradients.length],
  }
}

function mapBooking(item: ApiBooking, index: number): BookingRecord {
  const status =
    item.manageStatus === 1
      ? 'approved'
      : item.manageStatus === 2
        ? 'rejected'
        : item.manageStatus === 3
          ? 'cancelled'
          : 'pending'
  const dateTime = String(item.createTime || '').replace('T', ' ')

  return {
    id: Number(item.orderId || index + 1),
    bookingNo: `BOOK-${String(item.orderId || index + 1).padStart(6, '0')}`,
    serviceName: item.serviceName || '未命名服务',
    type: getServiceType(item.serviceName || ''),
    applicant: item.username || '未知用户',
    department: '未分配部门',
    location: item.serviceDescribe || '校园统一预约中心',
    date: dateTime.slice(0, 10) || '待定',
    timeRange: item.updateTime ? `${String(item.createTime || '').slice(11, 16)} - ${String(item.updateTime).slice(11, 16)}` : '待分配时段',
    status,
    createdAt: dateTime || '待定',
    remarks: item.statusDescription || item.serviceDescribe || '',
  }
}

export async function loadUserProfile() {
  const data = (await request.get('/users/me')) as ApiUser
  return normalizeUserInfo({
    ...data,
    username: data?.name,
    name: data?.name,
    role: mapUserRole(data?.role),
  })
}

export async function loadServiceCards() {
  const data = (await request.get('/app/services')) as { records?: ApiService[] } | ApiService[]
  // 后端返回 PageResult 分页结构，数据在 records 字段中
  const list = Array.isArray(data) ? data : data?.records
  if (!Array.isArray(list)) {
    throw new Error('获取服务列表失败')
  }
  return list.map(mapService)
}

export async function loadBookingRecords() {
  const response = (await request.get('/users/me/bookings')) as { bookings?: ApiBooking[]; serviceStatusList?: ApiBooking[] }
  // 后端 UserInfoAndServicesViaMPRespVO 的字段名是 bookings
  const list = response?.bookings || response?.serviceStatusList
  if (!Array.isArray(list)) {
    throw new Error('获取预约记录失败')
  }
  return list.map(mapBooking)
}

export async function loadAdminUsers() {
  const data = (await request.get('/users/list')) as { records?: ApiUser[] } | ApiUser[]
  // 后端返回 PageResult 分页结构，数据在 records 字段中
  const list = Array.isArray(data) ? data : data?.records
  if (!Array.isArray(list)) {
    throw new Error('获取用户列表失败')
  }
  return list
}

export async function loadAdminSummary(): Promise<AdminSummary> {
  const [users, services, bookings] = await Promise.all([loadAdminUsers(), loadServiceCards(), loadBookingRecords()])
  const approvedCount = bookings.filter((item) => item.status === 'approved').length
  const total = bookings.length || 1

  return {
    totalUsers: users.length || 0,
    totalServices: services.length,
    activeBookings: bookings.filter((item) => ['pending', 'approved'].includes(item.status)).length,
    approvalRate: `${Math.round((approvedCount / total) * 1000) / 10}%`,
  }
}

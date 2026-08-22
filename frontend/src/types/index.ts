export type UserRole = 'user' | 'admin' | 'super_admin'

export type ServiceStatus = 'available' | 'busy' | 'maintenance'
export type BookingStatus = 'pending' | 'approved' | 'rejected' | 'completed' | 'cancelled'
export type BookingType = 'room' | 'equipment' | 'consultation' | 'printing'

export interface UserInfo {
  id: number
  username: string
  email: string
  phone: string
  role: UserRole
  department: string
  avatar?: string
  createdAt: string
}

export interface ServiceCard {
  id: number
  code: string
  name: string
  description: string
  type: BookingType
  category: string
  location: string
  priceLabel: string
  status: ServiceStatus
  tags: string[]
  image: string
  capacity?: number
}

export interface RoomResource {
  id: number
  name: string
  building: string
  floor: string
  capacity: number
  status: 'available' | 'occupied' | 'maintenance'
  facilities: string[]
  manager: string
  openTime: string
  image: string
}

export interface EquipmentResource {
  id: number
  name: string
  category: string
  description: string
  stock: number
  availableStock: number
  unit: string
  priceLabel: string
  location: string
  image: string
}

export interface Consultant {
  id: number
  name: string
  title: string
  department: string
  expertise: string[]
  rating: number
  reviews: number
  available: boolean
  nextSlot: string
  avatar?: string
}

export interface BookingRecord {
  id: number
  bookingNo: string
  serviceName: string
  type: BookingType
  applicant: string
  department: string
  location: string
  date: string
  timeRange: string
  status: BookingStatus
  createdAt: string
  remarks?: string
  orderId?: number
}

export interface MessageItem {
  id: number
  title: string
  content: string
  time: string
  type: 'system' | 'approval' | 'notice'
  unread: boolean
}

export interface DashboardStat {
  label: string
  value: string
  trend: string
  tone: 'brand' | 'success' | 'warning' | 'danger'
}

export interface AdminSummary {
  totalUsers: number
  totalServices: number
  activeBookings: number
  approvalRate: string
}

export interface BookingDraft {
  targetId: number
  targetName: string
  date: string
  startTime: string
  endTime: string
  contactName: string
  contactPhone: string
  purpose: string
  notes: string
}
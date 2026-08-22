import request from '@/common/utils/request'
import type { UserInfo } from '@/types'

// 用户相关API
export const userAPI = {
  // 获取图形验证码
  getGraphicCaptcha: () => {
    return request.get('/captcha')
  },

  // 登录
  login: (data: { email: string; password: string; captcha?: string }) => {
    return request.post('/auth/login', data)
  },

  // 注册
  register: (data: { name: string; email: string; password: string; code: string; grade?: string; sex?: string }) => {
    return request.post('/auth/register', data)
  },

  // 登出
  logout: () => {
    return Promise.resolve()
  },

  // 获取当前用户信息
  getInfo: () => {
    return request.get('/users/me')
  },

  // 更新用户信息
  updateInfo: (data: Partial<UserInfo>) => {
    return request.put('/users/me', data)
  },

  // 修改密码
  changePassword: (data: { oldPassword: string; newPassword: string }) => {
    return request.put('/users/password', data)
  },

  // 发送邮箱验证码
  sendCaptcha: (email: string) => {
    return request.post('/auth/verification-code', { to: email })
  }
}

// 预约相关API
export const bookingAPI = {
  // 获取预约列表
  getBookings: (params: {
    pageNo?: number
    pageSize?: number
    type?: string
    date?: string
    status?: string
  }) => {
    return request.get('/app/bookings', { params })
  },

  // 获取预约详情
  getBooking: (id: string) => {
    return request.get(`/app/bookings/${id}`)
  },

  // 创建预约
  createBooking: (data: { serviceIds: number[] }) => {
    return request.post('/app/bookings', data)
  },

  // 创建会议室预约
  createRoomBooking: (data: {
    roomId: string
    date: string
    startTime: string
    endTime: string
    purpose: string
    remarks?: string
  }) => {
    return request.post('/app/bookings/room', data)
  },

  // 创建设备预约
  createEquipmentBooking: (data: {
    equipmentId: string
    date: string
    startTime: string
    endTime: string
    purpose: string
    remarks?: string
  }) => {
    return request.post('/app/bookings/equipment', data)
  },

  // 创建咨询预约
  createConsultationBooking: (data: {
    consultantId: string
    date: string
    startTime: string
    endTime: string
    subject: string
    description: string
  }) => {
    return request.post('/app/bookings/consultation', data)
  },

  // 取消预约
  cancelBooking: (bookingId: string) => {
    return request.patch(`/app/bookings/${bookingId}/cancel`)
  },

  // 管理员审批预约
  approveBooking: (bookingId: string, status: 'approved' | 'rejected', remark?: string) => {
    const auditStatus = status === 'approved' ? 1 : 2
    const path = status === 'approved'
      ? `/admin/bookings/${bookingId}/approve`
      : `/admin/bookings/${bookingId}/reject`
    return request.patch(path, { orderId: Number(bookingId), status: auditStatus, reason: remark || '' })
  }
}

// 服务相关API (原会议室/设备查询使用 /services)
export const roomAPI = {
  // 获取服务列表（后端仅支持 serviceName 模糊 + serviceState 状态筛选）
  getRooms: (params: {
    pageNo?: number
    pageSize?: number
    serviceName?: string
    serviceState?: number
  }) => {
    return request.get('/app/services', { params })
  },

  // 获取服务详情
  getRoom: (id: string) => {
    return request.get(`/app/services/${id}`)
  },

  // 获取可用服务（serviceState=1）
  getAvailableRooms: (params: { serviceName?: string } = {}) => {
    return request.get('/app/services', { params: { ...params, serviceState: 1 } })
  }
}

// 设备相关API
export const equipmentAPI = {
  getEquipment: (params: {
    pageNo?: number
    pageSize?: number
    category?: string
    available?: boolean
  }) => {
    return request.get('/app/equipment', { params })
  },
  getCategories: () => {
    return request.get('/app/equipment/categories')
  },
  getEquipmentDetail: (id: string) => {
    return request.get(`/app/equipment/${id}`)
  }
}

// 咨询相关API
export const consultationAPI = {
  getConsultants: (params: {
    pageNo?: number
    pageSize?: number
    department?: string
    available?: boolean
  }) => {
    return request.get('/app/consultations', { params })
  },
  getConsultant: (id: string) => {
    return request.get(`/app/consultations/${id}`)
  },
  getAvailableTime: (consultantId: string, date: string) => {
    return request.get(`/app/consultations/${consultantId}/slots`, { params: { date } })
  }
}

// 管理员相关API
export const adminAPI = {
  // 获取用户列表
  getUsers: (params: {
    pageNo?: number
    pageSize?: number
    username?: string
    email?: string
    status?: string
  }) => {
    return request.get('/users/list', { params })
  },

  // 获取预约管理列表
  getBookingManagement: (params: {
    pageNo?: number
    pageSize?: number
    type?: string
    status?: string
    date?: string
  }) => {
    return request.get('/admin/bookings', { params })
  },
}

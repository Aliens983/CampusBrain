import { userAPI, bookingAPI, roomAPI, equipmentAPI, consultationAPI, adminAPI } from './api'
import type { UserInfo } from '@/types'

// 用户服务
export const userService = {
  /**
   * 登录
   */
  async login(email: string, password: string, captcha?: string) {
    return await userAPI.login({ email, password, captcha })
  },
  
  /**
   * 注册
   */
  async register(name: string, email: string, password: string, code: string) {
    return await userAPI.register({ name, email, password, code })
  },
  
  /**
   * 登出
   */
  async logout() {
    return await userAPI.logout()
  },
  
  /**
   * 获取用户信息
   */
  async getInfo() {
    return await userAPI.getInfo()
  },
  
  /**
   * 更新用户信息
   */
  async updateInfo(data: Partial<UserInfo>) {
    return await userAPI.updateInfo(data)
  },
  
  /**
   * 修改密码
   */
  async changePassword(oldPassword: string, newPassword: string) {
    return await userAPI.changePassword({ oldPassword, newPassword })
  },
  
  /**
   * 发送验证码
   */
  async sendCaptcha(email: string) {
    return await userAPI.sendCaptcha(email)
  }
}

// 预约服务
export const bookingService = {
  /**
   * 获取预约列表
   */
  async getBookings(params: {
    pageNo?: number
    pageSize?: number
    type?: string
    date?: string
    status?: string
  }) {
    return await bookingAPI.getBookings(params)
  },
  
  /**
   * 获取预约详情
   */
  async getBooking(id: string) {
    return await bookingAPI.getBooking(id)
  },
  
  /**
   * 创建会议室预约
   */
  async createRoomBooking(data: {
    roomId: string
    date: string
    startTime: string
    endTime: string
    purpose: string
    remarks?: string
  }) {
    return await bookingAPI.createRoomBooking(data)
  },
  
  /**
   * 创建设备预约
   */
  async createEquipmentBooking(data: {
    equipmentId: string
    date: string
    startTime: string
    endTime: string
    purpose: string
    remarks?: string
  }) {
    return await bookingAPI.createEquipmentBooking(data)
  },
  
  /**
   * 创建咨询预约
   */
  async createConsultationBooking(data: {
    consultantId: string
    date: string
    startTime: string
    endTime: string
    subject: string
    description: string
  }) {
    return await bookingAPI.createConsultationBooking(data)
  },
  
  /**
   * 取消预约
   */
  async cancelBooking(id: string) {
    return await bookingAPI.cancelBooking(id)
  },
  
  /**
   * 管理员审批预约
   */
  async approveBooking(id: string, status: 'approved' | 'rejected', remark?: string) {
    return await bookingAPI.approveBooking(id, status, remark)
  }
}

// 会议室服务
export const roomService = {
  /**
   * 获取会议室列表
   */
  async getRooms(params: {
    pageNo?: number
    pageSize?: number
    serviceName?: string
    serviceState?: number
  }) {
    return await roomAPI.getRooms(params)
  },
  
  /**
   * 获取会议室详情
   */
  async getRoom(id: string) {
    return await roomAPI.getRoom(id)
  },
  
  /**
   * 获取可用会议室
   */
  async getAvailableRooms(params: { serviceName?: string } = {}) {
    return await roomAPI.getAvailableRooms(params)
  }
}

// 设备服务
export const equipmentService = {
  /**
   * 获取设备列表
   */
  async getEquipment(params: {
    pageNo?: number
    pageSize?: number
    category?: string
    available?: boolean
  }) {
    return await equipmentAPI.getEquipment(params)
  },
  
  /**
   * 获取设备分类
   */
  async getCategories() {
    return await equipmentAPI.getCategories()
  },
  
  /**
   * 获取设备详情
   */
  async getEquipmentDetail(id: string) {
    return await equipmentAPI.getEquipmentDetail(id)
  }
}

// 咨询服务
export const consultationService = {
  /**
   * 获取咨询师列表
   */
  async getConsultants(params: {
    pageNo?: number
    pageSize?: number
    department?: string
    available?: boolean
  }) {
    return await consultationAPI.getConsultants(params)
  },
  
  /**
   * 获取咨询师详情
   */
  async getConsultant(id: string) {
    return await consultationAPI.getConsultant(id)
  },
  
  /**
   * 获取咨询师可用时间
   */
  async getAvailableTime(consultantId: string, date: string) {
    return await consultationAPI.getAvailableTime(consultantId, date)
  }
}

// 管理员服务
export const adminService = {
  /**
   * 获取用户列表
   */
  async getUsers(params: {
    pageNo?: number
    pageSize?: number
    username?: string
    email?: string
    status?: string
  }) {
    return await adminAPI.getUsers(params)
  },

  /**
   * 获取预约管理列表
   */
  async getBookingManagement(params: {
    pageNo?: number
    pageSize?: number
    manageStatus?: number
    serviceName?: string
  }) {
    return await adminAPI.getBookingManagement(params)
  }
}
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/common/utils/request'

/** 后端 /teacher/bookings 列表项（ServiceStatusResponse 子集） */
export interface TeacherBooking {
  orderId?: number
  username?: string
  serviceName?: string
  serviceDescribe?: string
  consultantName?: string
  slotDate?: string
  startTime?: string
  endTime?: string
  campus?: string
  manageStatus?: number
  reason?: string
  createTime?: string
}

export async function fetchTeacherBookings(status?: number): Promise<TeacherBooking[]> {
  const query = new URLSearchParams({ pageNo: '1', pageSize: '200' })
  if (status !== undefined && status !== null) query.set('status', String(status))
  const resp = (await request.get(`/teacher/bookings?${query.toString()}`)) as {
    records?: TeacherBooking[]
  } | null
  return resp?.records || []
}

export function campusName(c?: string): string {
  if (c === 'cq') return '仓前'
  if (c === 'xs') return '下沙'
  return ''
}

export async function approveBooking(orderId: number): Promise<void> {
  await request.patch(`/teacher/bookings/${orderId}/approve`, { reason: null })
  ElMessage.success('已通过，申请学生将收到通知')
}

export async function rejectBooking(orderId: number): Promise<void> {
  const { value } = await ElMessageBox.prompt('请填写拒绝原因（必填，用于通知学生）', '拒绝该申请', {
    inputType: 'textarea',
    inputPlaceholder: '如：该时段已有其他安排',
    inputValidator: (v: string) => (v && v.trim() ? true : '拒绝必须填写原因'),
    confirmButtonText: '确认拒绝',
  })
  await request.patch(`/teacher/bookings/${orderId}/reject`, { reason: value })
  ElMessage.success('已拒绝，时段已释放')
}

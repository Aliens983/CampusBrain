import type { UserInfo, UserRole } from '@/types'

type AnyRecord = Record<string, any>

export function normalizeRole(input: unknown): UserRole {
  const value = String(input || '').toLowerCase()
  if (['super_admin', 'superadmin', 'root'].includes(value)) return 'super_admin'
  if (['admin', 'administrator'].includes(value)) return 'admin'
  return 'user'
}

export function isAdminRole(role: unknown) {
  const normalized = normalizeRole(role)
  return normalized === 'admin' || normalized === 'super_admin'
}

export function resolveHomeByRole(role: unknown) {
  return isAdminRole(role) ? '/admin' : '/dashboard'
}

export function extractToken(payload: unknown): string {
  const source = (payload || {}) as AnyRecord
  return (
    source.token ||
    source.accessToken ||
    source.access_token ||
    source.jwt ||
    source.data?.token ||
    source.data?.accessToken ||
    source.data?.access_token ||
    ''
  )
}

export function normalizeUserInfo(payload: unknown): UserInfo {
  const source = (payload || {}) as AnyRecord
  const data = (source.data && typeof source.data === 'object' ? source.data : source) as AnyRecord

  return {
    id: Number(data.id || data.userId || data.uid || 0),
    username: data.username || data.nickname || data.name || data.realName || '用户',
    email: data.email || '',
    phone: data.phone || data.mobile || '',
    role: normalizeRole(data.role || data.userRole || data.identity),
    department: data.department || data.deptName || data.orgName || '未分配部门',
    avatar: data.avatar || data.avatarUrl || '',
    createdAt: data.createdAt || data.createTime || new Date().toISOString(),
  }
}

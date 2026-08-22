import type { UserRole } from '@/common/types'

export function isAdminRole(role?: UserRole): boolean {
  return role === 'admin' || role === 'super_admin'
}

export function normalizeRole(role: string | number | undefined): UserRole {
  if (role === 'admin' || role === 1) return 'admin'
  if (role === 'super_admin' || role === 2) return 'super_admin'
  return 'user'
}

export function resolveHomeByRole(role?: UserRole): string {
  if (isAdminRole(role)) return '/admin'
  return '/dashboard'
}

export function extractToken(data: unknown): string {
  if (typeof data === 'string') return data
  if (typeof data === 'object' && data !== null) {
    const obj = data as Record<string, unknown>
    if (typeof obj.token === 'string') return obj.token
    if (typeof obj.access_token === 'string') return obj.access_token
    if (typeof obj.data === 'object' && obj.data !== null) {
      const nested = obj.data as Record<string, unknown>
      if (typeof nested.token === 'string') return nested.token
      if (typeof nested.access_token === 'string') return nested.access_token
    }
  }
  return ''
}

export function normalizeUserInfo(data: unknown): UserInfo {
  if (!data || typeof data !== 'object') {
    return {
      id: 0,
      username: '未知用户',
      email: '',
      phone: '',
      role: 'user',
      department: '',
      createdAt: '',
    }
  }

  const d = data as Record<string, unknown>

  return {
    id: (d.id as number) || 0,
    username: (d.username as string) || (d.name as string) || '未知用户',
    email: (d.email as string) || '',
    phone: (d.phone as string) || '',
    role: normalizeRole(d.role as string | number | undefined),
    department: (d.department as string) || '',
    avatar: d.avatar as string | undefined,
    createdAt: (d.createdAt as string) || (d.createTime as string) || '',
  }
}

interface UserInfo {
  id: number
  username: string
  email: string
  phone: string
  role: UserRole
  department: string
  avatar?: string
  createdAt: string
}
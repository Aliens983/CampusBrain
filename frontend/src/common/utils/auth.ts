import type { UserRole } from '@/common/types'

/**
 * 角色/用户信息归一化 —— 全项目唯一实现。
 *
 * 曾存在两份：src/utils/auth.ts（legacy）与本文件，逻辑不一致：
 * legacy 版用字符串匹配，normalizeRole(1) 会误判为 'user'（管理员被降级）；
 * 本版改为同时兼容数字(0/1/2/3)与字符串('admin'/'teacher'...)两种后端返回形态。
 */

export function normalizeRole(role: unknown): UserRole {
  // 数字形态（CAS user.role 为 0 用户 / 1 管理员 / 2 超管 / 3 教师）
  if (role === 1 || role === '1') return 'admin'
  if (role === 2 || role === '2') return 'super_admin'
  if (role === 3 || role === '3') return 'teacher'

  // 字符串形态（大小写不敏感，兼容 superadmin / administrator 等别名）
  if (typeof role === 'string') {
    const v = role.toLowerCase()
    if (v === 'admin' || v === 'administrator') return 'admin'
    if (v === 'super_admin' || v === 'superadmin' || v === 'root') return 'super_admin'
    if (v === 'teacher') return 'teacher'
  }

  return 'user'
}

export function isAdminRole(role: unknown): boolean {
  const normalized = normalizeRole(role)
  return normalized === 'admin' || normalized === 'super_admin'
}

export function isTeacherRole(role: unknown): boolean {
  return normalizeRole(role) === 'teacher'
}

export function resolveHomeByRole(role: unknown): string {
  if (isAdminRole(role)) return '/admin'
  if (isTeacherRole(role)) return '/teacher'
  return '/dashboard'
}

/** 从登录响应的多种可能结构中提取 token */
export function extractToken(payload: unknown): string {
  if (typeof payload === 'string') return payload
  if (typeof payload !== 'object' || payload === null) return ''

  const obj = payload as Record<string, unknown>
  const direct = obj.token ?? obj.accessToken ?? obj.access_token ?? obj.jwt
  if (typeof direct === 'string') return direct

  if (typeof obj.data === 'object' && obj.data !== null) {
    const nested = obj.data as Record<string, unknown>
    const nestedToken = nested.token ?? nested.accessToken ?? nested.access_token ?? nested.jwt
    if (typeof nestedToken === 'string') return nestedToken
  }

  return ''
}

export interface NormalizedUserInfo {
  id: number
  username: string
  email: string
  phone: string
  role: UserRole
  department: string
  avatar?: string
  createdAt: string
}

/** 把后端多种字段命名的用户信息归一化为统一结构 */
export function normalizeUserInfo(payload: unknown): NormalizedUserInfo {
  if (!payload || typeof payload !== 'object') {
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

  // 兼容 { data: {...} } 与直接对象两种返回
  const source = payload as Record<string, unknown>
  const d = (source.data && typeof source.data === 'object' ? source.data : source) as Record<string, unknown>

  return {
    id: Number(d.id ?? d.userId ?? d.uid ?? 0),
    username: (d.username as string) || (d.nickname as string) || (d.name as string) || (d.realName as string) || '未知用户',
    email: (d.email as string) || '',
    phone: (d.phone as string) || (d.mobile as string) || '',
    role: normalizeRole(d.role ?? d.userRole ?? d.identity),
    department: (d.department as string) || (d.deptName as string) || (d.orgName as string) || '',
    avatar: d.avatar as string | undefined,
    createdAt: (d.createdAt as string) || (d.createTime as string) || '',
  }
}

import type { RouteRecordRaw } from 'vue-router'

export const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayoutShell.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: '',
        name: 'admin-home',
        component: () => import('@/views/admin/Index.vue'),
        meta: { title: '管理驾驶舱', requiresAuth: true, requiresAdmin: true },
      },
      {
        path: 'services',
        name: 'admin-services',
        component: () => import('@/views/admin/ServiceManage.vue'),
        meta: { title: '服务治理', requiresAuth: true, requiresAdmin: true },
      },
      {
        path: 'bookings',
        name: 'admin-bookings',
        component: () => import('@/views/admin/BookingManage.vue'),
        meta: { title: '预约审核', requiresAuth: true, requiresAdmin: true },
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { title: '用户与权限', requiresAuth: true, requiresAdmin: true },
      },
      {
        path: 'system',
        name: 'admin-system',
        component: () => import('@/views/admin/SystemManage.vue'),
        meta: { title: '系统设置', requiresAuth: true, requiresAdmin: true },
      },
      {
        path: 'tools',
        name: 'admin-tools',
        component: () => import('@/views/admin/Tools.vue'),
        meta: { title: '工具箱', requiresAuth: true, requiresAdmin: true },
      },
    ],
  },
]

export default adminRoutes
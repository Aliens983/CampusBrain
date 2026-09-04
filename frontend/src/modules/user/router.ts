import type { RouteRecordRaw } from 'vue-router'

export const userRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layout/UserLayoutShell.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'user-home',
        redirect: () => '/dashboard',
      },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/modules/user/views/dashboard/Index.vue'),
        meta: { title: '工作台', requiresAuth: true, audience: 'user' },
      },
      {
        path: 'services',
        name: 'services',
        component: () => import('@/modules/user/views/services/Index.vue'),
        meta: { title: '服务中心', requiresAuth: true, audience: 'user' },
      },
      {
        path: 'service/:id',
        name: 'service-detail',
        component: () => import('@/modules/user/views/services/Detail.vue'),
        meta: { title: '服务详情', requiresAuth: true, audience: 'user' },
      },
      {
        path: 'rooms',
        name: 'rooms',
        component: () => import('@/modules/user/views/rooms/Index.vue'),
        meta: { title: '会议室预约', requiresAuth: true, audience: 'user' },
      },
      {
        path: 'equipment',
        name: 'equipment',
        component: () => import('@/modules/user/views/equipment/Index.vue'),
        meta: { title: '设备借用', requiresAuth: true, audience: 'user' },
      },
      {
        path: 'consultation',
        name: 'consultation',
        component: () => import('@/modules/user/views/consultation/Index.vue'),
        meta: { title: '咨询服务', requiresAuth: true, audience: 'user' },
      },
      {
        path: 'bookings',
        name: 'bookings',
        component: () => import('@/modules/user/views/bookings/Index.vue'),
        meta: { title: '我的预约', requiresAuth: true, audience: 'user' },
      },
      {
        path: 'bookings/:id',
        name: 'booking-detail',
        component: () => import('@/modules/user/views/bookings/Detail.vue'),
        meta: { title: '预约详情', requiresAuth: true, audience: 'user' },
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/modules/user/views/profile/Index.vue'),
        meta: { title: '个人中心', requiresAuth: true },
      },
      {
        path: 'assistant',
        name: 'assistant',
        component: () => import('@/modules/assistant/views/QaPortal.vue'),
        meta: { title: 'AI 助手', requiresAuth: true, audience: 'user' },
      },
    ],
  },
]

export default userRoutes
import type { RouteRecordRaw } from 'vue-router'

export const teacherRoutes: RouteRecordRaw[] = [
  {
    path: '/teacher',
    component: () => import('@/layout/TeacherLayoutShell.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/teacher/review',
      },
      {
        path: 'review',
        name: 'teacher-review',
        component: () => import('@/modules/teacher/views/Review.vue'),
        meta: { title: '待我审核', requiresAuth: true },
      },
      {
        path: 'consultations',
        name: 'teacher-consultations',
        component: () => import('@/modules/teacher/views/Consultations.vue'),
        meta: { title: '我的咨询', requiresAuth: true },
      },
      {
        path: 'profile',
        name: 'teacher-profile',
        component: () => import('@/modules/user/views/profile/Index.vue'),
        meta: { title: '个人中心', requiresAuth: true },
      },
    ],
  },
]

export default teacherRoutes

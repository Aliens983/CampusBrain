import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/common/stores/user'
import { isAdminRole, isTeacherRole, resolveHomeByRole } from '@/common/utils/auth'
import { userRoutes } from '@/modules/user/router'
import { adminRoutes } from '@/modules/admin/router'
import { teacherRoutes } from '@/modules/teacher/router'

const lazyModules = import.meta.glob(['../modules/**/*.vue', '../layout/*.vue'])
let prefetched = false

function warmRouteChunks() {
  if (prefetched) return
  prefetched = true

  const preload = () => {
    Object.values(lazyModules)
      .slice(0, 12)
      .forEach((loader) => void loader())
  }

  if ('requestIdleCallback' in window) {
    window.requestIdleCallback(preload, { timeout: 1200 })
    return
  }

  setTimeout(preload, 600)
}

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: () => {
      const userStore = useUserStore()
      if (!userStore.isLogin) return '/login'
      return resolveHomeByRole(userStore.userInfo?.role)
    },
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginPage.vue'),
    meta: { title: '登录', requiresAuth: false, audience: 'guest' },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterPage.vue'),
    meta: { title: '注册', requiresAuth: false, audience: 'guest' },
  },
  ...userRoutes,
  ...adminRoutes,
  ...teacherRoutes,
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/errors/NotFound.vue'),
    meta: { title: '页面不存在', requiresAuth: false },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    // 浏览器前进/后退时恢复位置
    if (savedPosition) return savedPosition
    // 只在切换页面(路径变化)时回到顶部；仅改 query(分类筛选等)不滚
    if (to.path !== from.path) return { top: 0 }
    return false
  },
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLogin) {
    next('/login')
    return
  }

  if ((to.path === '/login' || to.path === '/register') && userStore.isLogin) {
    next(resolveHomeByRole(userStore.userInfo?.role))
    return
  }

  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next('/dashboard')
    return
  }

  if (userStore.isLogin && !userStore.isAdmin && String(to.path).startsWith('/admin')) {
    next(resolveHomeByRole(userStore.userInfo?.role))
    return
  }

  const role = userStore.userInfo?.role
  // 教师只进教师端（独立工作台），看不到学生/管理页面
  if (userStore.isLogin && isTeacherRole(role) && !String(to.path).startsWith('/teacher')) {
    next('/teacher/review')
    return
  }
  // /teacher/* 仅教师 / 管理员 / 超管可进
  if (String(to.path).startsWith('/teacher') && userStore.isLogin && !isTeacherRole(role) && !isAdminRole(role)) {
    next(resolveHomeByRole(role))
    return
  }

  next()
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title || '智汇校园')} · CampusBrain`
  warmRouteChunks()
})

export default router
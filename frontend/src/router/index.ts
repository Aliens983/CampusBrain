import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/common/stores/user'
import { resolveHomeByRole } from '@/common/utils/auth'
import { userRoutes } from '@/modules/user/router'
import { adminRoutes } from '@/modules/admin/router'

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
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/errors/NotFound.vue'),
    meta: { title: '页面不存在', requiresAuth: false },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 }),
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
    next('/dashboard')
    return
  }

  next()
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title || '校园预约系统')} - Campus Appointment System`
  warmRouteChunks()
})

export default router
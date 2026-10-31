<template>
  <div class="shell">
    <header class="shell__header">
      <div
        class="brand"
        @click="router.push('/dashboard')"
      >
        <span class="hz-seal brand__seal">杭</span>
        <span class="brand__text">
          <strong>智汇校园</strong>
          <span>HANGZHOU NORMAL UNIVERSITY</span>
        </span>
      </div>

      <nav
        class="nav"
        aria-label="主导航"
      >
        <button
          v-for="item in navItems"
          :key="item.path"
          class="nav__item"
          :class="{ 'is-active': isNavActive(item) }"
          @click="router.push(item.path)"
        >
          <span class="nav__index">{{ item.index }}</span>
          <span>{{ item.label }}</span>
          <span
            v-if="item.chat && unread > 0"
            class="nav__badge"
          >{{ unread }}</span>
        </button>
      </nav>

      <div class="header-right">
        <div
          v-if="weather"
          class="weather"
        >
          <el-icon class="weather__icon">
            <component :is="weatherIcon(weather.weather1)" />
          </el-icon>
          <span class="weather__place">杭州</span>
          <span class="weather__text">{{ weather.weather1 }} {{ weather.temp }}</span>
        </div>

        <span class="header-rule" />

        <el-dropdown
          class="user-dropdown"
          @command="handleCommand"
        >
          <span class="user-trigger">
            <span class="user-avatar">{{ initial }}</span>
            <span class="user-meta">
              <span class="user-name">{{ userStore.userInfo?.username || '校园用户' }}</span>
              <span class="user-role">HZNU MEMBER</span>
            </span>
            <el-icon class="user-caret">
              <ArrowDown />
            </el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                个人中心
              </el-dropdown-item>
              <el-dropdown-item
                command="logout"
                divided
              >
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <main class="shell__content">
      <router-view />
    </main>

    <footer class="shell__footer">
      <span class="shell__motto">勤慎诚恕 · 博雅精进</span>
      <span class="shell__meta">仓前 · 下沙 / HZNU 1908</span>
      <span class="shell__meta">智汇校园 CampusBrain</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/common/stores/user'
import request from '@/common/utils/request'
import { useChatUnread, refreshChatUnread } from '@/common/consultChat'

const route = useRoute()
const router = useRouter()
const weather = ref<{ shi: string; weather1: string; temp: string } | null>(null)

let unreadTimer: number | undefined

onMounted(async () => {
  try {
    weather.value = (await request.get('/weather/local')) as { shi: string; weather1: string; temp: string } | null
  } catch { /* 静默 */ }
  refreshChatUnread()
  unreadTimer = window.setInterval(refreshChatUnread, 20000)
})
onUnmounted(() => {
  if (unreadTimer) window.clearInterval(unreadTimer)
})

function weatherIcon(d: string) {
  if (!d) return 'Sunny'
  if (d.includes('晴')) return 'Sunny'
  if (d.includes('云')) return 'Cloudy'
  if (d.includes('雨')) return 'Drizzling'
  if (d.includes('雪')) return 'Cloudy'
  return 'PartlyCloudy'
}
const userStore = useUserStore()

interface NavItem {
  label: string
  path: string
  index: string
  chat?: boolean
  match?: string[]
}

const navItems: NavItem[] = [
  { label: '工作台', path: '/dashboard', index: '壹' },
  { label: '服务中心', path: '/services', index: '贰', match: ['/services', '/service'] },
  { label: '我的预约', path: '/bookings', index: '叁' },
  { label: '消息', path: '/chat', index: '肆', chat: true },
  { label: 'AI 助手', path: '/assistant', index: '伍' },
  { label: '个人中心', path: '/profile', index: '陆' },
]

function isNavActive(item: NavItem) {
  return (item.match ?? [item.path]).some(path => route.path.startsWith(path))
}

const unread = useChatUnread()

const initial = computed(() => userStore.userInfo?.username?.slice(0, 1) || 'U')

function handleCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
    return
  }
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped lang="scss">
.shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.shell__header {
  position: sticky;
  top: 0;
  z-index: 40;
  display: flex;
  align-items: center;
  gap: 28px;
  height: 80px;
  padding: 0 max(28px, calc((100vw - 1440px) / 2));
  background: rgba(245, 241, 230, 0.9);
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(16px);
}

.shell__header::after {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: 3px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(176, 138, 69, 0.42), transparent);
  pointer-events: none;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  flex-shrink: 0;
}

.brand__seal {
  width: 38px;
  height: 38px;
  font-size: 19px;
}

.brand__text {
  display: grid;
  line-height: 1.2;
}

.brand__text strong {
  font-family: var(--font-serif);
  font-size: 19px;
  font-weight: 600;
  letter-spacing: 0.12em;
  color: var(--ink);
}

.brand__text span {
  margin-top: 4px;
  font-family: var(--font-latin);
  font-size: 8px;
  font-weight: 600;
  letter-spacing: 0.17em;
  color: var(--ink-4);
  white-space: nowrap;
}

.nav {
  display: flex;
  align-items: stretch;
  gap: 2px;
  flex: 1;
  justify-content: center;
  height: 100%;
  min-width: 0;
}

.nav__item {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 14px;
  border: 0;
  background: transparent;
  color: var(--ink-3);
  font-family: var(--font-sans);
  font-size: 13.5px;
  letter-spacing: 0.02em;
  white-space: nowrap;
  cursor: pointer;
  transition: color 0.16s ease;
}

.nav__item::after {
  content: "";
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 17px;
  height: 1.5px;
  border-radius: 999px;
  background: var(--gold);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.22s var(--ease-out);
}

.nav__item:hover {
  color: var(--lake);
}

.nav__item:hover::after,
.nav__item.is-active::after {
  transform: scaleX(1);
}

.nav__item.is-active {
  color: var(--ink);
  font-weight: 600;
}

.nav__index {
  font-family: var(--font-latin);
  font-size: 9px;
  color: var(--gold);
  opacity: 0.7;
  transform: translateY(-4px);
}

.nav__badge {
  display: grid;
  place-items: center;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  background: var(--seal);
  color: #fffdf7;
  font-size: 9px;
  font-weight: 600;
  line-height: 1;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.weather {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--ink-3);
  white-space: nowrap;
}

.weather__icon {
  font-size: 13px;
  line-height: 1;
}

.weather__place {
  font-family: var(--font-latin);
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.18em;
  color: var(--gold);
}

.weather__text {
  color: var(--ink-3);
}

.header-rule {
  width: 1px;
  height: 20px;
  background: var(--line);
}

.user-dropdown {
  cursor: pointer;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 9px;
}

.user-avatar {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid var(--line-strong);
  background: var(--surface);
  color: var(--lake);
  font-family: var(--font-serif);
  font-size: 14px;
  font-weight: 600;
}

.user-meta {
  display: grid;
  line-height: 1.25;
}

.user-name {
  max-width: 84px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12.5px;
  color: var(--ink);
}

.user-role {
  margin-top: 2px;
  font-family: var(--font-latin);
  font-size: 8px;
  letter-spacing: 0.14em;
  color: var(--ink-4);
}

.user-caret {
  font-size: 11px;
  color: var(--ink-4);
}

.shell__content {
  flex: 1;
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  padding: 28px 28px 0;
}

.shell__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
  max-width: 1440px;
  margin: 40px auto 0;
  padding: 22px 28px 30px;
  border-top: 1px solid var(--line);
  color: var(--ink-4);
  font-size: 10.5px;
  letter-spacing: 0.05em;
}

.shell__motto {
  font-family: var(--font-serif);
  font-size: 12px;
  letter-spacing: 0.18em;
  color: var(--ink-3);
}

.shell__motto::before {
  content: "—— ";
  color: var(--gold);
}

.shell__meta {
  font-family: var(--font-latin);
  letter-spacing: 0.1em;
}

@media (max-width: 1120px) {
  .nav__index {
    display: none;
  }

  .nav__item {
    padding: 0 11px;
  }

  .nav__item::after {
    left: 11px;
    right: 11px;
  }

  .brand__text span {
    display: none;
  }
}

@media (max-width: 900px) {
  .shell__header {
    height: auto;
    flex-wrap: wrap;
    gap: 10px;
    padding: 12px 20px 0;
  }

  .brand {
    flex: 1;
  }

  .header-right {
    gap: 10px;
  }

  .weather,
  .header-rule,
  .user-meta {
    display: none;
  }

  .nav {
    order: 3;
    flex-basis: 100%;
    height: 48px;
    justify-content: flex-start;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .nav::-webkit-scrollbar {
    display: none;
  }

  .nav__item {
    flex-shrink: 0;
    padding: 0 10px;
  }

  .nav__item::after {
    left: 10px;
    right: 10px;
    bottom: 9px;
  }

  .shell__content {
    padding: 20px 20px 0;
  }

  .shell__footer {
    flex-wrap: wrap;
    padding: 20px 20px 26px;
  }
}

@media (max-width: 560px) {
  .shell__header {
    padding: 10px 14px 0;
  }

  .shell__content {
    padding: 16px 14px 0;
  }

  .shell__footer {
    padding: 18px 14px 24px;
  }

  .shell__meta:last-child {
    display: none;
  }
}
</style>

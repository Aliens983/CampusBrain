<template>
  <div class="shell">
    <header class="shell__header glass-panel">
      <div
        class="brand"
        @click="router.push('/dashboard')"
      >
        <div class="brand__mark">
          CAS
        </div>
        <span class="brand__title">校园统一预约门户</span>
      </div>

      <nav class="nav">
        <button
          v-for="item in navItems"
          :key="item.path"
          class="nav__item"
          :class="{ 'is-active': route.path.startsWith(item.path) }"
          @click="router.push(item.path)"
        >
          {{ navLabel(item) }}
        </button>
      </nav>

      <div class="header-right">
        <div
          v-if="weather"
          class="weather-pill"
        >
          <span class="weather-pill__icon">{{ weatherIcon(weather.weather1) }}</span>
          <span class="weather-pill__text">{{ weather.shi }} {{ weather.weather1 }} {{ weather.temp }}</span>
        </div>

        <el-dropdown
          class="user-dropdown"
          @command="handleCommand"
        >
          <span class="user-trigger">
            <el-avatar :size="34">{{ initial }}</el-avatar>
            <span class="user-name">{{ userStore.userInfo?.username || '校园用户' }}</span>
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
  try { weather.value = await request.get('/weather/local') as any } catch { /* 静默 */ }
  refreshChatUnread()
  unreadTimer = window.setInterval(refreshChatUnread, 20000)
})
onUnmounted(() => {
  if (unreadTimer) window.clearInterval(unreadTimer)
})

function weatherIcon(d: string) {
  if (!d) return '☀️'; if (d.includes('晴')) return '☀️'; if (d.includes('云')) return '⛅'; if (d.includes('雨')) return '🌧'; if (d.includes('雪')) return '🌨'; return '🌈'
}
const userStore = useUserStore()

const navItems = [
  { label: '工作台', path: '/dashboard' },
  { label: '服务中心', path: '/services' },
  { label: '我的预约', path: '/bookings' },
  { label: '消息', path: '/chat', chat: true },
  { label: 'AI 助手', path: '/assistant' },
  { label: '个人中心', path: '/profile' },
]

const unread = useChatUnread()

function navLabel(item: { label: string; chat?: boolean }) {
  if (item.chat && unread.value > 0) return `${item.label} · ${unread.value}`
  return item.label
}

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
  padding: 18px;
}

.shell__header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 20px;
  border-radius: 20px;
  flex-wrap: nowrap;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  flex-shrink: 0;
}
.brand__mark {
  width: 36px; height: 36px; display: grid; place-items: center;
  border-radius: 10px; color: #fff; font-weight: 800; font-size: 14px;
  background: linear-gradient(135deg, #3FB6FF, #ADE2FF);
}
.brand__title { font-size: 14px; font-weight: 600; color: var(--text-primary); white-space: nowrap; }

.nav { display: flex; gap: 4px; flex: 1; justify-content: center; }
.nav__item {
  padding: 8px 16px; border: 0; border-radius: 999px; font-size: 13px;
  background: transparent; color: var(--text-secondary); cursor: pointer; white-space: nowrap;
  transition: background .2s, color .2s;
}
.nav__item:hover { background: rgba(63,182,255,.06); color: var(--brand-500); }
.nav__item.is-active { color: #fff; background: linear-gradient(135deg, #3FB6FF, #ADE2FF); }

.header-right { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.user-dropdown { cursor: pointer; }
.user-trigger { display: flex; align-items: center; gap: 8px; }
.user-name { font-size: 13px; font-weight: 500; color: var(--text-primary); max-width: 80px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.hero {
  margin-top: 18px;
  padding: 28px 30px;
  border-radius: 28px;
  color: #fff;
  display: grid;
  grid-template-columns: 1.25fr 0.75fr;
  gap: 20px;
  background: linear-gradient(135deg, #0E6CD6, #3FB6FF 55%, #ADE2FF);
  box-shadow: var(--shadow-card);
}

.hero h1 {
  margin: 14px 0 10px;
  font-size: 34px;
  line-height: 1.2;
}

.hero p {
  margin: 0;
  max-width: 760px;
  line-height: 1.75;
  color: rgba(255, 255, 255, 0.86);
}

.shell__content {
  margin-top: 18px;
}

@media (max-width: 960px) {
  .shell__header { flex-wrap: wrap; gap: 10px; }
  .nav { order: 3; flex-basis: 100%; justify-content: flex-start; }
  .brand__title { display: none; }
  .user-name { display: none; }
}
@media (max-width: 500px) {
  .shell { padding: 8px; }
  .nav__item { padding: 6px 12px; font-size: 12px; }
}

.weather-pill { display: flex; align-items: center; gap: 5px; padding: 4px 12px; border-radius: 8px; background: rgba(63,182,255,.05); border: 1px solid rgba(63,182,255,.1); }
.weather-pill__icon { font-size: 16px; }
.weather-pill__text { font-size: 12px; color: var(--text-secondary); white-space: nowrap; }
@media (max-width: 860px) { .weather-pill__text { display: none; } }

/* 「管理后台」切换钮：与全局按钮同语言的小胶囊 */
.header-right .admin-switch {
  height: 34px;
  padding: 0 16px;
  border-radius: 999px;
  font-size: 13px;
  border-color: #C8EBFF;
  color: #1E98F2;
  background: #fff;
}
.header-right .admin-switch:hover {
  color: #fff;
  border-color: transparent;
  background: linear-gradient(135deg, #7BD0FF, #1E98F2);
}
</style>

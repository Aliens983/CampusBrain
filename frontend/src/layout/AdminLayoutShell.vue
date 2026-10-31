<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <div class="sidebar__brand">
        <span class="sidebar__seal">杭</span>
        <div class="sidebar__brand-text">
          <strong>智汇校园</strong>
          <p>管理控制台</p>
          <span>HZNU · ADMIN CONSOLE</span>
        </div>
      </div>

      <nav class="sidebar__nav">
        <button
          v-for="item in navItems"
          :key="item.path"
          class="sidebar__item"
          :class="{ 'is-active': route.path === item.path }"
          @click="router.push(item.path)"
        >
          <el-icon class="sidebar__icon">
            <component :is="item.icon" />
          </el-icon>
          {{ item.label }}
        </button>
      </nav>

      <div class="sidebar__foot">
        <span class="sidebar__motto">勤慎诚恕 · 博雅精进</span>
        <span>HZNU · 1908</span>
      </div>
    </aside>

    <div class="workspace">
      <header class="workspace__header">
        <div class="workspace__title">
          <span>Admin console</span>
          <strong>校园预约管理台</strong>
        </div>

        <div class="workspace__header-right">
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
          <el-button
            class="workspace__logout"
            @click="logout"
          >
            退出
          </el-button>
        </div>
      </header>

      <main class="workspace__content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/common/stores/user'
import request from '@/common/utils/request'

const weather = ref<{ shi: string; weather1: string; temp: string } | null>(null)

onMounted(async () => {
  try {
    weather.value = (await request.get('/weather/local')) as { shi: string; weather1: string; temp: string } | null
  } catch { /* 静默 */ }
})

function weatherIcon(d: string) {
  if (!d) return 'Sunny'
  if (d.includes('晴')) return 'Sunny'
  if (d.includes('云')) return 'Cloudy'
  if (d.includes('雨')) return 'Drizzling'
  if (d.includes('雪')) return 'Cloudy'
  return 'PartlyCloudy'
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const navItems = [
  { label: '管理概览', path: '/admin', icon: 'Odometer' },
  { label: '服务治理', path: '/admin/services', icon: 'Grid' },
  { label: '预约审核', path: '/admin/bookings', icon: 'Calendar' },
  { label: '排班调整审批', path: '/admin/schedule-cancellations', icon: 'Calendar' },
  { label: '用户与权限', path: '/admin/users', icon: 'User' },
  { label: '系统设置', path: '/admin/system', icon: 'Setting' },
  { label: '工具箱', path: '/admin/tools', icon: 'Tools' },
  { label: 'AI 助手', path: '/admin/assistant', icon: 'ChatDotRound' },
]

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped lang="scss">
.admin-shell {
  height: 100vh;
  overflow: hidden;
  display: grid;
  grid-template-columns: 264px 1fr;
}

.sidebar {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 24px 16px 18px;
  color: #f7f1e4;
  background:
    radial-gradient(ellipse 80% 40% at 20% 0%, rgba(63, 182, 255, 0.12), transparent 60%),
    linear-gradient(180deg, #17333d 0%, #1d3f4a 58%, #244c58 100%);
  overflow-y: auto;
}

.sidebar::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.1;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='420' height='80' viewBox='0 0 420 80'%3E%3Cpath d='M0 40 Q 52.5 16 105 40 T 210 40 T 315 40 T 420 40' fill='none' stroke='%23d6c19a' stroke-width='1'/%3E%3C/svg%3E");
  background-size: 420px 80px;
}

.sidebar__brand {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 6px 10px 22px;
  border-bottom: 1px solid rgba(247, 241, 228, 0.12);
}

.sidebar__seal {
  display: inline-grid;
  place-items: center;
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border: 1.5px solid rgba(214, 193, 154, 0.64);
  border-radius: 3px;
  color: var(--gold-soft);
  background: rgba(247, 241, 228, 0.04);
  box-shadow: inset 0 0 0 3px #17333d, inset 0 0 0 4px rgba(214, 193, 154, 0.28);
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
}

.sidebar__brand-text {
  display: grid;
  line-height: 1.3;
}

.sidebar__brand-text strong {
  font-family: var(--font-serif);
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.12em;
}

.sidebar__brand-text p {
  margin: 3px 0 0;
  font-size: 11px;
  color: rgba(247, 241, 228, 0.6);
}

.sidebar__brand-text span {
  margin-top: 5px;
  font-family: var(--font-latin);
  font-size: 7.5px;
  letter-spacing: 0.15em;
  color: rgba(247, 241, 228, 0.36);
}

.sidebar__nav {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 3px;
  margin-top: 20px;
}

.sidebar__item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  width: 100%;
  padding: 12px 14px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: rgba(247, 241, 228, 0.68);
  font-family: var(--font-sans);
  font-size: 13px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  transition:
    background-color 0.16s ease,
    color 0.16s ease;
}

.sidebar__icon {
  font-size: 16px;
  flex-shrink: 0;
  opacity: 0.76;
  transition: opacity 0.16s ease;
}

.sidebar__item:hover {
  color: #f7f1e4;
  background: rgba(247, 241, 228, 0.06);
}

.sidebar__item:hover .sidebar__icon,
.sidebar__item.is-active .sidebar__icon {
  opacity: 1;
}

.sidebar__item.is-active {
  color: #f7f1e4;
  font-weight: 600;
  background: rgba(247, 241, 228, 0.1);
}

.sidebar__item.is-active::before {
  content: "";
  position: absolute;
  left: -16px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  border-radius: 0 3px 3px 0;
  background: var(--gold-soft);
}

.sidebar__foot {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 5px;
  margin-top: auto;
  padding: 18px 10px 0;
  border-top: 1px solid rgba(247, 241, 228, 0.12);
  font-family: var(--font-latin);
  font-size: 7.5px;
  letter-spacing: 0.14em;
  color: rgba(247, 241, 228, 0.32);
}

.sidebar__motto {
  font-family: var(--font-serif);
  font-size: 10px;
  letter-spacing: 0.14em;
  color: rgba(214, 193, 154, 0.66);
}

.workspace {
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 16px 18px 0;
  overflow: hidden;
}

.workspace__header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 20px;
  border-radius: 12px;
  border: 1px solid var(--line);
  background: rgba(255, 253, 247, 0.82);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(14px);
}

.workspace__title {
  display: grid;
  line-height: 1.3;
}

.workspace__title span {
  font-family: var(--font-latin);
  font-size: 8px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--gold);
}

.workspace__title strong {
  margin-top: 3px;
  font-family: var(--font-serif);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--ink);
}

.workspace__header-right {
  display: flex;
  align-items: center;
  gap: 12px;
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
  letter-spacing: 0.16em;
  color: var(--gold);
}

.workspace__logout {
  height: 32px;
  padding: 0 16px;
  font-size: 12.5px;
}

.workspace__content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  margin-top: 18px;
  padding-bottom: 24px;
}

@media (max-width: 960px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    display: none;
  }

  .workspace {
    padding: 12px;
  }

  .workspace__header {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .workspace__header-right {
    justify-content: space-between;
  }
}

@media (max-width: 600px) {
  .weather__text {
    display: none;
  }
}
</style>

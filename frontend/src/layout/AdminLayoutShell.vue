<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <div class="sidebar__brand">
        <div class="sidebar__mark">
          CAS
        </div>
        <div>
          <strong>Admin Console</strong>
          <p>校园预约后台</p>
        </div>
      </div>

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
    </aside>

    <div class="workspace">
      <header class="workspace__header glass-panel">
        <div
          v-if="weather"
          class="weather-pill"
        >
          <span class="weather-pill__icon">{{ weatherIcon(weather.weather1) }}</span>
          <span class="weather-pill__text">{{ weather.shi }} {{ weather.weather1 }} {{ weather.temp }}</span>
        </div>
        <div class="workspace__actions">
          <el-button @click="logout">
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
  try { weather.value = await request.get('/weather/local') as any } catch { /* 静默 */ }
})

function weatherIcon(d: string) {
  if (!d) return '☀️'; if (d.includes('晴')) return '☀️'; if (d.includes('云')) return '⛅'; if (d.includes('雨')) return '🌧'; if (d.includes('雪')) return '🌨'; return '🌈'
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const navItems = [
  { label: '管理概览', path: '/admin', icon: 'Odometer' },
  { label: '服务治理', path: '/admin/services', icon: 'Grid' },
  { label: '预约审核', path: '/admin/bookings', icon: 'Calendar' },
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
  height: 100vh;            /* 整页定高，不让 body 滚动，左栏因此固定 */
  overflow: hidden;
  display: grid;
  grid-template-columns: 272px 1fr;
}

.sidebar {
  position: relative;
  padding: 20px 16px;
  background:
    radial-gradient(circle at 18% -6%, rgba(123, 208, 255, 0.22), transparent 46%),
    linear-gradient(180deg, #143A78, #1A4C92 55%, #1F63B8);
  color: #fff;
  overflow-y: auto;         /* 菜单多时左栏内部自滚，但栏体始终固定 */
}

.sidebar__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding: 8px;
}

.sidebar__mark {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: linear-gradient(135deg, #ADE2FF, #1E98F2);
  font-weight: 800;
}

.sidebar__brand p {
  margin: 4px 0 0;
  color: rgba(255, 255, 255, 0.68);
  font-size: 12px;
}

.sidebar__item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  width: 100%;
  margin-bottom: 6px;
  padding: 13px 16px;
  text-align: left;
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.78);
  border: 0;
  border-radius: 14px;
  background: transparent;
  cursor: pointer;
  transition: background 0.22s ease, color 0.22s ease, transform 0.22s ease;
}

.sidebar__icon {
  font-size: 17px;
  flex-shrink: 0;
  opacity: 0.85;
  transition: opacity 0.22s ease;
}

.sidebar__item:hover .sidebar__icon,
.sidebar__item.is-active .sidebar__icon {
  opacity: 1;
}

.sidebar__item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
  transform: translateX(2px);
}

.sidebar__item.is-active {
  color: #fff;
  font-weight: 700;
  background: linear-gradient(135deg, rgba(63, 182, 255, 0.95), rgba(30, 152, 242, 0.9));
  box-shadow: 0 10px 24px rgba(30, 120, 220, 0.34);
}

.sidebar__item.is-active::before {
  content: "";
  position: absolute;
  left: -16px;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 22px;
  border-radius: 0 4px 4px 0;
  background: #7BD0FF;
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
  padding: 16px 18px;
  border-radius: 24px;
}

.workspace__actions {
  display: flex;
  gap: 10px;
}

/* 右侧一组（天气 + 操作按钮）整体锚定最右，
   这样标题字数变化不会让中间的天气被 space-between 挤动 */
.workspace__header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.workspace__content {
  flex: 1;
  min-height: 0;            /* 允许 flex 子项压缩并独立滚动 */
  overflow-y: auto;         /* 只有右侧内容滚动，左侧面板与顶栏保持不动 */
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
  }
}

.weather-pill { display: flex; align-items: center; gap: 6px; padding: 6px 14px; border-radius: 10px; background: rgba(63,182,255,.06); border: 1px solid rgba(63,182,255,.1); }
.weather-pill__icon { font-size: 18px; }
.weather-pill__text { font-size: 12px; color: var(--text-secondary); white-space: nowrap; }
@media (max-width: 900px) { .weather-pill { display: none; } }

/* 顶栏操作按钮：小胶囊，与用户端「管理后台」成套 */
.workspace__actions .el-button {
  height: 34px;
  padding: 0 18px;
  border-radius: 999px;
  font-size: 13px;
  margin-left: 0;
}
.workspace__actions .el-button--default {
  color: #3E4C66;
  border-color: #E2ECFC;
  background: #fff;
}
.workspace__actions .el-button--default:hover {
  color: #1E98F2;
  border-color: #CBEBFF;
  background: #F4FAFF;
  box-shadow: none;
}
.workspace__actions .el-button--default:last-child:hover {
  color: #dc2626;
  border-color: #fda4af;
  background: #fff1f2;
}
</style>

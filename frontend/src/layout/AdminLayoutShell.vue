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
        {{ item.label }}
      </button>
    </aside>

    <div class="workspace">
      <header class="workspace__header glass-panel">
        <div>
          <div class="workspace__title">
            {{ titleMap[route.path] || '管理后台' }}
          </div>
        </div>
        <div class="workspace__header-right">
          <div
            v-if="weather"
            class="weather-pill"
          >
            <span class="weather-pill__icon">{{ weatherIcon(weather.weather1) }}</span>
            <span class="weather-pill__text">{{ weather.shi }} {{ weather.weather1 }} {{ weather.temp }}</span>
          </div>
          <div class="workspace__actions">
            <el-button
              plain
              @click="router.push('/dashboard')"
            >
              切到用户端
            </el-button>
            <el-button @click="logout">
              退出
            </el-button>
          </div>
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
  { label: '管理驾驶舱', path: '/admin' },
  { label: '服务治理', path: '/admin/services' },
  { label: '预约审核', path: '/admin/bookings' },
  { label: '用户与权限', path: '/admin/users' },
  { label: '系统设置', path: '/admin/system' },
  { label: '工具箱', path: '/admin/tools' },
  { label: 'AI 助手', path: '/assistant' },
]

const titleMap: Record<string, string> = {
  '/admin': '管理驾驶舱',
  '/admin/services': '服务治理',
  '/admin/bookings': '预约审核',
  '/admin/users': '用户与权限',
  '/admin/system': '系统设置',
  '/admin/tools': '工具箱',
}

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
  padding: 20px 16px;
  background: linear-gradient(180deg, #0f172a, #132949 52%, #10264e);
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
  background: linear-gradient(135deg, #4d95ff, #1458d4);
  font-weight: 800;
}

.sidebar__brand p {
  margin: 4px 0 0;
  color: rgba(255, 255, 255, 0.68);
  font-size: 12px;
}

.sidebar__item {
  width: 100%;
  margin-bottom: 10px;
  padding: 14px 16px;
  text-align: left;
  color: rgba(255, 255, 255, 0.82);
  border: 0;
  border-radius: 16px;
  background: transparent;
  cursor: pointer;
}

.sidebar__item.is-active {
  color: #fff;
  background: rgba(84, 160, 255, 0.18);
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

.workspace__title {
  font-size: 20px;
  font-weight: 700;
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

.weather-pill { display: flex; align-items: center; gap: 6px; padding: 6px 14px; border-radius: 10px; background: rgba(20,88,212,.06); border: 1px solid rgba(20,88,212,.1); }
.weather-pill__icon { font-size: 18px; }
.weather-pill__text { font-size: 12px; color: var(--text-secondary); white-space: nowrap; }
@media (max-width: 900px) { .weather-pill { display: none; } }
</style>

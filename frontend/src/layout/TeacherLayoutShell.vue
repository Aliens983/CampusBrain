<template>
  <div class="shell">
    <header class="shell__header glass-panel">
      <div
        class="brand"
        @click="router.push('/teacher/review')"
      >
        <div class="brand__mark">
          TEA
        </div>
        <span class="brand__title">教师工作台</span>
      </div>

      <nav class="nav">
        <button
          v-for="item in navItems"
          :key="item.path"
          class="nav__item"
          :class="{ 'is-active': route.path.startsWith(item.path) }"
          @click="router.push(item.path)"
        >
          {{ item.label }}
        </button>
      </nav>

      <div class="header-right">
        <el-dropdown
          class="user-dropdown"
          @command="handleCommand"
        >
          <span class="user-trigger">
            <el-avatar :size="34">{{ initial }}</el-avatar>
            <span class="user-name">{{ userStore.userInfo?.username || '教师' }}</span>
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
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/common/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const navItems = [
  { label: '待我审核', path: '/teacher/review' },
  { label: '我的咨询', path: '/teacher/consultations' },
  { label: '个人中心', path: '/teacher/profile' },
]

const initial = computed(() => userStore.userInfo?.username?.slice(0, 1) || 'T')

function handleCommand(command: string) {
  if (command === 'profile') {
    router.push('/teacher/profile')
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
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  color: #fff;
  font-weight: 800;
  font-size: 13px;
  background: linear-gradient(135deg, #1f6fb2, #3FB6FF);
}
.brand__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
}

.nav {
  display: flex;
  gap: 4px;
  flex: 1;
  justify-content: center;
}
.nav__item {
  padding: 8px 16px;
  border: 0;
  border-radius: 999px;
  font-size: 13px;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s, color 0.2s;
}
.nav__item:hover {
  background: rgba(63, 182, 255, 0.06);
  color: var(--brand-500);
}
.nav__item.is-active {
  color: #fff;
  background: linear-gradient(135deg, #3FB6FF, #ADE2FF);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
.user-dropdown {
  cursor: pointer;
}
.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
}
.user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shell__content {
  margin-top: 18px;
}

@media (max-width: 720px) {
  .shell__header {
    flex-wrap: wrap;
    gap: 10px;
  }
  .nav {
    order: 3;
    flex-basis: 100%;
    justify-content: flex-start;
  }
  .user-name {
    display: none;
  }
}
</style>

<template>
  <div class="page-shell">
    <ProfileHero
      :is-teacher="userStore.isTeacher"
      @navigate="go"
      @open-quick="quickVisible = true"
    />

    <ProfileInfoCards
      :user="user"
      :role-label="roleLabel"
      :role-tag-type="roleTagType"
    />

    <ActionCards
      :is-teacher="userStore.isTeacher"
      @open-preference="preferenceVisible = true"
      @open-password="passwordVisible = true"
      @navigate="go"
      @logout="handleLogout"
    />

    <!-- 通知偏好弹窗：点击「通知偏好」卡片打开 -->
    <PreferenceDialog
      v-model="preferenceVisible"
      v-model:email-on="prefs.emailOn"
      @save="savePrefs"
    />

    <!-- 修改密码弹窗：当前登录用户改自己的密码，PUT /users/password -->
    <PasswordDialog v-model="passwordVisible" />

    <QuickActionsDrawer
      v-model="quickVisible"
      :is-teacher="userStore.isTeacher"
      @navigate="go"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/common/stores/user'
import ProfileHero from './components/ProfileHero.vue'
import ProfileInfoCards from './components/ProfileInfoCards.vue'
import ActionCards from './components/ActionCards.vue'
import PreferenceDialog from './components/PreferenceDialog.vue'
import PasswordDialog from './components/PasswordDialog.vue'
import QuickActionsDrawer from './components/QuickActionsDrawer.vue'
import { roleLabelOf, roleTagTypeOf, useNotifyPrefs } from './composables'

const router = useRouter()
const userStore = useUserStore()

const preferenceVisible = ref(false)
const quickVisible = ref(false)
const passwordVisible = ref(false)

// 个人通知偏好（与后端 /users/me/notify 同步）
const { prefs, loadPrefs, savePrefs } = useNotifyPrefs()
onMounted(loadPrefs)

const user = computed(() => userStore.userInfo)
const roleLabel = computed(() => roleLabelOf(user.value?.role))
const roleTagType = computed(() => roleTagTypeOf(user.value?.role))

function go(to: string) {
  router.push(to)
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

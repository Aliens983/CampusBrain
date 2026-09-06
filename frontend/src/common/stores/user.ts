import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '@/common/types'
import { isAdminRole, isTeacherRole } from '@/common/utils/auth'

export const useUserStore = defineStore(
  'user',
  () => {
    const token = ref<string>('')
    const userInfo = ref<UserInfo | null>(null)

    const isLogin = computed(() => Boolean(token.value))
    const isAdmin = computed(() => isAdminRole(userInfo.value?.role))
    const isTeacher = computed(() => isTeacherRole(userInfo.value?.role))

    function setToken(value: string) {
      token.value = value
    }

    function setUserInfo(value: UserInfo) {
      userInfo.value = value
    }

    function logout() {
      token.value = ''
      userInfo.value = null
    }

    return {
      token,
      userInfo,
      isLogin,
      isAdmin,
      isTeacher,
      setToken,
      setUserInfo,
      logout,
    }
  },
  {
    persist: {
      key: 'enterprise_frontend_user',
      paths: ['token', 'userInfo'],
    },
  },
)
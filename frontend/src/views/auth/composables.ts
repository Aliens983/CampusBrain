import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormRules } from 'element-plus'
import request from '@/common/utils/request'
import { useUserStore } from '@/common/stores/user'
import { extractToken, resolveHomeByRole } from '@/common/utils/auth'
import { fetchUserProfile } from '@/common/campus'

export interface LoginFormState {
  email: string
  password: string
  captcha: string
}

export interface ResetFormState {
  email: string
  code: string
  password: string
  confirmPassword: string
}

/**
 * 校验登录回跳地址（7.3.11）：只接受站内单斜杠路径，
 * 拒绝 //host 协议相对 URL 与外链，避免开放重定向。
 */
function safeRedirect(redirect: unknown): string | null {
  if (typeof redirect !== 'string' || !redirect.startsWith('/') || redirect.startsWith('//')) {
    return null
  }
  return redirect
}

export function useAuthPage() {
  const router = useRouter()
  const userStore = useUserStore()
  const loading = ref(false)
  const sendingCode = ref(false)
  const captchaImage = ref('')
  const captchaUuid = ref('')
  const resetMode = ref(false)

  const loginForm = reactive<LoginFormState>({
    email: '',
    password: '',
    captcha: '',
  })

  const resetForm = reactive<ResetFormState>({
    email: '',
    code: '',
    password: '',
    confirmPassword: '',
  })

  const loginRules: FormRules = {
    email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
    captcha: [{ required: true, message: '请输入图形验证码', trigger: 'blur' }],
  }

  const resetRules: FormRules = {
    email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
    code: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
    password: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
    confirmPassword: [{ required: true, message: '请再次输入新密码', trigger: 'blur' }],
  }

  async function refreshCaptcha() {
    try {
      const response = await request.get('/captcha') as unknown as { uuid: string; imageUrl: string }
      // uuid 需随验证码答案一起提交；图形验证码一次性，提交后即失效
      captchaUuid.value = response.uuid
      // 后端返回 http://localhost:18080/api/v1/uploads/xxx.png
      // 转为走Vite代理的路径 /api/uploads/xxx.png
      const path = new URL(response.imageUrl).pathname.replace('/api/v1', '')
      captchaImage.value = '/api' + path
    } catch {
      captchaUuid.value = ''
      captchaImage.value = ''
      ElMessage.warning('验证码加载失败，点击刷新重试')
    }
  }

  async function sendResetCode() {
    if (!resetForm.email) {
      ElMessage.warning('请先输入邮箱')
      return
    }
    sendingCode.value = true
    try {
      await request.post('/auth/verification-code', { to: resetForm.email })
      ElMessage.success('验证码已发送')
    } catch (error: unknown) {
      const err = error as { message?: string }
      ElMessage.error(err.message || '验证码发送失败')
    } finally {
      sendingCode.value = false
    }
  }

  // 表单校验由子组件在提交前完成，校验通过后触发 submit 事件进入本流程
  async function handleLogin() {
    loading.value = true

    try {
      const loginResult = await request.post<string>('/auth/login', {
        email: loginForm.email,
        password: loginForm.password,
        captchaUuid: captchaUuid.value,
        captchaCode: loginForm.captcha,
      })

      const token = extractToken(loginResult) || String(loginResult || '')
      userStore.setToken(token)
      userStore.setUserInfo(await fetchUserProfile())
      ElMessage.success('登录成功')
      // 7.3.11：优先回跳登录前被守卫拦下的目标页；query 缺失/非法时回角色首页
      router.push(safeRedirect(router.currentRoute.value.query.redirect)
        || resolveHomeByRole(userStore.userInfo?.role))
    } catch (error: unknown) {
      const err = error as { message?: string }
      ElMessage.error(err.message || '登录失败，请检查账号和密码')
      // 图形验证码一次性：提交后即失效，登录失败需重新拉取并清空已填答案
      loginForm.captcha = ''
      await refreshCaptcha()
    } finally {
      loading.value = false
    }
  }

  async function handleReset() {
    if (resetForm.password !== resetForm.confirmPassword) {
      ElMessage.warning('两次密码输入不一致')
      return
    }

    loading.value = true
    try {
      await request.post('/auth/reset', {
        email: resetForm.email,
        code: resetForm.code,
        password: resetForm.password,
      })
      ElMessage.success('密码重置成功，请使用新密码登录')
      resetForm.email = ''
      resetForm.code = ''
      resetForm.password = ''
      resetForm.confirmPassword = ''
      resetMode.value = false
    } catch (error: unknown) {
      const err = error as { message?: string }
      ElMessage.error(err.message || '密码重置失败，请稍后重试')
    } finally {
      loading.value = false
    }
  }

  function enterResetMode() {
    resetMode.value = true
  }

  function backToLogin() {
    resetMode.value = false
  }

  function goRegister() {
    router.push('/register')
  }

  onMounted(() => {
    void refreshCaptcha()
  })

  return {
    resetMode,
    loginForm,
    loginRules,
    loading,
    captchaImage,
    refreshCaptcha,
    handleLogin,
    resetForm,
    resetRules,
    sendingCode,
    sendResetCode,
    handleReset,
    enterResetMode,
    backToLogin,
    goRegister,
  }
}

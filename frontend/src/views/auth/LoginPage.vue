<template>
  <div class="auth-page">
    <div class="auth-shell">
      <section class="auth-hero">
        <div class="auth-hero__deco auth-hero__deco--1" />
        <div class="auth-hero__deco auth-hero__deco--2" />
        <div class="auth-hero__brand">
          <span class="auth-hero__logo">C</span>
          <span>CampusBrain 智汇校园</span>
        </div>
        <span class="status-pill is-brand auth-hero__pill">校园预约门户</span>
        <h1>校园预约系统</h1>
        <p>{{ resetMode ? '通过邮箱验证码重置您的密码。' : '统一提供空间、设备与咨询等预约服务。' }}</p>
        <ul class="auth-hero__points">
          <li>两校区服务，按仓前 / 下沙分流预约</li>
          <li>咨询、教室、设备、活动一站式办理</li>
          <li>知识库 AI 助手，随时解答预约问题</li>
        </ul>
      </section>

      <!-- 登录表单 -->
      <section
        v-if="!resetMode"
        class="auth-card"
      >
        <div class="auth-card__head">
          <h2>登录</h2>
          <p>使用注册邮箱登录，验证通过后进入系统。</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          size="large"
        >
          <el-form-item prop="email">
            <el-input
              v-model="loginForm.email"
              placeholder="邮箱"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              show-password
              placeholder="密码"
            />
          </el-form-item>
          <el-form-item prop="captcha">
            <div class="captcha-row">
              <el-input
                v-model="loginForm.captcha"
                placeholder="图形验证码"
              />
              <div
                class="captcha-box"
                @click="refreshCaptcha"
              >
                <img
                  v-if="captchaImage"
                  :src="captchaImage"
                  alt="captcha"
                >
                <span v-else>获取验证码</span>
              </div>
            </div>
          </el-form-item>

          <el-button
            type="primary"
            class="submit-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登录并进入系统
          </el-button>

          <div class="auth-footer">
            <el-link
              type="primary"
              @click="resetMode = true"
            >
              忘记密码？
            </el-link>
            <el-link
              type="primary"
              @click="router.push('/register')"
            >
              去注册
            </el-link>
          </div>
        </el-form>
      </section>

      <!-- 重置密码表单 -->
      <section
        v-else
        class="auth-card"
      >
        <div class="auth-card__head">
          <h2>重置密码</h2>
          <p>输入邮箱获取验证码，设置新密码后登录。</p>
        </div>

        <el-form
          ref="resetFormRef"
          :model="resetForm"
          :rules="resetRules"
          size="large"
        >
          <el-form-item prop="email">
            <el-input
              v-model="resetForm.email"
              placeholder="邮箱"
            />
          </el-form-item>
          <el-form-item prop="code">
            <div
              class="captcha-row"
              style="grid-template-columns: 1fr auto"
            >
              <el-input
                v-model="resetForm.code"
                placeholder="邮箱验证码"
              />
              <el-button
                :loading="sendingCode"
                @click="sendResetCode"
              >
                发送验证码
              </el-button>
            </div>
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="resetForm.password"
              type="password"
              show-password
              placeholder="新密码"
            />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="resetForm.confirmPassword"
              type="password"
              show-password
              placeholder="确认新密码"
            />
          </el-form-item>

          <el-button
            type="primary"
            class="submit-btn"
            :loading="loading"
            @click="handleReset"
          >
            确认重置
          </el-button>

          <div class="auth-footer">
            <span />
            <el-link
              type="primary"
              @click="resetMode = false"
            >
              返回登录
            </el-link>
          </div>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import request from '@/common/utils/request'
import { useUserStore } from '@/common/stores/user'
import { extractToken, resolveHomeByRole } from '@/utils/auth'
import { loadUserProfile } from '@/services/portal'

const router = useRouter()
const userStore = useUserStore()
const loginFormRef = ref<FormInstance>()
const resetFormRef = ref<FormInstance>()
const loading = ref(false)
const sendingCode = ref(false)
const captchaImage = ref('')
const captchaUuid = ref('')
const resetMode = ref(false)

const loginForm = reactive({
  email: '',
  password: '',
  captcha: '',
})

const resetForm = reactive({
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

async function handleLogin() {
  await loginFormRef.value?.validate()
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
    userStore.setUserInfo(await loadUserProfile())
    ElMessage.success('登录成功')
    router.push(resolveHomeByRole(userStore.userInfo?.role))
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
  await resetFormRef.value?.validate()
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

onMounted(() => {
  void refreshCaptcha()
})
</script>

<style scoped lang="scss">
.auth-page {
  min-height: 100vh;
  padding: 28px;
  display: grid;
  place-items: center;
}

.auth-shell {
  position: relative;
  width: min(1100px, 100%);
  display: grid;
  grid-template-columns: 1fr 460px;
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 0 28px 70px rgba(16, 24, 40, 0.18);
  animation: authIn 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes authIn {
  from { opacity: 0; transform: translateY(18px) scale(0.985); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.auth-hero {
  position: relative;
  padding: 44px;
  color: #fff;
  overflow: hidden;
  background:
    radial-gradient(circle at 82% 8%, rgba(255, 255, 255, 0.16), transparent 42%),
    linear-gradient(145deg, #0E6CD6, #3FB6FF 56%, #7BD0FF);
}

.auth-hero::after {
  content: "";
  position: absolute;
  inset: auto -90px -110px auto;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.22), rgba(255, 255, 255, 0));
  pointer-events: none;
}

.auth-hero__deco {
  position: absolute;
  border-radius: 22px;
  border: 1.5px solid rgba(255, 255, 255, 0.28);
  pointer-events: none;
}
.auth-hero__deco--1 {
  top: 42px;
  right: 48px;
  width: 84px;
  height: 84px;
  transform: rotate(18deg);
  animation: decoFloatA 9s ease-in-out infinite;
}
.auth-hero__deco--2 {
  bottom: 70px;
  right: 110px;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  animation: decoFloatB 7s ease-in-out infinite;
}

@keyframes decoFloatA {
  0%, 100% { transform: rotate(18deg) translateY(0); }
  50% { transform: rotate(26deg) translateY(-12px); }
}
@keyframes decoFloatB {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-14px); }
}

.auth-hero__brand {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: rgba(255, 255, 255, 0.92);
}

.auth-hero__logo {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  font-size: 16px;
  font-weight: 800;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.35);
}

.auth-hero__pill {
  position: relative;
  z-index: 1;
  display: inline-flex;
  margin-top: 40px;
  color: #fff;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.28);
}

.auth-hero h1 {
  position: relative;
  z-index: 1;
  margin: 18px 0 12px;
  font-size: 44px;
}

.auth-hero p {
  position: relative;
  z-index: 1;
  margin: 0;
  max-width: 520px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.88);
}

.auth-hero__points {
  position: relative;
  z-index: 1;
  margin: 30px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 12px;
}

.auth-hero__points li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13.5px;
  color: rgba(255, 255, 255, 0.92);
}

.auth-hero__points li::before {
  content: "";
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.18);
}

.auth-card {
  padding: 40px;
  background: rgba(255, 255, 255, 0.98);
}

.auth-card__head h2 {
  margin: 0 0 8px;
  font-size: 24px;
}

.auth-card__head p {
  margin: 0 0 24px;
  color: var(--text-secondary);
}

.captcha-row {
  display: grid;
  grid-template-columns: 1fr 120px;
  gap: 10px;
  width: 100%;
}

.captcha-box {
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  border: 1px solid var(--border-soft);
  background: #fff;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.captcha-box:hover {
  border-color: var(--brand-500);
  box-shadow: 0 0 0 3px rgba(63, 182, 255, 0.12);
}

.captcha-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.submit-btn {
  width: 100%;
  margin-top: 4px;
}

.auth-footer {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
}

@media (max-width: 960px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }
  .auth-hero {
    padding: 32px;
  }
  .auth-hero__points {
    display: none;
  }
  .auth-hero h1 {
    font-size: 32px;
  }
}
</style>

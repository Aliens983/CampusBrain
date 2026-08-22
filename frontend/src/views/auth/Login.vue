<template>
  <div class="auth-page">
    <div class="auth-shell">
      <section class="auth-hero">
        <span class="status-pill is-brand">登录后按身份自动分流</span>
        <h1>校园预约系统</h1>
        <p>普通用户进入用户端，管理员与超级管理员进入管理端。前端会在登录成功后继续查询当前用户身份并自动跳转。</p>
        <div class="hero-grid">
          <div>
            <strong>用户端</strong>
            <span>预约、查询、消息、个人资料</span>
          </div>
          <div>
            <strong>管理端</strong>
            <span>服务治理、审核、权限、系统设置</span>
          </div>
        </div>
      </section>

      <section class="auth-card">
        <div class="auth-card__head">
          <h2>登录</h2>
          <p>安全可靠的校园统一身份认证入口，登录后按角色自动分流至对应工作台。</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
        >
          <el-form-item prop="email">
            <el-input
              v-model="form.email"
              placeholder="邮箱"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="密码"
            />
          </el-form-item>
          <el-form-item prop="captcha">
            <div class="captcha-row">
              <el-input
                v-model="form.captcha"
                placeholder="验证码"
              />
              <div
                class="captcha-image-wrapper"
                @click="refreshCaptcha"
              >
                <img
                  v-if="captchaImage"
                  :src="captchaImage"
                  alt="验证码"
                  class="captcha-image"
                >
                <el-button
                  v-else
                  plain
                >
                  获取验证码
                </el-button>
              </div>
            </div>
          </el-form-item>

          <div class="auth-row">
            <el-checkbox v-model="form.remember">
              记住登录状态
            </el-checkbox>
            <el-link type="primary">
              忘记密码
            </el-link>
          </div>

          <el-button
            type="primary"
            class="submit-btn"
            :loading="loading"
            @click="submit"
          >
            登录并进入系统
          </el-button>

          <div class="auth-footer">
            <span>还没有账号？</span>
            <el-link
              type="primary"
              @click="router.push('/register')"
            >
              立即注册
            </el-link>
          </div>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/common/stores/user'
import { userService } from '@/services'
import { userAPI } from '@/services/api'
import { extractToken, normalizeUserInfo, resolveHomeByRole } from '@/utils/auth'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaImage = ref<string>('')

const form = reactive({
  email: '',
  password: '',
  captcha: '',
  remember: true,
})

const rules: FormRules = {
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

// 刷新验证码
async function refreshCaptcha() {
  try {
    const response = await userAPI.getGraphicCaptcha() as unknown as { uuid: string; imageUrl: string }
    // 后端返回 http://localhost:18080/api/v1/uploads/xxx.png
    // 转为走Vite代理的路径 /api/uploads/xxx.png
    const path = new URL(response.imageUrl).pathname.replace('/api/v1', '')
    captchaImage.value = '/api' + path
  } catch (error) {
    console.error('获取验证码失败:', error)
    ElMessage.error('获取验证码失败，请重试')
    captchaImage.value = ''
  }
}

async function submit() {
  await formRef.value?.validate()
  loading.value = true

  try {
    const loginResult = await userService.login(form.email, form.password, form.captcha)
    const token = extractToken(loginResult)
    if (token) userStore.setToken(token)

    const infoPayload = await userService.getInfo()
    const normalized = normalizeUserInfo(infoPayload)
    userStore.setUserInfo(normalized)
    ElMessage.success(`登录成功，已进入${userStore.isAdmin ? '管理端' : '用户端'}`)
    router.push(resolveHomeByRole(normalized.role))
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '登录失败，请检查账号和密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.auth-page {
  min-height: 100vh;
  padding: 28px;
  display: grid;
  place-items: center;
}

.auth-shell {
  width: min(1180px, 100%);
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  overflow: hidden;
  border-radius: 32px;
  box-shadow: 0 28px 70px rgba(16, 24, 40, 0.16);
  animation: authReveal 0.85s cubic-bezier(0.22, 1, 0.36, 1);
}

.auth-hero {
  position: relative;
  padding: 48px;
  color: #fff;
  background: linear-gradient(145deg, #0b1e3a, #1458d4 56%, #58a4ff);
  overflow: hidden;
}

.auth-hero::after {
  content: '';
  position: absolute;
  inset: auto -12% -28% auto;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.24), rgba(255, 255, 255, 0));
  animation: authOrb 8.4s ease-in-out infinite;
}

.auth-hero h1 {
  margin: 18px 0 12px;
  font-size: 46px;
  line-height: 1.12;
}

.auth-hero p {
  max-width: 560px;
  margin: 0;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.84);
}

.hero-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
  margin-top: 34px;
}

.hero-grid div {
  display: grid;
  gap: 8px;
  padding: 20px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.12);
  transition: transform 0.26s ease, background-color 0.26s ease;
}

.hero-grid div:hover {
  transform: translateY(-4px);
  background: rgba(255, 255, 255, 0.18);
}

.hero-grid strong {
  font-size: 20px;
}

.hero-grid span {
  color: rgba(255, 255, 255, 0.76);
  line-height: 1.6;
}

.auth-card {
  padding: 42px;
  background: rgba(255, 255, 255, 0.98);
  position: relative;
}

.auth-card__head h2 {
  margin: 0 0 8px;
  font-size: 30px;
}

.auth-card__head p {
  margin: 0 0 26px;
  color: var(--text-secondary);
}

.captcha-row {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 108px;
  gap: 10px;
}

.captcha-image-wrapper {
  width: 100%;
  height: 40px;
  border-radius: 6px;
  border: 1px solid var(--el-border-color);
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  
  &:hover {
    border-color: var(--el-color-primary);
    box-shadow: 0 0 0 2px rgba(var(--el-color-primary), 0.1);
  }
  
  .captcha-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
  
  el-button {
    width: 100%;
    height: 100%;
    border-radius: 6px;
  }
}

.auth-row,
.auth-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.auth-row {
  margin-bottom: 18px;
}

.submit-btn {
  width: 100%;
  height: 48px;
}

.hint-box {
  margin: 18px 0;
  padding: 14px 16px;
  display: grid;
  gap: 6px;
  border-radius: 18px;
  background: #f3f7ff;
  color: var(--text-secondary);
  font-size: 13px;
  transition: transform 0.24s ease, box-shadow 0.24s ease;
}

.hint-box:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 26px rgba(20, 33, 61, 0.08);
}

.hint-box strong {
  color: var(--text-primary);
}

@keyframes authReveal {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.985);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes authOrb {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(-26px, 18px, 0) scale(1.1);
  }
}

@media (max-width: 980px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .hero-grid {
    grid-template-columns: 1fr;
  }
}
</style>

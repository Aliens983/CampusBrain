<template>
  <div class="register-page">
    <div class="register-card">
      <header class="register-hero">
        <img
          class="register-hero__photo"
          :src="cqImg"
          alt="杭州师范大学仓前校区校门"
        >
        <div class="register-hero__veil" />
        <div class="register-hero__brand">
          <span class="register-hero__crest">
            <img
              :src="hznuCrest"
              alt="杭州师范大学校徽"
            >
          </span>
          <span class="register-hero__brand-text">
            <strong>杭州师范大学</strong>
            <span>智汇校园 · HZNU</span>
          </span>
        </div>
        <img
          class="register-hero__motto"
          :src="hznuMottoLight"
          alt="校训：勤慎诚恕，博雅精进"
        >
        <div class="register-hero__copy">
          <p class="register-hero__eyebrow">
            Create account · HZNU
          </p>
          <h1>加入智汇校园</h1>
          <p>用邮箱验证码完成注册；注册后即可预约空间、设备与咨询服务。</p>
        </div>
      </header>

      <div class="register-body">
        <div class="register-body__head">
          <span class="register-body__index">01</span>
          <div>
            <h2>填写校园身份</h2>
            <p>信息仅用于校园预约与通知，请使用真实邮箱。</p>
          </div>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
        >
          <el-row :gutter="18">
            <el-col
              :md="12"
              :xs="24"
            >
              <el-form-item
                label="姓名"
                prop="name"
              >
                <el-input
                  v-model="form.name"
                  placeholder="请输入真实姓名"
                />
              </el-form-item>
            </el-col>
            <el-col
              :md="12"
              :xs="24"
            >
              <el-form-item
                label="邮箱"
                prop="email"
              >
                <el-input
                  v-model="form.email"
                  placeholder="name@example.com"
                />
              </el-form-item>
            </el-col>
            <el-col
              :md="12"
              :xs="24"
            >
              <el-form-item
                label="图形验证码"
                prop="captcha"
              >
                <div class="captcha-row">
                  <el-input
                    v-model="form.captcha"
                    placeholder="输入右侧字符"
                  />
                  <div
                    class="captcha-box"
                    title="点击刷新验证码"
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
            </el-col>
            <el-col
              :md="12"
              :xs="24"
            >
              <el-form-item
                label="年级 / 部门"
                prop="grade"
              >
                <el-input
                  v-model="form.grade"
                  placeholder="如：2024 级 / 信息学院"
                />
              </el-form-item>
            </el-col>
            <el-col
              :md="12"
              :xs="24"
            >
              <el-form-item
                label="邮箱验证码"
                prop="code"
              >
                <div class="verify-row">
                  <el-input
                    v-model="form.code"
                    placeholder="6 位验证码"
                  />
                  <el-button
                    :loading="sending"
                    @click="sendCode"
                  >
                    发送验证码
                  </el-button>
                </div>
              </el-form-item>
            </el-col>
            <el-col
              :md="12"
              :xs="24"
            >
              <el-form-item
                label="密码"
                prop="password"
              >
                <el-input
                  v-model="form.password"
                  type="password"
                  show-password
                  placeholder="设置登录密码"
                />
              </el-form-item>
            </el-col>
            <el-col
              :md="12"
              :xs="24"
            >
              <el-form-item
                label="确认密码"
                prop="confirmPassword"
              >
                <el-input
                  v-model="form.confirmPassword"
                  type="password"
                  show-password
                  placeholder="再次输入密码"
                />
              </el-form-item>
            </el-col>
            <el-col
              :md="12"
              :xs="24"
            >
              <div class="register-note">
                <span class="register-note__mark">注</span>
                <p>注册默认为学生账号；教师与管理员权限由学校管理员在后台分配。</p>
              </div>
            </el-col>
          </el-row>
        </el-form>

        <div class="actions">
          <el-button @click="router.push('/login')">
            返回登录
          </el-button>
          <el-button
            type="primary"
            :loading="loading"
            @click="submit"
          >
            提交注册
          </el-button>
        </div>

        <div class="register-foot">
          <div class="register-foot__brand">
            <img
              :src="hznuLogo"
              alt="杭州师范大学"
              class="register-foot__logo"
            >
            <span>勤慎诚恕 · 博雅精进</span>
          </div>
          <span>HZNU · EST. 1908</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import request from '@/common/utils/request'
import cqImg from '@/assets/images/campus/cq-hero.jpg'
import hznuCrest from '@/assets/images/brand/hznu-crest.png'
import hznuMottoLight from '@/assets/images/brand/hznu-motto-light.png'
import hznuLogo from '@/assets/images/brand/hznu-logo.png'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const sending = ref(false)
const captchaImage = ref('')

const form = reactive({
  name: '',
  email: '',
  grade: '',
  code: '',
  password: '',
  confirmPassword: '',
  captcha: '',
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  grade: [{ required: true, message: '请输入年级或部门', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请再次输入密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入图形验证码', trigger: 'blur' }],
}

async function sendCode() {
  if (!form.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  sending.value = true
  try {
    await request.post('/auth/verification-code', { to: form.email })
    ElMessage.success('验证码已发送')
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '验证码发送失败')
  } finally {
    sending.value = false
  }
}

async function refreshCaptcha() {
  try {
    const response = await request.get('/captcha') as unknown as { uuid: string; imageUrl: string }
    // 验证码图片使用与文件服务一致的同源 /uploads 路径。
    const path = new URL(response.imageUrl).pathname.replace('/api/v1', '')
    captchaImage.value = path
  } catch {
    captchaImage.value = ''
    ElMessage.warning('验证码加载失败，点击刷新重试')
  }
}

async function submit() {
  await formRef.value?.validate()
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }

  loading.value = true
  try {
    await request.post('/auth/register', {
      name: form.name,
      grade: form.grade,
      email: form.email,
      code: form.code,
      password: form.password,
      role: 0,
    })
    ElMessage.success('注册成功，请返回登录')
    router.push('/login')
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void refreshCaptcha()
})
</script>

<style scoped lang="scss">
.register-page {
  position: relative;
  min-height: 100vh;
  padding: 32px;
  display: grid;
  place-items: center;
  overflow: hidden;
}

.register-page::before {
  content: "1908";
  position: absolute;
  left: -30px;
  bottom: -110px;
  font-family: var(--font-latin);
  font-size: clamp(200px, 26vw, 360px);
  line-height: 1;
  letter-spacing: 0.04em;
  color: rgba(24, 49, 59, 0.035);
  pointer-events: none;
  user-select: none;
}

.register-card {
  position: relative;
  z-index: 1;
  width: min(980px, 100%);
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid var(--line);
  background: var(--surface);
  box-shadow: 0 44px 96px -54px rgba(24, 49, 59, 0.6);
  animation: registerIn 0.42s var(--ease-out) both;
}

@keyframes registerIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.register-hero {
  position: relative;
  min-height: 238px;
  padding: 30px 38px;
  display: flex;
  flex-direction: column;
  color: #f7f1e4;
  overflow: hidden;
  isolation: isolate;
}

.register-hero__photo {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 52%;
  z-index: -2;
  filter: saturate(0.78) contrast(1.02);
}

.register-hero__veil {
  position: absolute;
  inset: 0;
  z-index: -1;
  background:
    linear-gradient(95deg, rgba(13, 32, 39, 0.9) 0%, rgba(15, 38, 46, 0.62) 52%, rgba(15, 38, 46, 0.2) 100%),
    linear-gradient(0deg, rgba(13, 32, 39, 0.58), transparent 55%);
}

.register-hero__brand {
  display: flex;
  align-items: center;
  gap: 11px;
}

.register-hero__seal {
  display: inline-grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border: 1.5px solid rgba(214, 193, 154, 0.72);
  border-radius: 3px;
  color: var(--gold-soft);
  background: rgba(247, 241, 228, 0.06);
  box-shadow: inset 0 0 0 3px rgba(13, 32, 39, 0.5), inset 0 0 0 4px rgba(214, 193, 154, 0.3);
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
}

.register-hero__crest {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
}

.register-hero__crest img {
  width: 38px;
  height: 38px;
  object-fit: contain;
  filter: brightness(0) invert(1);
  opacity: 0.88;
}

.register-hero__brand-text {
  display: grid;
  line-height: 1.2;
}

.register-hero__brand-text strong {
  font-family: var(--font-serif);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.14em;
}

.register-hero__brand-text span {
  margin-top: 3px;
  font-family: var(--font-latin);
  font-size: 8px;
  font-weight: 600;
  letter-spacing: 0.18em;
  color: rgba(247, 241, 228, 0.52);
}

.register-hero__motto {
  position: absolute;
  top: 50%;
  right: 16px;
  width: auto;
  height: 160px;
  transform: translateY(-50%);
  opacity: 0.62;
  filter: drop-shadow(0 8px 24px rgba(13, 32, 39, 0.3));
  pointer-events: none;
}

.register-hero__copy {
  margin-top: auto;
  padding-top: 34px;
}

.register-hero__eyebrow {
  margin: 0 0 8px;
  font-family: var(--font-latin);
  font-size: 9.5px;
  font-weight: 600;
  letter-spacing: 0.22em;
  color: var(--gold-soft);
}

.register-hero h1 {
  margin: 0 0 8px;
  font-family: var(--font-serif);
  font-size: 32px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: #f8f2e6;
}

.register-hero p {
  margin: 0;
  max-width: 460px;
  font-size: 12.5px;
  line-height: 1.8;
  color: rgba(247, 241, 228, 0.72);
}

.register-body {
  padding: 32px 38px 28px;
}

.register-body__head {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 24px;
}

.register-body__index {
  font-family: var(--font-latin);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.12em;
  color: var(--gold);
  padding-top: 5px;
}

.register-body__head h2 {
  margin: 0 0 4px;
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 600;
  color: var(--ink);
}

.register-body__head p {
  margin: 0;
  font-size: 12px;
  color: var(--ink-3);
}

.register-body :deep(.el-form-item) {
  margin-bottom: 16px;
}

.register-body :deep(.el-form-item__label) {
  padding-bottom: 5px;
  font-size: 11.5px;
  letter-spacing: 0.06em;
  color: var(--ink-3);
}

.register-body :deep(.el-input__wrapper) {
  min-height: 42px;
  background: var(--surface-2);
}

.verify-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  width: 100%;
}

.captcha-row {
  display: grid;
  grid-template-columns: 1fr 122px;
  gap: 10px;
  width: 100%;
}

.captcha-box {
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: var(--surface-2);
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.16s ease;
}

.captcha-box:hover {
  border-color: rgba(176, 138, 69, 0.5);
}

.captcha-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-box span {
  font-size: 11px;
  color: var(--ink-4);
}

.register-note {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-height: 42px;
  margin-top: 27px;
  padding: 12px 14px;
  border: 1px dashed var(--line-strong);
  border-radius: 8px;
  background: var(--surface-2);
}

.register-note__mark {
  display: inline-grid;
  place-items: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  border: 1px solid rgba(176, 138, 69, 0.45);
  border-radius: 2px;
  font-family: var(--font-serif);
  font-size: 10px;
  color: var(--gold);
}

.register-note p {
  margin: 0;
  font-size: 11px;
  line-height: 1.7;
  color: var(--ink-3);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 10px;
}

.register-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 26px;
  padding-top: 18px;
  border-top: 1px solid var(--line);
  font-family: var(--font-latin);
  font-size: 8.5px;
  letter-spacing: 0.16em;
  color: var(--ink-4);
}

.register-foot span:first-child {
  font-family: var(--font-serif);
  font-size: 10px;
  letter-spacing: 0.12em;
}

.register-foot__brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.register-foot__logo {
  width: 84px;
  height: auto;
  opacity: 0.72;
}

@media (max-width: 700px) {
  .register-page {
    padding: 12px;
  }

  .register-card {
    border-radius: 14px;
  }

  .register-hero {
    padding: 22px 24px;
    min-height: 210px;
  }

  .register-hero__motto {
    display: none;
  }

  .register-hero h1 {
    font-size: 26px;
  }

  .register-body {
    padding: 24px 22px 22px;
  }

  .register-foot {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }
}
</style>

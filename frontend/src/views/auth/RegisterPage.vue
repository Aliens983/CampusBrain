<template>
  <div class="auth-page">
    <div class="register-card">
      <div class="auth-head">
        <span class="status-pill is-success">安全注册</span>
        <h1>创建校园预约账号</h1>
        <p>使用邮箱验证码完成注册；注册后即可预约空间、设备与咨询服务。</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
      >
        <el-row :gutter="16">
          <el-col
            :md="12"
            :xs="24"
          >
            <el-form-item
              label="姓名"
              prop="name"
            >
              <el-input v-model="form.name" />
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
              <el-input v-model="form.email" />
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
          </el-col>
          <el-col
            :md="12"
            :xs="24"
          >
            <el-form-item
              label="年级/部门"
              prop="grade"
            >
              <el-input v-model="form.grade" />
            </el-form-item>
          </el-col>
          <el-col
            :md="12"
            :xs="24"
          >
            <el-form-item
              label="验证码"
              prop="code"
            >
              <div class="verify-row">
                <el-input v-model="form.code" />
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
              />
            </el-form-item>
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import request from '@/common/utils/request'

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
    // 后端返回 http://localhost:18080/api/v1/uploads/xxx.png
    // 转为走Vite代理的路径 /api/uploads/xxx.png
    const path = new URL(response.imageUrl).pathname.replace('/api/v1', '')
    captchaImage.value = '/api' + path
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
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 28px;
}

.register-card {
  width: min(920px, 100%);
  padding: 40px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 24px 60px rgba(16, 24, 40, 0.12);
}

.auth-head {
  margin-bottom: 22px;
}

.auth-head h1 {
  margin: 14px 0 8px;
  font-size: 34px;
}

.auth-head p {
  margin: 0;
  color: var(--text-secondary);
}

.verify-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  width: 100%;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
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
  border: 1px solid var(--border-soft, #e0e0e0);
  background: #fff;
  overflow: hidden;
  cursor: pointer;
}

.captcha-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>

<template>
  <div class="auth-page">
    <div class="register-card">
      <div class="auth-form__head">
        <span class="status-pill is-success">注册后自动判断身份</span>
        <h1>创建校园预约账号</h1>
        <p>如果注册接口返回登录态，则前端会继续查询当前用户身份并自动进入用户端或管理端。</p>
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
              label="用户名"
              prop="username"
            >
              <el-input v-model="form.username" />
            </el-form-item>
          </el-col>
          <el-col
            :md="12"
            :xs="24"
          >
            <el-form-item
              label="手机号"
              prop="phone"
            >
              <el-input v-model="form.phone" />
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
              label="所属部门"
              prop="department"
            >
              <el-input v-model="form.department" />
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

      <div class="footer-actions">
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
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { userService } from '@/services'
import { useUserStore } from '@/common/stores/user'
import { extractToken, normalizeUserInfo, resolveHomeByRole } from '@/utils/auth'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  phone: '',
  email: '',
  department: '',
  password: '',
  confirmPassword: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  department: [{ required: true, message: '请输入所属部门', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请再次输入密码', trigger: 'blur' }],
}

async function submit() {
  await formRef.value?.validate()
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }

  loading.value = true
  try {
    const registerResult = await userService.register(form.username, form.email, form.password, '')
    const token = extractToken(registerResult)

    if (token) {
      userStore.setToken(token)
      const infoPayload = await (userService as any).getInfo?.().catch(() => registerResult)
      const normalized = normalizeUserInfo(infoPayload)
      userStore.setUserInfo({
        ...normalized,
        phone: normalized.phone || form.phone,
        department: normalized.department || form.department,
      })
      ElMessage.success('注册并登录成功')
      router.push(resolveHomeByRole(normalized.role))
      return
    }

    ElMessage.success('注册成功，请登录后进入对应端')
    router.push('/login')
  } catch {
    ElMessage.error('注册失败，请检查注册接口或表单内容')
  } finally {
    loading.value = false
  }
}
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
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 24px 60px rgba(16, 24, 40, 0.12);
}

.auth-form__head {
  margin-bottom: 22px;
}

.auth-form__head h1 {
  margin: 14px 0 8px;
  font-size: 34px;
}

.auth-form__head p {
  margin: 0;
  color: var(--text-secondary);
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
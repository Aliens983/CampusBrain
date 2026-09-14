<template>
  <section class="auth-card">
    <div class="auth-card__head">
      <h2>登录</h2>
      <p>使用注册邮箱登录，验证通过后进入系统。</p>
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
            placeholder="图形验证码"
          />
          <div
            class="captcha-box"
            @click="emit('refresh-captcha')"
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
        @click="handleSubmit"
      >
        登录并进入系统
      </el-button>

      <div class="auth-footer">
        <el-link
          type="primary"
          @click="emit('forgot-password')"
        >
          忘记密码？
        </el-link>
        <el-link
          type="primary"
          @click="emit('go-register')"
        >
          去注册
        </el-link>
      </div>
    </el-form>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { LoginFormState } from '../composables'

defineProps<{
  form: LoginFormState
  rules: FormRules
  loading: boolean
  captchaImage: string
}>()

const emit = defineEmits<{
  submit: []
  'refresh-captcha': []
  'forgot-password': []
  'go-register': []
}>()

const formRef = ref<FormInstance>()

async function handleSubmit() {
  await formRef.value?.validate()
  emit('submit')
}
</script>

<style scoped lang="scss">
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
</style>

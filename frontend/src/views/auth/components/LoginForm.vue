<template>
  <section class="auth-card">
    <div class="auth-card__head">
      <p class="hz-eyebrow">
        Sign in · HZNU
      </p>
      <h2>回到校园</h2>
      <p class="auth-card__lede">
        使用注册邮箱登录，验证通过后进入系统。
      </p>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      size="large"
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
      <el-form-item
        label="密码"
        prop="password"
      >
        <el-input
          v-model="form.password"
          type="password"
          show-password
          placeholder="请输入登录密码"
        />
      </el-form-item>
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

      <div class="auth-card__links">
        <el-link
          :underline="false"
          @click="emit('forgot-password')"
        >
          忘记密码？
        </el-link>
      </div>

      <el-button
        type="primary"
        class="submit-btn"
        :loading="loading"
        @click="handleSubmit"
      >
        登录并进入系统
      </el-button>

      <div class="auth-card__switch">
        <span>第一次来到智汇校园？</span>
        <el-link
          :underline="false"
          @click="emit('go-register')"
        >
          创建账号
        </el-link>
      </div>
    </el-form>

    <div class="auth-card__foot">
      <span>勤慎诚恕 · 博雅精进</span>
      <span>HZNU · 1908</span>
    </div>
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
  display: flex;
  flex-direction: column;
  padding: 46px 48px 34px;
  background: var(--surface);
}

.auth-card__head {
  margin-bottom: 28px;
}

.auth-card__head h2 {
  margin: 12px 0 8px;
  font-family: var(--font-serif);
  font-size: 34px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--ink);
}

.auth-card__lede {
  margin: 0;
  color: var(--ink-3);
  font-size: 13px;
}

.auth-card :deep(.el-form-item) {
  margin-bottom: 18px;
}

.auth-card :deep(.el-form-item__label) {
  padding-bottom: 6px;
  font-size: 11.5px;
  letter-spacing: 0.08em;
  color: var(--ink-3);
}

.auth-card :deep(.el-input__wrapper) {
  min-height: 44px;
  background: var(--surface-2);
  box-shadow: 0 0 0 1px var(--line) inset;
}

.auth-card :deep(.el-input__wrapper.is-focus) {
  background: var(--surface);
}

.captcha-row {
  display: grid;
  grid-template-columns: 1fr 122px;
  gap: 10px;
  width: 100%;
}

.captcha-box {
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: var(--surface-2);
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.captcha-box:hover {
  border-color: rgba(176, 138, 69, 0.5);
  box-shadow: 0 0 0 3px var(--gold-faint);
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

.auth-card__links {
  display: flex;
  justify-content: flex-end;
  margin: -4px 0 18px;
}

.auth-card__links :deep(.el-link),
.auth-card__switch :deep(.el-link) {
  font-size: 12.5px;
  color: var(--lake);
}

.auth-card__links :deep(.el-link:hover),
.auth-card__switch :deep(.el-link:hover) {
  color: var(--gold);
}

.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  letter-spacing: 0.08em;
}

.auth-card__switch {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 18px;
  font-size: 12.5px;
  color: var(--ink-3);
}

.auth-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: auto;
  padding-top: 28px;
  border-top: 1px solid var(--line);
  font-family: var(--font-latin);
  font-size: 8.5px;
  letter-spacing: 0.16em;
  color: var(--ink-4);
}

.auth-card__foot span:first-child {
  font-family: var(--font-serif);
  font-size: 10px;
  letter-spacing: 0.12em;
}

@media (max-width: 700px) {
  .auth-card {
    padding: 32px 24px 26px;
  }

  .auth-card__head h2 {
    font-size: 28px;
  }

  .auth-card__foot {
    padding-top: 20px;
  }
}
</style>

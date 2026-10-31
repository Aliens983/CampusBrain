<template>
  <section class="auth-card">
    <div class="auth-card__head">
      <p class="hz-eyebrow">
        Reset · HZNU
      </p>
      <h2>寻回密码</h2>
      <p class="auth-card__lede">
        输入邮箱获取验证码，设置新密码后重新登录。
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
        label="邮箱验证码"
        prop="code"
      >
        <div class="verify-row">
          <el-input
            v-model="form.code"
            placeholder="6 位验证码"
          />
          <el-button
            :loading="sendingCode"
            @click="emit('send-code')"
          >
            发送验证码
          </el-button>
        </div>
      </el-form-item>
      <el-form-item
        label="新密码"
        prop="password"
      >
        <el-input
          v-model="form.password"
          type="password"
          show-password
          placeholder="设置新密码"
        />
      </el-form-item>
      <el-form-item
        label="确认新密码"
        prop="confirmPassword"
      >
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          placeholder="再次输入新密码"
        />
      </el-form-item>

      <el-button
        type="primary"
        class="submit-btn"
        :loading="loading"
        @click="handleSubmit"
      >
        确认重置
      </el-button>

      <div class="auth-card__switch">
        <span>想起密码了？</span>
        <el-link
          :underline="false"
          @click="emit('back-login')"
        >
          返回登录
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
import type { ResetFormState } from '../composables'

defineProps<{
  form: ResetFormState
  rules: FormRules
  loading: boolean
  sendingCode: boolean
}>()

const emit = defineEmits<{
  submit: []
  'send-code': []
  'back-login': []
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
}

.verify-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  width: 100%;
}

.submit-btn {
  width: 100%;
  height: 46px;
  margin-top: 6px;
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

.auth-card__switch :deep(.el-link) {
  font-size: 12.5px;
  color: var(--lake);
}

.auth-card__switch :deep(.el-link:hover) {
  color: var(--gold);
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
}
</style>

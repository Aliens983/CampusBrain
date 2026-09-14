<template>
  <div class="auth-page">
    <div class="auth-shell">
      <AuthHero :reset-mode="resetMode" />

      <!-- 登录表单 -->
      <LoginForm
        v-if="!resetMode"
        :form="loginForm"
        :rules="loginRules"
        :loading="loading"
        :captcha-image="captchaImage"
        @submit="handleLogin"
        @refresh-captcha="refreshCaptcha"
        @forgot-password="enterResetMode"
        @go-register="goRegister"
      />

      <!-- 重置密码表单 -->
      <ResetPasswordForm
        v-else
        :form="resetForm"
        :rules="resetRules"
        :loading="loading"
        :sending-code="sendingCode"
        @submit="handleReset"
        @send-code="sendResetCode"
        @back-login="backToLogin"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import AuthHero from './components/AuthHero.vue'
import LoginForm from './components/LoginForm.vue'
import ResetPasswordForm from './components/ResetPasswordForm.vue'
import { useAuthPage } from './composables'

const {
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
} = useAuthPage()
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

@media (max-width: 960px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }
}
</style>

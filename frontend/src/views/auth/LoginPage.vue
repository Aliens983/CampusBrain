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
  position: relative;
  min-height: 100vh;
  padding: 32px;
  display: grid;
  place-items: center;
  overflow: hidden;
}

/* 页面水印：超大号 HZNU 衬线字，只做纸面肌理 */
.auth-page::before {
  content: "HZNU";
  position: absolute;
  right: -30px;
  bottom: -130px;
  font-family: var(--font-latin);
  font-size: clamp(200px, 26vw, 380px);
  line-height: 1;
  letter-spacing: 0.06em;
  color: rgba(24, 49, 59, 0.035);
  pointer-events: none;
  user-select: none;
}

.auth-page::after {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(176, 138, 69, 0.3), transparent);
  pointer-events: none;
}

.auth-shell {
  position: relative;
  z-index: 1;
  width: min(1180px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(420px, 0.88fr);
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid var(--line);
  background: var(--surface);
  box-shadow:
    0 44px 96px -54px rgba(24, 49, 59, 0.6),
    0 1px 0 rgba(255, 255, 255, 0.8) inset;
  animation: authIn 0.42s var(--ease-out) both;
}

.auth-shell::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  z-index: 2;
  background: linear-gradient(90deg, var(--lake), var(--hz-blue) 56%, var(--gold));
}

@keyframes authIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 960px) {
  .auth-shell {
    grid-template-columns: 1fr;
    max-width: 620px;
  }
}

@media (max-width: 600px) {
  .auth-page {
    padding: 12px;
  }

  .auth-shell {
    border-radius: 14px;
  }
}
</style>

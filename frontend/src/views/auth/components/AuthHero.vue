<template>
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
</template>

<script setup lang="ts">
defineProps<{
  resetMode: boolean
}>()
</script>

<style scoped lang="scss">
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

@media (max-width: 960px) {
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

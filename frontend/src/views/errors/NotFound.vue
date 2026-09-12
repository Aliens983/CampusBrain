<template>
  <div class="not-found">
    <div class="not-found__halo" />
    <div class="not-found__card">
      <div class="not-found__code">
        <span>4</span>
        <span class="is-zero">
          <el-icon><Compass /></el-icon>
        </span>
        <span>4</span>
      </div>
      <h1>页面走丢了</h1>
      <p>您访问的页面不存在或已被调整，返回工作台继续使用校园预约服务吧。</p>
      <div class="not-found__actions">
        <el-button type="primary" @click="router.push('/dashboard')">
          返回工作台
        </el-button>
        <el-button @click="router.back()">返回上一页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Compass } from '@element-plus/icons-vue'

const router = useRouter()
</script>

<style scoped lang="scss">
.not-found {
  position: relative;
  min-height: 74vh;
  display: grid;
  place-content: center;
  padding: 24px;
}

.not-found__halo {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 420px;
  height: 420px;
  transform: translate(-50%, -52%);
  border-radius: 50%;
  background: radial-gradient(circle, rgba(63, 182, 255, 0.14), rgba(63, 182, 255, 0) 68%);
  pointer-events: none;
}

.not-found__card {
  position: relative;
  display: grid;
  justify-items: center;
  gap: 14px;
  padding: 48px 56px;
  text-align: center;
  border-radius: 28px;
  background: var(--bg-panel);
  border: 1px solid var(--border-soft);
  box-shadow: var(--shadow-card);
  animation: nfRise 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.not-found__code {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 88px;
  font-weight: 800;
  line-height: 1;
  background: linear-gradient(135deg, #0E6CD6 10%, #3FB6FF 60%, #7BD0FF);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  letter-spacing: -0.03em;
}

.not-found__code .is-zero {
  display: inline-grid;
  place-items: center;
  width: 78px;
  height: 78px;
  margin: 0 2px;
  border-radius: 24px;
  background: linear-gradient(135deg, #0E6CD6, #3FB6FF);
  -webkit-text-fill-color: #fff;
  box-shadow: 0 14px 30px rgba(30, 120, 220, 0.32);
  animation: nfFloat 3.4s ease-in-out infinite;
}

.not-found__code .is-zero .el-icon {
  font-size: 40px;
}

.not-found h1 {
  margin: 6px 0 0;
  font-size: 22px;
  font-weight: 700;
}

.not-found p {
  margin: 0;
  max-width: 440px;
  color: var(--text-secondary);
  line-height: 1.8;
}

.not-found__actions {
  display: flex;
  gap: 12px;
  margin-top: 10px;
}

@keyframes nfRise {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes nfFloat {
  0%, 100% { transform: translateY(0) rotate(-4deg); }
  50% { transform: translateY(-8px) rotate(4deg); }
}

@media (max-width: 560px) {
  .not-found__card { padding: 36px 24px; }
  .not-found__code { font-size: 64px; }
  .not-found__code .is-zero { width: 58px; height: 58px; border-radius: 18px; }
  .not-found__code .is-zero .el-icon { font-size: 30px; }
  .not-found__actions { flex-direction: column; width: 100%; }
  .not-found__actions .el-button { margin-left: 0; width: 100%; }
}
</style>

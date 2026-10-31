<template>
  <div class="not-found">
    <div class="not-found__halo" />
    <div class="not-found__card">
      <p class="hz-eyebrow">
        Lost page · HZNU
      </p>
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
  background: radial-gradient(circle, rgba(214, 193, 154, 0.12), rgba(214, 193, 154, 0) 68%);
  pointer-events: none;
}

.not-found__card {
  position: relative;
  display: grid;
  justify-items: center;
  gap: 14px;
  padding: 46px 56px;
  text-align: center;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-card);
  animation: nfRise 0.28s var(--ease-out) both;
}

.not-found__card .hz-eyebrow {
  margin: 0;
  font-size: 9px;
  letter-spacing: 0.2em;
}

.not-found__code {
  display: flex;
  align-items: center;
  gap: 4px;
  font-family: var(--font-latin);
  font-size: 88px;
  font-weight: 600;
  line-height: 1;
  color: var(--lake);
  letter-spacing: 0.02em;
}

.not-found__code .is-zero {
  display: inline-grid;
  place-items: center;
  width: 76px;
  height: 76px;
  margin: 0 2px;
  border-radius: var(--radius-md);
  background: var(--lake);
  color: #f7f1e4;
  box-shadow: none;
}

.not-found__code .is-zero .el-icon {
  font-size: 38px;
}

.not-found h1 {
  margin: 6px 0 0;
  font-family: var(--font-serif);
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--ink);
}

.not-found p {
  margin: 0;
  max-width: 440px;
  color: var(--ink-3);
  font-size: 12.5px;
  line-height: 1.9;
}

.not-found__actions {
  display: flex;
  gap: 12px;
  margin-top: 10px;
}

@keyframes nfRise {
  from { transform: translateY(10px); }
  to { transform: translateY(0); }
}

@media (max-width: 560px) {
  .not-found__card {
    padding: 36px 24px;
  }

  .not-found__code {
    font-size: 64px;
  }

  .not-found__code .is-zero {
    width: 58px;
    height: 58px;
    border-radius: var(--radius-sm);
  }

  .not-found__code .is-zero .el-icon {
    font-size: 30px;
  }

  .not-found__actions {
    flex-direction: column;
    width: 100%;
  }

  .not-found__actions .el-button {
    width: 100%;
    margin-left: 0;
  }
}
</style>

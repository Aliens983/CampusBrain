<template>
  <div class="route-loading">
    <div class="route-loading__card">
      <div class="route-loading__spinner" />
      <div class="route-loading__title">
        页面加载中
      </div>
      <div class="route-loading__text">
        正在准备界面与数据
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.route-loading {
  min-height: 46vh;
  display: grid;
  place-items: center;
}

.route-loading__card {
  width: min(320px, calc(100vw - 32px));
  padding: 28px 24px;
  text-align: center;
  border-radius: 24px;
  border: 1px solid var(--border-soft);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: var(--shadow-card);
}

.route-loading__spinner {
  width: 40px;
  height: 40px;
  margin: 0 auto 14px;
  border: 3px solid rgba(20, 88, 212, 0.14);
  border-top-color: var(--brand-500);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.route-loading__title {
  font-size: 16px;
  font-weight: 700;
}

.route-loading__text {
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 13px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>

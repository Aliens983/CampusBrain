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
  padding: 26px 24px;
  text-align: center;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.route-loading__spinner {
  width: 36px;
  height: 36px;
  margin: 0 auto 14px;
  border: 2.5px solid var(--line);
  border-top-color: var(--lake);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.route-loading__title {
  font-family: var(--font-serif);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--ink);
}

.route-loading__text {
  margin-top: 6px;
  color: var(--ink-4);
  font-size: 11.5px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>

<template>
  <div
    v-if="loading"
    class="loading-container"
  >
    <div class="loading-spinner">
      <div class="spinner-circle circle-1" />
      <div class="spinner-circle circle-2" />
      <div class="spinner-circle circle-3" />
    </div>
    <p class="loading-text">
      {{ text }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

// 加载状态
const loading = ref(false)

// 加载文本
const text = ref('加载中...')

// 监听loading变化
watch(() => loading.value, (newVal) => {
  if (newVal) {
    text.value = '加载中...'
  }
})

// 暴露属性和方法
defineExpose({
  show,
  hide,
  setText
})

// 显示加载
function show(customText?: string) {
  loading.value = true
  if (customText) {
    text.value = customText
  }
}

// 隐藏加载
function hide() {
  loading.value = false
}

// 设置文本
function setText(customText: string) {
  text.value = customText
}
</script>

<style scoped lang="scss">
.loading-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(244, 249, 255, 0.62);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 2000;

  .loading-spinner {
    display: flex;
    gap: 10px;

    .spinner-circle {
      width: 18px;
      height: 18px;
      border-radius: 50%;
      background: linear-gradient(135deg, #3FB6FF, #1E98F2);
      box-shadow: 0 4px 12px rgba(30, 152, 242, 0.3);
      animation: bounce 1.4s infinite ease-in-out both;

      &.circle-1 {
        animation-delay: -0.32s;
      }

      &.circle-2 {
        animation-delay: -0.16s;
      }
    }
  }

  .loading-text {
    margin-top: 20px;
    color: #1E6FB8;
    font-size: 14px;
    font-weight: 500;
    letter-spacing: 0.02em;
  }
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  
  40% {
    transform: scale(1);
  }
}
</style>
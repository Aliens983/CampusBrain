<template>
  <div class="not-found">
    <div class="status-pill is-danger">
      404
    </div>
    <h1>页面不存在</h1>
    <p>当前地址未匹配到已交付的前端页面，你可以返回工作台继续查看综合前端结果。</p>
    <el-button
      type="primary"
      @click="router.push('/dashboard')"
    >
      返回工作台
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped lang="scss">
.not-found {
  min-height: 70vh;
  display: grid;
  place-content: center;
  gap: 14px;
  justify-items: center;
  text-align: center;
}

.not-found h1,
.not-found p {
  margin: 0;
}

.not-found p {
  max-width: 520px;
  color: var(--text-secondary);
}
</style>

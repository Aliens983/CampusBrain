<script setup lang="ts">
/** 服务治理页顶部：标题、新增按钮、统计卡 */
defineProps<{ total: number; availableTotal: number }>()
const emit = defineEmits<{ (e: 'create'): void }>()
</script>

<template>
  <section class="admin-hero">
    <div class="admin-hero__main">
      <p class="hz-eyebrow">
        Service governance · HZNU
      </p>
      <h1>服务治理</h1>
      <p class="admin-hero__desc">
        维护服务目录、开放状态与封面，让每一项校园资源都清晰可达。
      </p>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="emit('create')">新增服务</el-button>
      </div>
    </div>
    <div class="admin-hero__signal">
      <div class="signal-card">
        <span>服务总数</span><strong>{{ total }}</strong><small>已纳入统一管理</small>
      </div>
      <div class="signal-card">
        <span>可用服务</span><strong>{{ availableTotal }}</strong><small>当前可开放预约</small>
      </div>
    </div>
  </section>
</template>

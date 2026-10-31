<template>
  <section class="dashboard-hero">
    <div class="dashboard-hero__main">
      <p class="hz-eyebrow">
        Member profile · HZNU
      </p>
      <h1>个人中心</h1>
      <p class="dashboard-hero__desc">
        你的校园身份、通知偏好与常用入口，都在这里。
      </p>
      <p class="dashboard-hero__motto">
        HZNU · 1908 / 勤慎诚恕 · 博雅精进
      </p>
    </div>
    <div class="dashboard-hero__panel">
      <div class="hero-panel__label">
        快捷入口
      </div>
      <div
        v-if="isTeacher"
        class="hero-panel__item hero-panel__item--action"
        @click="emit('navigate', '/teacher/review')"
      >
        <strong>待我审核</strong><span>学生申请处理 →</span>
      </div>
      <div
        v-else
        class="hero-panel__item hero-panel__item--action"
        @click="emit('navigate', '/bookings')"
      >
        <strong>我的预约</strong><span>查看记录 →</span>
      </div>
      <div
        class="hero-panel__item hero-panel__item--action"
        @click="emit('openQuick')"
      >
        <strong>快捷操作</strong><span>常用入口 →</span>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{ isTeacher: boolean }>()

const emit = defineEmits<{
  navigate: [to: string]
  openQuick: []
}>()
</script>

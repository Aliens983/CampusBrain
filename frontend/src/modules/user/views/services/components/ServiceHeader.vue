<template>
  <div class="service-detail__cover">
    <img
      v-if="service.imageUrl"
      :src="assetUrl(service.imageUrl)"
      class="cover-badge cover-badge__img"
      alt="封面"
    >
    <div
      v-else
      class="cover-badge"
      :class="service.image"
    >
      {{ service.code }}
    </div>
    <div>
      <h3>{{ service.name }}</h3>
      <p class="muted">
        {{ service.description }}
      </p>
    </div>
  </div>
  <div class="info-list">
    <div class="info-row">
      <span>业务类别</span><strong>{{ service.category }}</strong>
    </div>
    <div class="info-row">
      <span>所属校区</span><strong>{{ service.location }}</strong>
    </div>
    <div class="info-row">
      <span>状态</span>
      <el-tag :type="service.status === 'available' ? 'success' : 'warning'">
        {{ service.status === 'available' ? '可预约' : '维护中' }}
      </el-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ServiceCard } from '@/common/types'
import { assetUrl } from '../composables'

defineProps<{ service: ServiceCard }>()
</script>

<style scoped lang="scss">
.service-detail__cover { display: flex; align-items: center; gap: 16px; }
.service-detail__cover h3 { margin: 0 0 4px; font-size: 20px; }
.service-detail__cover .muted { margin: 0; }
.cover-badge { width: 72px; height: 72px; border-radius: 18px; display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: 700; color: #fff; flex-shrink: 0; }
.cover-badge__img { object-fit: cover; background: #fff; }
.info-list { display: grid; gap: 8px; }
.info-row { display: flex; justify-content: space-between; align-items: center; font-size: 14px; padding: 6px 0; border-bottom: 1px dashed var(--border-soft); }
.info-row:last-child { border-bottom: none; }
.info-row span { color: var(--text-secondary); }
</style>

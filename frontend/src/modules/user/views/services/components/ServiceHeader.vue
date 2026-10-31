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
    <div class="service-detail__intro">
      <p class="hz-eyebrow">
        Campus service · HZNU
      </p>
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
.service-detail__cover {
  display: flex;
  align-items: center;
  gap: 18px;
}

.service-detail__intro {
  min-width: 0;
}

.service-detail__intro .hz-eyebrow {
  margin: 0 0 6px;
  font-size: 9px;
  letter-spacing: 0.2em;
}

.service-detail__cover h3 {
  margin: 0 0 6px;
  font-family: var(--font-serif);
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--ink);
}

.service-detail__cover .muted {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
}

.cover-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 84px;
  height: 84px;
  min-height: 0;
  flex-shrink: 0;
  border-radius: var(--radius-md);
  color: #f7f1e4;
  font-family: var(--font-serif);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.cover-badge__img {
  object-fit: cover;
  background: var(--surface-2);
  border: 1px solid var(--line);
}

.info-list {
  margin-top: 18px;
}

.info-row {
  align-items: center;
  font-size: 13px;
}

.info-row:last-child {
  padding-bottom: 0;
}
</style>

<template>
  <el-card class="surface-card">
    <template #header>
      <div class="card-head">
        <div>
          <h3>常用服务</h3>
          <p>常用校园预约服务，点击卡片即可发起预约。</p>
        </div>
        <el-button
          text
          @click="emit('view-all')"
        >
          全部服务
        </el-button>
      </div>
    </template>
    <div class="service-grid">
      <article
        v-for="service in services"
        :key="service.id"
        class="service-card"
        @click="emit('select', service)"
      >
        <img
          v-if="service.imageUrl"
          :src="assetUrl(service.imageUrl)"
          class="service-card__cover-img"
          alt="封面"
        >
        <div
          v-else
          class="service-card__cover"
          :class="service.image"
        >
          {{ service.code }}
        </div>
        <div class="service-card__content">
          <div class="service-card__head">
            <strong>{{ service.name }}</strong>
            <el-tag :type="service.status === 'available' ? 'success' : 'warning'">
              {{ service.status === 'available' ? '可预约' : '维护中' }}
            </el-tag>
          </div>
          <p>{{ service.description }}</p>
          <div class="service-card__meta">
            <span>{{ service.category }}</span>
            <span>{{ service.location }}</span>
          </div>
        </div>
      </article>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { ServiceCard } from '@/common/types'
import { assetUrl } from '../composables'

defineProps<{
  services: ServiceCard[]
}>()

const emit = defineEmits<{
  (e: 'select', service: ServiceCard): void
  (e: 'view-all'): void
}>()
</script>

<style scoped lang="scss">
.surface-card {
  border-radius: 24px;
  box-shadow: var(--shadow-card);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-head h3 {
  font-size: 17px;
  font-weight: 600;
}

.card-head p {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.service-card {
  display: flex;
  gap: 14px;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid var(--border-soft);
  background: linear-gradient(180deg, #fff, #F9FCFF);
  cursor: pointer;
  transition: transform .24s ease, box-shadow .24s ease;
}

.service-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 28px rgba(20,33,61,.1);
}

.service-card__cover {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

/* 上传过封面图：用真实图片替代渐变底（与 service-card__cover 同尺寸） */
.service-card__cover-img {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  object-fit: cover;
  border: 1px solid var(--border-soft);
  flex-shrink: 0;
}

.gradient-brand { background: linear-gradient(135deg, #ADE2FF, #7BD0FF); }
.gradient-teal { background: linear-gradient(135deg, #11998e, #38ef7d); }
.gradient-amber { background: linear-gradient(135deg, #f093fb, #f5576c); }
.gradient-slate { background: linear-gradient(135deg, #4b6cb7, #182848); }

.service-card__content {
  flex: 1;
  min-width: 0;
}

.service-card__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.service-card__head strong {
  font-size: 14px;
  font-weight: 600;
}

.service-card__content p {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.service-card__meta {
  display: flex;
  gap: 12px;
  margin-top: 8px;
  font-size: 11px;
  color: var(--text-secondary);
}
</style>

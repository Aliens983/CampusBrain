<template>
  <el-card
    class="surface-card"
    shadow="never"
  >
    <template #header>
      <div class="card-head">
        <div class="card-head__title">
          <span class="card-head__index">贰</span>
          <div>
            <h3>常用服务</h3>
            <p>常用校园预约服务，点击卡片即可发起预约。</p>
          </div>
        </div>
        <el-button
          text
          @click="emit('view-all')"
        >
          全部服务 →
        </el-button>
      </div>
    </template>
    <div class="service-grid">
      <article
        v-for="service in services.slice(0, 4)"
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
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.card-head__title {
  display: flex;
  align-items: flex-start;
  gap: 11px;
}

.card-head__index {
  padding-top: 3px;
  font-family: var(--font-serif);
  font-size: 11px;
  color: var(--gold);
}

.card-head h3 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 600;
  color: var(--ink);
}

.card-head p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--ink-3);
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.service-card {
  display: flex;
  gap: 13px;
  padding: 15px;
  border-radius: 10px;
  border: 1px solid var(--line);
  background: var(--surface-2);
  cursor: pointer;
  transition:
    transform 0.18s var(--ease-out),
    border-color 0.18s ease,
    background-color 0.18s ease;
}

.service-card:hover {
  transform: translateY(-2px);
  border-color: rgba(176, 138, 69, 0.42);
  background: var(--surface);
}

.service-card__cover {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-serif);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: #f7f1e4;
  flex-shrink: 0;
}

.service-card__cover-img {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid var(--line);
  flex-shrink: 0;
}

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
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
}

.service-card__content p {
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11.5px;
  color: var(--ink-3);
}

.service-card__meta {
  display: flex;
  gap: 10px;
  margin-top: 8px;
  font-size: 10.5px;
  color: var(--ink-4);
}

.service-card__meta span + span::before {
  content: "·";
  margin-right: 10px;
  color: var(--gold);
}

@media (max-width: 720px) {
  .service-grid {
    grid-template-columns: 1fr;
  }

  .card-head :deep(.el-button) {
    display: none;
  }
}
</style>

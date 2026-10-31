<template>
  <el-drawer
    v-model="drawerVisible"
    title="服务速览"
    size="420px"
    :lock-scroll="false"
  >
    <template v-if="service">
      <div class="drawer-stack">
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
          <p class="hz-eyebrow">
            Service preview · HZNU
          </p>
          <h3>{{ service.name }}</h3>
          <p class="muted">
            {{ service.description }}
          </p>
        </div>
        <div class="info-list">
          <div class="info-row">
            <span>业务类别</span><strong>{{ service.category }}</strong>
          </div>
          <div class="info-row">
            <span>所属校区</span><strong>{{ service.location }}</strong>
          </div>
          <div class="info-row">
            <span>状态</span><strong>{{ service.priceLabel }}</strong>
          </div>
        </div>
        <div class="tag-wrap">
          <el-tag
            v-for="tag in service.tags"
            :key="tag"
          >
            {{ tag }}
          </el-tag>
        </div>
        <el-button
          type="primary"
          @click="emit('view-detail', service.id)"
        >
          查看完整详情
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ServiceCard } from '@/common/types'
import { assetUrl } from '../composables'

const props = defineProps<{
  visible: boolean
  service: ServiceCard | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'view-detail', id: number): void
}>()

const drawerVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value),
})
</script>

<style scoped lang="scss">
.drawer-stack {
  display: grid;
  gap: 16px;
}

.drawer-stack .hz-eyebrow {
  margin: 0 0 6px;
  font-size: 8.5px;
  letter-spacing: 0.18em;
}

.drawer-stack h3 {
  margin: 0 0 6px;
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--ink);
}

.drawer-stack .muted {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.7;
}

.cover-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  min-height: 0;
  border-radius: var(--radius-sm);
  color: #f7f1e4;
  font-family: var(--font-serif);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.06em;
}

.cover-badge__img {
  object-fit: cover;
  border: 1px solid var(--line);
  background: var(--surface-2);
}

.tag-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>

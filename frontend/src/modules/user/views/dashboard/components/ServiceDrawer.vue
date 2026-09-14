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
            round
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

.cover-badge {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
}

/* 抽屉速览里上传过封面时，用图片替代渐变底 */
.cover-badge__img {
  object-fit: cover;
  background: #fff;
}

.gradient-brand { background: linear-gradient(135deg, #ADE2FF, #7BD0FF); }
.gradient-teal { background: linear-gradient(135deg, #11998e, #38ef7d); }
.gradient-amber { background: linear-gradient(135deg, #f093fb, #f5576c); }
.gradient-slate { background: linear-gradient(135deg, #4b6cb7, #182848); }

.info-list {
  display: grid;
  gap: 10px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.info-row span {
  color: var(--text-secondary);
}

.tag-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>

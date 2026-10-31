<script setup lang="ts">
import type { ServiceCard } from '@/common/types'

/** 服务列表：卡片 + 空状态 + 分页 */
const props = defineProps<{
  services: ServiceCard[]
  loading: boolean
  total: number
  pageNo: number
  pageSize: number
  assetUrl: (path?: string) => string
}>()

const emit = defineEmits<{
  (e: 'edit', item: ServiceCard): void
  (e: 'page-change', page: number): void
  (e: 'size-change', size: number): void
}>()
</script>

<template>
  <div>
    <div class="service-stack">
      <article v-for="item in props.services" :key="item.id" class="service-item">
        <img
          v-if="item.imageUrl"
          :src="props.assetUrl(item.imageUrl)"
          class="service-item__cover-img"
          alt="封面"
        >
        <div v-else class="service-item__cover" :class="item.image">{{ item.code }}</div>
        <div class="service-item__main">
          <div class="service-item__head">
            <div>
              <strong>{{ item.name }}</strong>
              <p>{{ item.category }} / {{ item.location }}</p>
            </div>
            <el-tag :type="item.status === 'available' ? 'success' : 'warning'">
              {{ item.status === 'available' ? '可用' : '维护中' }}
            </el-tag>
          </div>
          <div class="service-item__meta">
            <span>{{ item.priceLabel }}</span>
            <span>{{ item.description }}</span>
          </div>
        </div>
        <div class="service-item__action">
          <el-button plain @click="emit('edit', item)">编辑</el-button>
        </div>
      </article>

      <el-empty
        v-if="!props.loading && props.services.length === 0"
        description="没有符合条件的服务"
      />
    </div>

    <el-pagination
      v-if="props.total > 0"
      class="list-pagination"
      background
      :current-page="props.pageNo"
      :page-size="props.pageSize"
      :page-sizes="[5, 10, 20]"
      :total="props.total"
      layout="total, sizes, prev, pager, next"
      @current-change="emit('page-change', $event)"
      @size-change="emit('size-change', $event)"
    />
  </div>
</template>

<style scoped lang="scss">
.service-stack {
  display: grid;
  gap: 12px;
}

.list-pagination {
  display: flex;
  justify-content: flex-start;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--line);
}

.service-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 16px;
  padding: 16px 18px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface-2);
  transition:
    transform 0.18s var(--ease-out),
    border-color 0.18s ease,
    background-color 0.18s ease,
    box-shadow 0.18s ease;
}

.service-item:hover {
  transform: translateY(-2px);
  border-color: rgba(176, 138, 69, 0.42);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.service-item__cover {
  display: grid;
  place-items: center;
  width: 72px;
  min-height: 72px;
  border-radius: var(--radius-sm);
  color: #f7f1e4;
  font-family: var(--font-serif);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
}

.service-item__cover-img {
  width: 72px;
  height: 72px;
  flex-shrink: 0;
  object-fit: cover;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
}

.service-item__main {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.service-item__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.service-item__head strong {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}

.service-item__head p {
  margin: 4px 0 0;
  color: var(--ink-4);
  font-size: 11.5px;
}

.service-item__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 14px;
  color: var(--ink-3);
  font-size: 12px;
}

.service-item__action {
  display: flex;
  align-items: center;
  gap: 10px;
}

@media (max-width: 960px) {
  .service-item,
  .service-item__head,
  .service-item__action {
    display: flex;
    flex-direction: column;
    align-items: stretch;
  }
}
</style>

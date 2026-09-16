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
.service-stack { display: grid; gap: 14px; }
/* 分页栏固定在列表左下角 */
.list-pagination {
  display: flex; justify-content: flex-start; flex-wrap: wrap; gap: 8px;
  margin-top: 18px; padding-top: 16px; border-top: 1px solid var(--border-soft);
}
.service-item {
  display: grid; grid-template-columns: auto 1fr auto; gap: 16px; padding: 18px;
  border-radius: 20px; border: 1px solid var(--border-soft);
  background: linear-gradient(180deg, #fff, #F9FCFF);
  transition: transform .24s ease, box-shadow .24s ease, border-color .24s ease;
}
.service-item:hover {
  transform: translateY(-4px); box-shadow: 0 18px 28px rgba(20, 33, 61, .1);
  border-color: rgba(63, 182, 255, .14);
}
.service-item__cover {
  width: 72px; min-height: 72px; display: grid; place-items: center;
  border-radius: 18px; color: #fff; font-weight: 700;
}
.service-item__cover-img {
  width: 72px; height: 72px; object-fit: cover; border-radius: 18px;
  border: 1px solid var(--border-soft); flex-shrink: 0;
}
.service-item__main { display: grid; gap: 10px; }
.service-item__head { display: flex; justify-content: space-between; gap: 12px; }
.service-item__head p { margin: 4px 0 0; color: var(--text-tertiary); font-size: 12px; }
.service-item__meta { display: grid; gap: 6px; color: var(--text-secondary); font-size: 13px; }
.service-item__action { display: flex; align-items: center; gap: 10px; }
@media (max-width: 960px) {
  .service-item, .service-item__head, .service-item__action {
    display: flex; flex-direction: column; align-items: stretch;
  }
}
</style>

<script setup lang="ts">
/** 服务列表筛选：名称搜索 + 状态下拉 + 校区 Tab */
defineProps<{
  keyword: string
  statusFilter: string
  campusFilter: string
  campusOptions: Array<{ label: string; value: string }>
  campusCount: (value: string) => number
}>()

const emit = defineEmits<{
  (e: 'update:keyword', value: string): void
  (e: 'update:statusFilter', value: string): void
  (e: 'search'): void
  (e: 'campus-change', value: string): void
}>()
</script>

<template>
  <div class="filter-bar">
    <div class="toolbar">
      <el-input
        :model-value="keyword"
        placeholder="搜索服务名称"
        class="service-search"
        clearable
        @update:model-value="emit('update:keyword', $event as string)"
        @keyup.enter="emit('search')"
        @clear="emit('search')"
      >
        <template #append>
          <el-button @click="emit('search')">搜索</el-button>
        </template>
      </el-input>
      <el-select
        :model-value="statusFilter"
        style="width: 150px"
        @update:model-value="emit('update:statusFilter', $event as string)"
      >
        <el-option label="全部状态" value="" />
        <el-option label="可用" value="available" />
        <el-option label="维护中" value="maintenance" />
      </el-select>
    </div>

    <div class="campus-seg">
      <button
        v-for="opt in campusOptions"
        :key="opt.value"
        type="button"
        class="campus-seg__item"
        :class="{ 'is-active': campusFilter === opt.value }"
        @click="emit('campus-change', opt.value)"
      >
        {{ opt.label }}
        <span class="campus-seg__count">{{ campusCount(opt.value) }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 14px;
}

.service-search {
  width: 240px;
  max-width: 46vw;
}

.service-search :deep(.el-input-group__append) {
  padding: 0;
  border-color: var(--line);
  background: var(--surface-2);
  box-shadow: none;
}

.service-search :deep(.el-input-group__append .el-button) {
  height: 100%;
  margin: 0;
  padding: 0 16px;
  border: none;
  background: transparent;
  color: var(--lake);
  box-shadow: none;
}

.service-search :deep(.el-input-group__append .el-button:hover) {
  background: var(--surface);
  color: var(--lake-deep);
}

.campus-seg {
  display: inline-flex;
  gap: 3px;
  padding: 3px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--surface-2);
}

.campus-seg__item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--ink-3);
  font-family: var(--font-sans);
  font-size: 12.5px;
  cursor: pointer;
  transition: background-color 0.16s ease, color 0.16s ease, box-shadow 0.16s ease;
}

.campus-seg__item:hover {
  color: var(--ink);
}

.campus-seg__item.is-active {
  background: var(--surface);
  color: var(--lake);
  font-weight: 600;
  box-shadow: 0 1px 4px rgba(24, 49, 59, 0.08);
}

.campus-seg__count {
  font-family: var(--font-latin);
  font-size: 10px;
  color: var(--gold);
}

@media (max-width: 960px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .service-search {
    max-width: none;
    width: 100%;
  }
}
</style>

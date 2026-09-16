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
.toolbar { display: flex; gap: 12px; }
.service-search { width: 240px; max-width: 46vw; }
.campus-seg {
  display: inline-flex; gap: 4px; padding: 4px; margin-bottom: 14px;
  border-radius: 999px; background: #EEF2F7;
}
.campus-seg__item {
  display: inline-flex; align-items: center; gap: 6px; padding: 6px 18px;
  border-radius: 999px; border: 0; font-size: 13px; color: var(--text-secondary);
  background: transparent; cursor: pointer;
  transition: background .2s, color .2s, box-shadow .2s;
}
.campus-seg__item:hover { color: var(--text-primary); }
.campus-seg__item.is-active {
  background: #fff; color: #3B82F6; font-weight: 600;
  box-shadow: 0 2px 8px rgba(20, 33, 61, .1);
}
.campus-seg__count { font-size: 11px; opacity: .7; }
@media (max-width: 960px) { .toolbar { display: flex; flex-direction: column; align-items: stretch; } }
</style>

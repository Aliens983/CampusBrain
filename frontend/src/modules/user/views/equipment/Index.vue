<template>
  <div class="page-shell">
    <section class="page-hero">
      <div>
        <div class="status-pill is-brand">
          设备借用
        </div>
        <h1 class="page-hero__title">
          校园设备资源库存与借还管理
        </h1>
        <p class="page-hero__desc">
          查看可借用设备、实时库存和使用规则，直接发起借用申请。
        </p>
      </div>
    </section>
    <el-card class="panel-card">
      <template #header>
        <div class="section-head">
          <h3 class="section-head__title">
            设备列表
          </h3>
        </div>
      </template>
      <div class="equipment-grid">
        <article
          v-for="item in equipment"
          :key="item.id"
          class="equipment-card"
        >
          <div class="equipment-card__cover">
            {{ item.type.slice(0, 2).toUpperCase() }}
          </div>
          <div class="equipment-card__info">
            <strong>{{ item.name }}</strong>
            <p>{{ item.type }} · {{ item.location }}</p>
            <p>库存: {{ item.available }}/{{ item.total }}</p>
          </div>
          <el-tag :type="item.status === 'available' ? 'success' : 'warning'">
            {{ item.status === 'available' ? '可借用' : '维护中' }}
          </el-tag>
        </article>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const equipment = ref([
  { id: 1, name: '投影仪 A1', type: '投影仪', available: 3, total: 5, location: 'A栋设备间', status: 'available' },
  { id: 2, name: '笔记本电脑', type: '电脑', available: 2, total: 8, location: 'B栋设备间', status: 'available' },
  { id: 3, name: '音响设备套装', type: '音响', available: 0, total: 2, location: 'C栋设备间', status: 'maintenance' },
])
</script>

<style scoped lang="scss">
.equipment-grid { display: grid; gap: 14px; }
.equipment-card { display: flex; align-items: center; gap: 16px; padding: 18px; border-radius: 16px; border: 1px solid var(--border-soft); background: linear-gradient(180deg, #fff, #f8fbff); }
.equipment-card__cover { width: 48px; height: 48px; border-radius: 12px; background: linear-gradient(135deg, #ccfbf1, #99f6e4); color: #0d9488; display: flex; align-items: center; justify-content: center; font-weight: 700; }
.equipment-card__info { flex: 1; }
.equipment-card__info strong { font-size: 15px; }
.equipment-card__info p { margin: 4px 0 0; color: var(--text-secondary); font-size: 13px; }
</style>
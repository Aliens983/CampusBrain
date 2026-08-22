<template>
  <div class="page-shell">
    <section class="page-hero">
      <div>
        <div class="status-pill is-brand">
          会议室预约
        </div>
        <h1 class="page-hero__title">
          校园空间资源与可用时段管理
        </h1>
        <p class="page-hero__desc">
          按建筑、容量和设施筛选，找到最合适的会议空间并快速锁定时段。
        </p>
      </div>
    </section>
    <el-card class="panel-card">
      <template #header>
        <div class="section-head">
          <h3 class="section-head__title">
            会议室列表
          </h3>
        </div>
      </template>
      <div class="rooms-grid">
        <article
          v-for="room in rooms"
          :key="room.id"
          class="room-card"
        >
          <div class="room-card__icon">
            MR
          </div>
          <div class="room-card__info">
            <strong>{{ room.name }}</strong>
            <p>{{ room.location }} · 容纳 {{ room.capacity }} 人</p>
          </div>
          <el-tag :type="room.status === 'available' ? 'success' : 'warning'">
            {{ room.status === 'available' ? '可预约' : '维护中' }}
          </el-tag>
        </article>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const rooms = ref([
  { id: 1, name: 'A栋 201 会议室', location: 'A栋2层', capacity: 20, status: 'available' },
  { id: 2, name: 'B栋 301 会议室', location: 'B栋3层', capacity: 40, status: 'available' },
  { id: 3, name: 'C栋 102 多功能厅', location: 'C栋1层', capacity: 80, status: 'maintenance' },
])
</script>

<style scoped lang="scss">
.rooms-grid { display: grid; gap: 14px; }
.room-card { display: flex; align-items: center; gap: 16px; padding: 18px; border-radius: 16px; border: 1px solid var(--border-soft); background: linear-gradient(180deg, #fff, #f8fbff); }
.room-card__icon { width: 48px; height: 48px; border-radius: 12px; background: linear-gradient(135deg, #e0f2fe, #bae6fd); color: #0284c7; display: flex; align-items: center; justify-content: center; font-weight: 700; }
.room-card__info { flex: 1; }
.room-card__info strong { font-size: 15px; }
.room-card__info p { margin: 4px 0 0; color: var(--text-secondary); font-size: 13px; }
</style>
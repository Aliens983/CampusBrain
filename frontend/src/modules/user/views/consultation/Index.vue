<template>
  <div class="page-shell">
    <section class="page-hero">
      <div>
        <div class="status-pill is-brand">
          咨询服务
        </div>
        <h1 class="page-hero__title">
          顾问资源与时段排班
        </h1>
        <p class="page-hero__desc">
          按领域、部门和可预约时段筛选，快速联系咨询顾问并锁定咨询时间。
        </p>
      </div>
    </section>
    <el-card class="panel-card">
      <template #header>
        <div class="section-head">
          <h3 class="section-head__title">
            咨询顾问
          </h3>
        </div>
      </template>
      <div class="consultants-grid">
        <article
          v-for="item in consultants"
          :key="item.id"
          class="consultant-card"
        >
          <div class="consultant-card__avatar">
            {{ item.name.slice(0, 1) }}
          </div>
          <div class="consultant-card__info">
            <strong>{{ item.name }}</strong>
            <p>{{ item.title }} · {{ item.department }}</p>
            <div class="tag-wrap">
              <el-tag
                v-for="tag in item.expertise"
                :key="tag"
                size="small"
                round
              >
                {{ tag }}
              </el-tag>
            </div>
          </div>
          <el-tag :type="item.status === 'available' ? 'success' : 'warning'">
            {{ item.status === 'available' ? '可预约' : '已满' }}
          </el-tag>
        </article>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const consultants = ref([
  { id: 1, name: '张教授', title: '资深顾问', department: '就业指导中心', expertise: ['就业规划', '简历优化', '面试技巧'], status: 'available' },
  { id: 2, name: '李老师', title: '心理咨询师', department: '心理健康中心', expertise: ['情绪管理', '压力疏导', '人际关系'], status: 'available' },
  { id: 3, name: '王教授', title: '学业导师', department: '教务处', expertise: ['选课指导', '学业规划', '学分认定'], status: 'busy' },
])
</script>

<style scoped lang="scss">
.consultants-grid { display: grid; gap: 14px; }
.consultant-card { display: flex; align-items: center; gap: 16px; padding: 18px; border-radius: 16px; border: 1px solid var(--border-soft); background: linear-gradient(180deg, #fff, #f8fbff); }
.consultant-card__avatar { width: 48px; height: 48px; border-radius: 50%; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 18px; }
.consultant-card__info { flex: 1; }
.consultant-card__info strong { font-size: 15px; }
.consultant-card__info p { margin: 4px 0; color: var(--text-secondary); font-size: 13px; }
.tag-wrap { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
</style>
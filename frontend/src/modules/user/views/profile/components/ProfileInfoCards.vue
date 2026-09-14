<template>
  <section class="grid-cards">
    <!-- 个人信息卡片 -->
    <div class="profile-card span-5">
      <div class="profile-card__avatar">
        <el-avatar :size="72">
          {{ user?.username?.slice(0, 1) || 'U' }}
        </el-avatar>
      </div>
      <div class="profile-card__info">
        <h2>{{ user?.username || '未登录' }}</h2>
        <p class="profile-card__dept">
          {{ user?.department || '未分配部门' }}
        </p>
        <p class="profile-card__email">
          {{ user?.email || '-' }}
        </p>
        <el-tag
          :type="roleTagType"
          size="small"
          effect="plain"
        >
          {{ roleLabel }}
        </el-tag>
      </div>
    </div>

    <!-- 账户详情 -->
    <div class="info-card span-7">
      <h3 class="info-card__title">
        账户信息
      </h3>
      <div class="info-grid">
        <div class="info-item">
          <span class="info-item__label">邮箱</span>
          <span class="info-item__value">{{ user?.email || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-item__label">手机</span>
          <span class="info-item__value">{{ user?.phone || '未绑定' }}</span>
        </div>
        <div class="info-item">
          <span class="info-item__label">角色</span>
          <span class="info-item__value">{{ roleLabel }}</span>
        </div>
        <div class="info-item">
          <span class="info-item__label">注册时间</span>
          <span class="info-item__value">{{ user?.createdAt || '-' }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { UserInfo } from '@/common/types'
import type { RoleTagType } from '../composables'

defineProps<{
  user: UserInfo | null
  roleLabel: string
  roleTagType: RoleTagType
}>()
</script>

<style scoped lang="scss">
.span-5 { grid-column: span 5; }
.span-7 { grid-column: span 7; }

.profile-card {
  display: flex; align-items: center; gap: 24px;
  padding: 28px; border-radius: 20px; background: #fff;
  border: 1px solid var(--border-soft); box-shadow: var(--shadow-card);
}
.profile-card__avatar { flex-shrink: 0; }
.profile-card__info { display: grid; gap: 4px; }
.profile-card__info h2 { margin: 0; font-size: 22px; font-weight: 700; }
.profile-card__dept { margin: 2px 0 0; color: var(--text-secondary); font-size: 14px; }
.profile-card__email { margin: 0; color: var(--text-tertiary); font-size: 13px; }

.info-card {
  padding: 24px 28px; border-radius: 20px; background: #fff;
  border: 1px solid var(--border-soft); box-shadow: var(--shadow-card);
}
.info-card__title { margin: 0 0 18px; font-size: 16px; font-weight: 700; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.info-item { display: grid; gap: 4px; }
.info-item__label { font-size: 12px; color: var(--text-tertiary); }
.info-item__value { font-size: 14px; font-weight: 500; color: var(--text-primary); }

@media (max-width: 1200px) { .span-5, .span-7 { grid-column: span 12; } }
@media (max-width: 900px) { .info-grid { grid-template-columns: 1fr; } }
@media (max-width: 500px) { .profile-card { flex-direction: column; text-align: center; } }
</style>

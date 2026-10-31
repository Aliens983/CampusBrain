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
        <p class="hz-eyebrow profile-card__eyebrow">
          Member profile · HZNU
        </p>
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
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 26px 28px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  background: var(--surface);
  box-shadow: var(--shadow-card);
}

.profile-card__avatar {
  flex-shrink: 0;
}

.profile-card__avatar :deep(.el-avatar) {
  border: 1px solid var(--line-strong);
  background: var(--paper-2);
  color: var(--lake);
  font-size: 26px;
}

.profile-card__info {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.profile-card__eyebrow {
  margin: 0 0 2px;
  font-size: 8.5px;
  letter-spacing: 0.18em;
}

.profile-card__info h2 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--ink);
}

.profile-card__dept {
  margin: 2px 0 0;
  color: var(--ink-3);
  font-size: 13px;
}

.profile-card__email {
  margin: 0;
  color: var(--ink-4);
  font-size: 12px;
}

.info-card {
  padding: 24px 28px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  background: var(--surface);
  box-shadow: var(--shadow-card);
}

.info-card__title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 18px;
  font-family: var(--font-serif);
  font-size: 19px;
  font-weight: 600;
  color: var(--ink);
}

.info-card__title::before {
  content: "";
  width: 3px;
  height: 17px;
  border-radius: 1px;
  background: var(--gold);
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 28px;
}

.info-item {
  display: grid;
  gap: 4px;
  padding: 12px 0;
  border-top: 1px solid var(--line);
}

.info-item:nth-child(-n + 2) {
  padding-top: 0;
  border-top: 0;
}

.info-item__label {
  font-size: 11px;
  letter-spacing: 0.06em;
  color: var(--ink-4);
}

.info-item__value {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
}

@media (max-width: 1200px) {
  .span-5,
  .span-7 {
    grid-column: span 12;
  }
}

@media (max-width: 900px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .info-item:nth-child(-n + 2) {
    padding-top: 12px;
    border-top: 1px solid var(--line);
  }

  .info-item:first-child {
    padding-top: 0;
    border-top: 0;
  }
}

@media (max-width: 500px) {
  .profile-card {
    flex-direction: column;
    text-align: center;
  }
}
</style>

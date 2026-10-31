<template>
  <section class="grid-cards">
    <div
      class="action-card span-6"
      @click="emit('openPreference')"
    >
      <div class="action-card__icon">
        <el-icon><Bell /></el-icon>
      </div>
      <div class="action-card__text">
        <strong>通知偏好</strong>
        <p>管理邮件和系统通知的接收方式</p>
      </div>
      <el-icon class="action-card__arrow">
        <ArrowRight />
      </el-icon>
    </div>
    <div
      class="action-card span-6"
      @click="emit('openPassword')"
    >
      <div class="action-card__icon">
        <el-icon><Lock /></el-icon>
      </div>
      <div class="action-card__text">
        <strong>修改密码</strong>
        <p>定期更换密码，保护账号安全</p>
      </div>
      <el-icon class="action-card__arrow">
        <ArrowRight />
      </el-icon>
    </div>
    <div
      v-if="isTeacher"
      class="action-card span-6"
      @click="emit('navigate', '/teacher/review')"
    >
      <div class="action-card__icon">
        <el-icon><Tickets /></el-icon>
      </div>
      <div class="action-card__text">
        <strong>待我审核</strong>
        <p>学生申请我名下咨询档期</p>
      </div>
      <el-icon class="action-card__arrow">
        <ArrowRight />
      </el-icon>
    </div>
    <div
      v-else
      class="action-card span-6"
      @click="emit('navigate', '/bookings')"
    >
      <div class="action-card__icon">
        <el-icon><Tickets /></el-icon>
      </div>
      <div class="action-card__text">
        <strong>我的预约</strong>
        <p>查看进行中和历史的预约记录</p>
      </div>
      <el-icon class="action-card__arrow">
        <ArrowRight />
      </el-icon>
    </div>
    <div
      class="action-card action-card--danger span-6"
      @click="emit('logout')"
    >
      <div class="action-card__icon">
        <el-icon><SwitchButton /></el-icon>
      </div>
      <div class="action-card__text">
        <strong>退出登录</strong>
        <p>清除登录状态并返回登录页</p>
      </div>
      <el-icon class="action-card__arrow">
        <ArrowRight />
      </el-icon>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ArrowRight, Bell, Lock, SwitchButton, Tickets } from '@element-plus/icons-vue'

defineProps<{ isTeacher: boolean }>()

const emit = defineEmits<{
  openPreference: []
  openPassword: []
  navigate: [to: string]
  logout: []
}>()
</script>

<style scoped lang="scss">
.span-6 { grid-column: span 6; }

.action-card {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 18px 22px;
  border-radius: var(--radius-md);
  background: var(--surface);
  border: 1px solid var(--line);
  box-shadow: var(--shadow-soft);
  cursor: pointer;
  transition:
    transform 0.18s var(--ease-out),
    box-shadow 0.18s var(--ease-out),
    border-color 0.18s ease;
}

.action-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card-hover);
  border-color: rgba(176, 138, 69, 0.42);
}

.action-card--danger:hover {
  border-color: rgba(168, 69, 47, 0.35);
  background: rgba(168, 69, 47, 0.03);
}

.action-card--danger:hover strong {
  color: var(--seal);
}

.action-card__icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 8px;
  background: var(--surface-2);
  color: var(--lake);
  font-size: 18px;
}

.action-card--danger .action-card__icon {
  color: var(--seal);
  background: rgba(168, 69, 47, 0.08);
}

.action-card__text {
  flex: 1;
  min-width: 0;
}

.action-card__text strong {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}

.action-card__text p {
  margin: 2px 0 0;
  color: var(--ink-3);
  font-size: 12px;
}

.action-card__arrow {
  color: var(--ink-4);
  font-size: 16px;
  transition: transform 0.18s var(--ease-out), color 0.18s ease;
}

.action-card:hover .action-card__arrow {
  transform: translateX(3px);
  color: var(--lake);
}

@media (max-width: 1200px) { .span-6 { grid-column: span 12; } }
</style>

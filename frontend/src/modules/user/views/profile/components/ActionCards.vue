<template>
  <section class="grid-cards">
    <div
      class="action-card span-6"
      @click="emit('openPreference')"
    >
      <div class="action-card__icon">
        🔔
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
        🔒
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
        📋
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
        📋
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
        🚪
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
import { ArrowRight } from '@element-plus/icons-vue'

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
  display: flex; align-items: center; gap: 16px;
  padding: 20px 24px; border-radius: 18px; background: #fff;
  border: 1px solid var(--border-soft); box-shadow: 0 1px 3px rgba(0,0,0,.04);
  cursor: pointer; transition: transform .22s ease, box-shadow .22s ease, border-color .22s ease;
}
.action-card:hover { transform: translateY(-3px); box-shadow: 0 8px 28px rgba(20,33,61,.1); border-color: var(--border-strong); }
.action-card--danger:hover { border-color: #fecaca; background: #fef2f2; }
.action-card--danger:hover strong { color: #dc2626; }
.action-card__icon { font-size: 28px; flex-shrink: 0; }
.action-card__text { flex: 1; }
.action-card__text strong { font-size: 15px; font-weight: 600; }
.action-card__text p { margin: 2px 0 0; color: var(--text-secondary); font-size: 13px; }
.action-card__arrow { color: var(--text-tertiary); font-size: 18px; transition: transform .2s; }
.action-card:hover .action-card__arrow { transform: translateX(3px); color: var(--brand-500); }

@media (max-width: 1200px) { .span-6 { grid-column: span 12; } }
</style>

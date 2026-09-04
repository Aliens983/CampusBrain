<template>
  <div class="page-shell">
    <section class="dashboard-hero">
      <div class="dashboard-hero__main">
        <span class="hero-chip">个人中心</span>
        <h1>账号信息与偏好设置</h1>
      </div>
      <div class="dashboard-hero__panel">
        <div class="hero-panel__label">
          快捷入口
        </div>
        <div
          class="hero-panel__item"
          @click="router.push('/bookings')"
        >
          <strong>我的预约</strong><span>查看记录 →</span>
        </div>
        <div
          class="hero-panel__item"
          @click="quickVisible = true"
        >
          <strong>快捷操作</strong><span>常用入口 →</span>
        </div>
      </div>
    </section>

    <section class="grid-cards">
      <el-card class="panel-card span-4">
        <div class="profile-summary">
          <div class="profile-summary__halo" />
          <el-avatar :size="92">
            {{ user.username.slice(0, 1) }}
          </el-avatar>
          <h3>{{ user.username }}</h3>
          <p>{{ user.department }}</p>
          <el-tag :type="user.role === 'admin' || user.role === 'super_admin' ? 'danger' : 'success'">
            {{ roleLabel }}
          </el-tag>
        </div>
      </el-card>

      <el-card class="panel-card span-8">
        <template #header>
          <div class="section-head">
            <h3 class="section-head__title">
              账户信息
            </h3>
          </div>
        </template>
        <div class="info-list">
          <div class="info-row">
            <span>邮箱</span><strong>{{ user.email }}</strong>
          </div>
          <div class="info-row">
            <span>手机号</span><strong>{{ user.phone }}</strong>
          </div>
          <div class="info-row">
            <span>角色</span><strong>{{ roleLabel }}</strong>
          </div>
          <div class="info-row">
            <span>创建时间</span><strong>{{ user.createdAt }}</strong>
          </div>
        </div>
        <el-divider />
        <div class="preference-grid">
          <div
            class="preference-card"
            @click="preferenceVisible = true"
          >
            <div>
              <strong>消息通知</strong>
              <p>站内信、系统公告和预约提醒</p>
            </div>
            <el-switch model-value />
          </div>
          <div
            class="preference-card"
            @click="preferenceVisible = true"
          >
            <div>
              <strong>审批结果提醒</strong>
              <p>审核通过、驳回和变更通知</p>
            </div>
            <el-switch model-value />
          </div>
        </div>
      </el-card>
    </section>

    <el-dialog
      v-model="preferenceVisible"
      title="通知偏好说明"
      width="560px"
    >
      <div class="dialog-list">
        <div class="dialog-card">
          站内消息默认开启，用于承接审批结果、系统公告和预约提醒。
        </div>
        <div class="dialog-card">
          您可以在此处配置消息通知和审批结果提醒的接收偏好。
        </div>
      </div>
    </el-dialog>

    <el-drawer
      v-model="quickVisible"
      title="个人中心快捷操作"
      size="420px"
    >
      <div class="drawer-stack">
        <el-button
          type="primary"
          @click="router.push('/bookings')"
        >
          查看我的预约
        </el-button>
        <el-button
          plain
          @click="router.push('/services')"
        >
          进入服务中心
        </el-button>
        <el-button
          plain
          @click="router.push('/dashboard')"
        >
          返回工作台
        </el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/common/stores/user'

const router = useRouter()
const userStore = useUserStore()
const preferenceVisible = ref(false)
const quickVisible = ref(false)

const user = computed(() => userStore.userInfo ?? {
  id: 0,
  username: '未登录',
  email: '-',
  phone: '-',
  role: 'user',
  department: '-',
  createdAt: '-',
})

const roleLabel = computed(() => {
  const role = user.value?.role
  if (role === 'super_admin') return '超级管理员'
  if (role === 'admin') return '管理员'
  return '普通用户'
})
</script>

<style scoped lang="scss">
.dashboard-hero {
  position: relative; display: grid; grid-template-columns: 1.2fr 0.8fr; gap: 20px;
  padding: 32px; border-radius: 30px; color: #fff;
  background: linear-gradient(135deg, #0e2647, #1458d4 62%, #52a1ff);
  box-shadow: var(--shadow-card); overflow: hidden;
}
.dashboard-hero::before {
  content: ""; position: absolute; inset: 0;
  background: radial-gradient(circle at 20% 20%, rgba(255,255,255,0.16), transparent 22%),
              linear-gradient(120deg, transparent 14%, rgba(255,255,255,0.08) 36%, transparent 62%);
}
.dashboard-hero::after {
  content: ""; position: absolute; inset: auto -60px -60px auto;
  width: 260px; height: 260px; border-radius: 50%;
  background: radial-gradient(circle, rgba(255,255,255,0.18), rgba(255,255,255,0));
  animation: dashHalo 8s ease-in-out infinite; pointer-events: none;
}
.dashboard-hero__main, .dashboard-hero__panel { position: relative; z-index: 1; }
.hero-chip { display: inline-flex; padding: 5px 12px; border-radius: 999px; font-size: 12px; letter-spacing: 0.06em; background: rgba(255,255,255,0.14); margin-bottom: 14px; }
.dashboard-hero h1 { margin: 12px 0 0; font-size: 36px; line-height: 1.18; }
.dashboard-hero__panel { display: grid; gap: 12px; padding: 22px; border-radius: 22px; background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.12); backdrop-filter: blur(10px); }
.hero-panel__label { font-size: 13px; color: rgba(255,255,255,0.64); margin-bottom: 2px; }
.hero-panel__item { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.1); cursor: pointer; transition: background .2s; }
.hero-panel__item:last-child { border-bottom: none; }
.hero-panel__item:hover { background: rgba(255,255,255,0.06); border-radius: 8px; padding-left: 8px; padding-right: 8px; }
.hero-panel__item strong { font-size: 14px; font-weight: 600; }
.hero-panel__item span { font-size: 12px; color: rgba(255,255,255,0.6); }
.span-4 { grid-column: span 4; }
.span-8 { grid-column: span 8; }
.profile-summary { position: relative; display: grid; justify-items: center; gap: 12px; padding: 12px 0; text-align: center; overflow: hidden; }
.profile-summary__halo { position: absolute; top: -32px; width: 120px; height: 120px; border-radius: 50%; background: radial-gradient(circle, rgba(20,88,212,.12), rgba(20,88,212,0)); animation: haloFloat 6s ease-in-out infinite; }
.profile-summary h3, .profile-summary p { position: relative; z-index: 1; margin: 0; }
.profile-summary p { color: var(--text-secondary); }
.preference-grid, .dialog-list, .drawer-stack { display: grid; gap: 12px; }
.preference-card { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 16px 18px; border-radius: 18px; border: 1px solid var(--border-soft); background: linear-gradient(180deg, #fff, #f8fbff); cursor: pointer; transition: transform .24s ease, box-shadow .24s ease; }
.preference-card:hover { transform: translateY(-4px); box-shadow: 0 16px 28px rgba(20,33,61,.1); }
.preference-card p { margin: 4px 0 0; color: var(--text-secondary); font-size: 13px; }
.dialog-card { padding: 16px; border-radius: 18px; background: linear-gradient(180deg, #fff, #f8fbff); border: 1px solid var(--border-soft); }
@keyframes dashHalo { 0%,100% { transform: translate3d(0,0,0) scale(1); } 50% { transform: translate3d(-20px,-10px,0) scale(1.08); } }
@keyframes haloFloat { 0%,100% { transform: translate3d(0,0,0) scale(1);} 50% { transform: translate3d(0,10px,0) scale(1.08);} }
@media (max-width: 1200px) { .span-4, .span-8 { grid-column: span 12; } }
@media (max-width: 900px) { .dashboard-hero { grid-template-columns: 1fr; } }
@media (max-width: 760px) { .preference-card { flex-direction: column; align-items: flex-start; } }
</style>

<template>
  <div class="page-shell">
    <section class="admin-hero">
      <div class="admin-hero__main">
        <span class="hero-chip">系统设置</span>
        <h1>预约策略与通知配置</h1>
        <p>统一管理系统运行参数，配置预约策略、通知规则与业务约束条件。</p>
      </div>
      <div class="admin-hero__signal">
        <div class="signal-card">
          <span>策略分组</span><strong>3</strong><small>预约、通知、约束</small>
        </div>
        <div class="signal-card">
          <span>启用通知</span><strong>{{ enabledNotifications }}</strong><small>站内信与邮件提醒</small>
        </div>
      </div>
    </section>

    <section class="grid-cards">
      <el-card class="panel-card span-6">
        <template #header>
          <div class="section-head">
            <h3 class="section-head__title">
              预约策略
            </h3>
          </div>
        </template>
        <div class="setting-stack">
          <div class="setting-item">
            <div>
              <strong>最早可预约时间</strong>
              <p>用于控制用户最远提前预约范围</p>
            </div>
            <el-input v-model="settings.advanceDays" />
          </div>
          <div class="setting-item">
            <div>
              <strong>默认审批模式</strong>
              <p>不同业务可继续扩展为多级审批策略</p>
            </div>
            <el-select v-model="settings.approvalMode">
              <el-option
                label="部门审批"
                value="部门审批"
              />
              <el-option
                label="系统自动通过"
                value="系统自动通过"
              />
            </el-select>
          </div>
          <div class="setting-item setting-item--switch">
            <div>
              <strong>超时自动取消</strong>
              <p>未确认或逾期使用的预约自动释放资源</p>
            </div>
            <el-switch v-model="settings.autoCancel" />
          </div>
        </div>
      </el-card>

      <el-card class="panel-card span-6">
        <template #header>
          <div class="section-head">
            <h3 class="section-head__title">
              通知策略
            </h3>
          </div>
        </template>
        <div class="setting-stack">
          <div class="setting-item setting-item--switch">
            <div>
              <strong>短信提醒</strong>
              <p>未开通短信服务，暂不可用</p>
            </div>
            <el-switch v-model="policy.smsEnabled" disabled />
          </div>
          <div class="setting-item setting-item--switch">
            <div>
              <strong>邮件通知</strong>
              <p>审核通过/拒绝结果邮件；关闭后不再发送</p>
            </div>
            <el-switch v-model="policy.emailEnabled" @change="savePolicy" />
          </div>
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import request from '@/common/utils/request'

const settings = reactive({
  advanceDays: '提前 7 天',
  approvalMode: '部门审批',
  autoCancel: true,
})

// 通知策略（管理端全局，后端 MySQL 存储；邮件开关真实控制审核结果邮件）
const policy = reactive({
  emailEnabled: true,
  smsEnabled: false,
})

onMounted(async () => {
  try {
    const data = await request.get('/admin/settings/notify') as any
    if (data) {
      if (typeof data.emailEnabled === 'boolean') policy.emailEnabled = data.emailEnabled
      if (typeof data.smsEnabled === 'boolean') policy.smsEnabled = data.smsEnabled
    }
  } catch {
    // 加载失败用默认值
  }
})

async function savePolicy() {
  try {
    await request.put('/admin/settings/notify', { ...policy })
  } catch {
    // 保存失败由响应拦截器统一提示
  }
}

const enabledNotifications = computed(() =>
  [policy.emailEnabled].filter(Boolean).length
)
</script>

<style scoped lang="scss">
.admin-hero {
  position: relative; display: grid; grid-template-columns: 1.2fr 0.8fr; gap: 20px;
  padding: 32px; border-radius: 30px; color: #fff;
  background: linear-gradient(135deg, #0f172a, #132949 55%, #1458d4);
  box-shadow: var(--shadow-card); overflow: hidden;
}
.admin-hero::before {
  content:""; position:absolute; inset:0;
  background: radial-gradient(circle at 18% 20%, rgba(255,255,255,.12), transparent 18%),
              linear-gradient(140deg, transparent 14%, rgba(255,255,255,.08) 42%, transparent 72%);
}
.admin-hero::after {
  content:""; position:absolute; inset:-30% -6% auto auto; width:280px; height:280px; border-radius:50%;
  background: radial-gradient(circle, rgba(59,130,246,.24), rgba(59,130,246,0));
  animation: adminGlow 8s ease-in-out infinite; pointer-events:none;
}
.admin-hero__main, .admin-hero__signal { position:relative; z-index:1; }
.hero-chip { display:inline-flex; padding:6px 12px; border-radius:999px; font-size:12px; letter-spacing:.08em; background:rgba(255,255,255,.12); margin-bottom:14px; }
.admin-hero h1 { margin:12px 0 10px; font-size:36px; line-height:1.18; }
.admin-hero p { max-width:740px; margin:0; line-height:1.8; color:rgba(255,255,255,.82); }
.admin-hero__signal { display:grid; gap:12px; }
.signal-card { display:grid; gap:4px; padding:16px 18px; border-radius:16px; background:rgba(255,255,255,.08); border:1px solid rgba(255,255,255,.1); transition:background .2s; }
.signal-card:hover { background:rgba(255,255,255,.14); }
.signal-card span { font-size:13px; color:rgba(255,255,255,.64); }
.signal-card strong { font-size:26px; font-weight:700; }
.signal-card small { font-size:12px; color:rgba(255,255,255,.5); }

@keyframes adminGlow { 0%,100%{ transform:translate3d(0,0,0) scale(1); } 50%{ transform:translate3d(-16px,-8px,0) scale(1.06); } }

.span-6 {
  grid-column: span 6;
}

.setting-stack {
  display: grid;
  gap: 14px;
}

.setting-item {
  display: grid;
  gap: 10px;
  padding: 18px;
  border-radius: 20px;
  border: 1px solid var(--border-soft);
  background: linear-gradient(180deg, #fff, #f8fbff);
  transition: transform 0.24s ease, box-shadow 0.24s ease;
}

.setting-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 18px 28px rgba(20, 33, 61, 0.1);
}

.setting-item p {
  margin: 4px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.setting-item--switch {
  grid-template-columns: 1fr auto;
  align-items: center;
}

@media (max-width: 1200px) {
  .admin-hero {
    grid-template-columns: 1fr;
  }

  .span-6 {
    grid-column: span 12;
  }
}

@media (max-width: 760px) {
  .setting-item--switch {
    grid-template-columns: 1fr;
  }
}
</style>

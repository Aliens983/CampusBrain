<template>
  <div class="page-shell">
    <section class="dashboard-hero">
      <div class="dashboard-hero__main">
        <h1>个人中心</h1>
      </div>
      <div class="dashboard-hero__panel">
        <div class="hero-panel__label">
          快捷入口
        </div>
        <div
          v-if="userStore.isTeacher"
          class="hero-panel__item"
          @click="router.push('/teacher/review')"
        >
          <strong>待我审核</strong><span>学生申请处理 →</span>
        </div>
        <div
          v-else
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

    <!-- 操作区 -->
    <section class="grid-cards">
      <div
        class="action-card span-6"
        @click="preferenceVisible = true"
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
        @click="passwordVisible = true"
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
        v-if="userStore.isTeacher"
        class="action-card span-6"
        @click="router.push('/teacher/review')"
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
        @click="router.push('/bookings')"
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
        @click="handleLogout"
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

    <!-- 通知偏好：点击 action-card 会设 preferenceVisible=true，这里必须有对应弹窗，否则点了没反应 -->
    <el-dialog
      v-model="preferenceVisible"
      title="通知偏好"
      width="520px"
      append-to-body
      :lock-scroll="false"
      :close-on-click-modal="false"
    >
      <div style="display:grid;gap:12px;line-height:1.6">
        <div
          class="notify-pref"
          style="display:flex;align-items:center;justify-content:space-between;gap:16px;padding:14px 16px;border:1px solid var(--border-soft);border-radius:14px"
        >
          <div>
            <strong>邮件通知</strong>
            <p style="margin:4px 0 0;color:var(--text-secondary);font-size:13px">
              审核通过/拒绝结果邮件；需管理员全局邮件策略也开启才生效
            </p>
          </div>
          <el-switch v-model="prefs.emailOn" @change="savePrefs" />
        </div>
        <p style="margin:2px 0 0;color:var(--text-secondary);font-size:12px">
          你的邮件偏好与管理员全局邮件策略都开启时，预约审核结果才会通过邮件通知你；任一关闭即不再发送。
        </p>
      </div>
    </el-dialog>

    <!-- 修改密码：当前登录用户改自己的密码，PUT /users/password -->
    <el-dialog
      v-model="passwordVisible"
      title="修改密码"
      width="460px"
      append-to-body
      :lock-scroll="false"
      :close-on-click-modal="false"
      @closed="resetPwdForm"
    >
      <div class="pwd-banner">
        <div class="pwd-banner__ico">
          <el-icon :size="18"><Lock /></el-icon>
        </div>
        <div class="pwd-banner__txt">
          <b>账号安全</b>
          <span>先验证当前密码，再设置新密码</span>
        </div>
      </div>

      <el-form
        ref="pwdFormRef"
        :model="pwdForm"
        :rules="pwdRules"
        label-position="top"
      >
        <el-form-item
          label="当前密码"
          prop="oldPassword"
        >
          <el-input
            v-model="pwdForm.oldPassword"
            type="password"
            show-password
            size="large"
            placeholder="请输入当前密码"
            autocomplete="current-password"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item
          label="新密码"
          prop="newPassword"
        >
          <el-input
            v-model="pwdForm.newPassword"
            type="password"
            show-password
            size="large"
            placeholder="6 位以上新密码"
            autocomplete="new-password"
          >
            <template #prefix>
              <el-icon><Key /></el-icon>
            </template>
          </el-input>
          <div
            v-if="pwdStrength.cells"
            class="pwd-strength"
          >
            <span class="pwd-strength__cells">
              <i
                v-for="n in 4"
                :key="n"
                :style="n <= pwdStrength.cells ? { background: pwdStrength.color } : {}"
              />
            </span>
            <span
              class="pwd-strength__label"
              :style="{ color: pwdStrength.color }"
            >
              强度{{ pwdStrength.label }}
            </span>
          </div>
        </el-form-item>
        <el-form-item
          label="确认新密码"
          prop="confirmPassword"
        >
          <el-input
            v-model="pwdForm.confirmPassword"
            type="password"
            show-password
            size="large"
            placeholder="再次输入新密码"
            autocomplete="new-password"
            @keyup.enter="submitChangePassword"
          >
            <template #prefix>
              <el-icon><CircleCheck /></el-icon>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button
          class="pwd-btn"
          @click="passwordVisible = false"
        >
          取 消
        </el-button>
        <el-button
          class="pwd-btn pwd-btn--primary"
          type="primary"
          :loading="pwdSubmitting"
          @click="submitChangePassword"
        >
          确认修改
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="quickVisible"
      title="快捷操作"
      size="420px"
      :lock-scroll="false"
    >
      <template v-if="userStore.isTeacher">
        <div class="drawer-stack">
          <el-button
            type="primary"
            @click="router.push('/teacher/review')"
          >
            待我审核
          </el-button>
          <el-button
            plain
            @click="router.push('/teacher/consultations')"
          >
            我的咨询
          </el-button>
          <el-button
            plain
            @click="router.push('/teacher')"
          >
            返回教师端工作台
          </el-button>
        </div>
      </template>
      <div
        v-else
        class="drawer-stack"
      >
        <el-button
          type="primary"
          @click="router.push('/dashboard')"
        >
          返回工作台
        </el-button>
        <el-button
          plain
          @click="router.push('/bookings')"
        >
          我的预约
        </el-button>
        <el-button
          plain
          @click="router.push('/services')"
        >
          服务中心
        </el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, CircleCheck, Key, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/common/stores/user'
import request from '@/common/utils/request'

const router = useRouter()
const userStore = useUserStore()
const preferenceVisible = ref(false)
const quickVisible = ref(false)

// 修改密码（PUT /users/password）
const passwordVisible = ref(false)
const pwdSubmitting = ref(false)
const pwdFormRef = ref<{ validate: () => Promise<unknown>; resetFields: () => void }>()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (e?: Error) => void) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次输入的新密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

// 新密码强度：长度 + 字符构成综合评分，驱动强度条展示
const pwdStrength = computed(() => {
  const v = pwdForm.newPassword
  if (!v) return { cells: 0, label: '', color: '' }
  let cells = 1
  if (v.length >= 8) cells++
  if (/[A-Za-z]/.test(v) && /\d/.test(v)) cells++
  if (/[^A-Za-z0-9]/.test(v)) cells++
  cells = Math.min(cells, 4)
  const map = [
    { label: '弱', color: '#ef4444' },
    { label: '一般', color: '#f97316' },
    { label: '中', color: '#eab308' },
    { label: '强', color: '#22c55e' },
  ]
  return { cells, label: map[cells - 1].label, color: map[cells - 1].color }
})

function resetPwdForm() {
  pwdFormRef.value?.resetFields()
  pwdSubmitting.value = false
}

async function submitChangePassword() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  pwdSubmitting.value = true
  try {
    await request.put('/users/password', {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    ElMessage.success('密码修改成功')
    passwordVisible.value = false
  } catch (err) {
    // 业务码错误：拦截器按「由组件处理」静默 reject(Error(msg))；
    // HTTP 层错误(4xx/5xx)拦截器已弹过提示(isAxiosError=true)，这里不重复弹
    const e = err as { isAxiosError?: boolean; message?: string }
    if (!e?.isAxiosError) {
      ElMessage.error(e?.message || '密码修改失败，请稍后重试')
    }
  } finally {
    pwdSubmitting.value = false
  }
}

// 个人通知偏好（与后端 /users/me/notify 同步）
const prefs = reactive({ emailOn: true })

async function loadPrefs() {
  try {
    const d = await request.get('/users/me/notify') as any
    if (d && typeof d.emailOn === 'boolean') {
      prefs.emailOn = d.emailOn
    }
  } catch {
    // 加载失败用默认值
  }
}

async function savePrefs() {
  try {
    await request.put('/users/me/notify', { ...prefs })
  } catch {
    // 保存失败由响应拦截器统一提示
  }
}

onMounted(loadPrefs)

const user = computed(() => userStore.userInfo)

const roleLabel = computed(() => {
  const r = user.value?.role
  if (r === 'super_admin') return '超级管理员'
  if (r === 'admin') return '管理员'
  if (r === 'teacher') return '教师'
  return '普通用户'
})

const roleTagType = computed(() => {
  const r = user.value?.role
  if (r === 'super_admin' || r === 'admin') return 'danger'
  if (r === 'teacher') return 'warning'
  return 'success'
})

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped lang="scss">
.dashboard-hero {
  position: relative; display: grid; grid-template-columns: 1.2fr 0.8fr; gap: 20px;
  padding: 32px; border-radius: 30px; color: #fff;
  background: linear-gradient(135deg, #0E6CD6, #3FB6FF 62%, #ADE2FF);
  box-shadow: var(--shadow-card); overflow: hidden;
}
.dashboard-hero::before {
  content:""; position:absolute; inset:0;
  background: radial-gradient(circle at 20% 20%, rgba(255,255,255,.16), transparent 22%),
              linear-gradient(120deg, transparent 14%, rgba(255,255,255,.08) 36%, transparent 62%);
}
.dashboard-hero::after {
  content:""; position:absolute; inset:auto -60px -60px auto;
  width:260px; height:260px; border-radius:50%;
  background: radial-gradient(circle, rgba(255,255,255,.18), rgba(255,255,255,0));
  animation: dashHalo 8s ease-in-out infinite; pointer-events:none;
}
.dashboard-hero__main, .dashboard-hero__panel { position:relative; z-index:1; }
.hero-chip { display:inline-flex; padding:5px 12px; border-radius:999px; font-size:12px; letter-spacing:.06em; background:rgba(255,255,255,.14); margin-bottom:14px; }
.dashboard-hero__main h1 { margin:12px 0 0; font-size:36px; line-height:1.18; }
.dashboard-hero__panel { display:grid; gap:12px; padding:22px; border-radius:22px; background:rgba(255,255,255,.1); border:1px solid rgba(255,255,255,.12); backdrop-filter:blur(10px); }
.hero-panel__label { font-size:13px; color:rgba(255,255,255,.64); margin-bottom:2px; }
.hero-panel__item { display:flex; justify-content:space-between; align-items:center; padding:10px 0; border-bottom:1px solid rgba(255,255,255,.1); cursor:pointer; }
.hero-panel__item:last-child { border-bottom:none; }
.hero-panel__item strong { font-size:14px; font-weight:600; }
.hero-panel__item span { font-size:12px; color:rgba(255,255,255,.6); }

.span-5 { grid-column: span 5; }
.span-6 { grid-column: span 6; }
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

.drawer-stack { display: grid; gap: 12px; }

/* 修改密码弹窗美化（append-to-body 但元素均在本模板内，scoped 即可命中，不外溢） */
.pwd-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: -2px 0 18px;
  padding: 12px 14px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(14, 108, 214, 0.08), rgba(63, 182, 255, 0.14) 75%);
}
.pwd-banner__ico {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 11px;
  color: #fff;
  background: linear-gradient(135deg, #0e6cd6, #3fb6ff);
  box-shadow: 0 4px 12px rgba(62, 175, 255, 0.32);
  flex-shrink: 0;
}
.pwd-banner__txt {
  display: grid;
  gap: 2px;
}
.pwd-banner__txt b {
  font-size: 14px;
  color: var(--text-primary);
}
.pwd-banner__txt span {
  font-size: 12px;
  color: var(--text-secondary);
}

.pwd-strength {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}
.pwd-strength__cells {
  display: flex;
  flex: 1;
  gap: 6px;
}
.pwd-strength__cells i {
  flex: 1;
  height: 4px;
  border-radius: 4px;
  background: var(--border-soft);
  transition: background 0.25s ease;
}
.pwd-strength__label {
  width: 52px;
  font-size: 12px;
  text-align: right;
}

.pwd-btn {
  padding: 8px 22px;
  border-radius: 10px;
  font-weight: 500;
}
.pwd-btn--primary {
  border: none;
  background: linear-gradient(135deg, #0e6cd6, #3fb6ff);
  box-shadow: 0 6px 14px rgba(62, 175, 255, 0.35);
}
.pwd-btn--primary:hover,
.pwd-btn--primary:focus {
  border: none;
  background: linear-gradient(135deg, #0b5ec0, #2ea6f0);
}

@keyframes dashHalo { 0%,100%{ transform:translate3d(0,0,0) scale(1); } 50%{ transform:translate3d(-20px,-10px,0) scale(1.08); } }

@media (max-width: 1200px) { .span-5, .span-7, .span-6 { grid-column: span 12; } }
@media (max-width: 900px) { .dashboard-hero { grid-template-columns: 1fr; } .info-grid { grid-template-columns: 1fr; } }
@media (max-width: 500px) { .profile-card { flex-direction: column; text-align: center; } }
</style>

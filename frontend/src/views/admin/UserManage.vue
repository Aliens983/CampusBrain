<template>
  <div class="page-shell">
    <section class="admin-hero">
      <div class="admin-hero__main">
        <h1>用户与权限</h1>
      </div>
      <div class="admin-hero__signal">
        <div class="signal-card">
          <span>用户总数</span><strong>{{ tableData.length }}</strong><small>所有已注册账号</small>
        </div>
        <div class="signal-card">
          <span>管理员</span><strong>{{ adminCount }}</strong><small>含超级管理员</small>
        </div>
      </div>
    </section>

    <el-card class="panel-card">
      <template #header>
        <div class="section-head">
          <h3 class="section-head__title">
            用户列表
          </h3>
          <el-input
            v-model="keyword"
            placeholder="搜索用户名、部门或邮箱"
            clearable
            style="width: 280px"
          />
        </div>
      </template>

      <div class="user-stack">
        <article
          v-for="item in filteredUsers"
          :key="item.id"
          class="user-item"
        >
          <div class="user-item__avatar">
            {{ item.username.slice(0, 1) }}
          </div>
          <div class="user-item__main">
            <div class="user-item__head">
              <div>
                <strong>{{ item.username }}</strong>
                <p>{{ item.department }}</p>
              </div>
              <el-tag :type="item.role === 'super_admin' ? 'danger' : item.role === 'admin' ? 'warning' : 'info'">
                {{ roleText(item.role) }}
              </el-tag>
            </div>
            <div class="user-item__meta">
              <span>{{ item.email }}</span>
              <span>{{ item.phone }}</span>
              <span>状态：启用</span>
            </div>
          </div>
          <div class="user-item__action">
            <el-button
              plain
              @click="selectedUser = item"
            >
              分配角色
            </el-button>
            <el-button
              type="primary"
              @click="detailUser(item)"
            >
              查看详情
            </el-button>
          </div>
        </article>
      </div>
    </el-card>

    <el-dialog
      v-if="overviewVisible"
      :model-value="true"
      :title="overviewTitle"
      width="560px"
      @close="overviewVisible = false"
    >
      <div class="dialog-list">
        <div
          v-for="item in overviewItems"
          :key="item"
          class="dialog-card"
        >
          {{ item }}
        </div>
      </div>
    </el-dialog>

    <el-drawer
      v-model="userDrawerVisible"
      title="角色分配"
      size="420px"
    >
      <template v-if="selectedUser">
        <div class="drawer-stack">
          <div>
            <h3>{{ selectedUser.username }}</h3>
            <p class="muted">
              {{ selectedUser.department }}
            </p>
          </div>
          <el-select
            v-model="selectedRole"
            style="width: 100%"
          >
            <el-option
              label="普通用户"
              value="user"
            />
            <el-option
              label="管理员"
              value="admin"
            />
          </el-select>
          <el-button
            type="primary"
            style="width: 100%"
            :loading="roleSaving"
            @click="saveRole"
          >
            保存角色
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/common/utils/request'
import { fetchAdminUsers } from '@/services/campus'
import type { UserInfo, UserRole } from '@/types'

const keyword = ref('')
const overviewVisible = ref(false)
const overviewTitle = ref('')
const overviewItems = ref<string[]>([])
const selectedUser = ref<UserInfo | null>(null)
const selectedRole = ref<UserRole>('user')
const tableData = ref<UserInfo[]>([])
const loading = ref(false)
const roleSaving = ref(false)

const filteredUsers = computed(() =>
  tableData.value.filter((item) => [item.username, item.department, item.email].join('|').toLowerCase().includes(keyword.value.toLowerCase())),
)
const adminCount = computed(() => tableData.value.filter((item) => item.role === 'admin' || item.role === 'super_admin').length)
const userDrawerVisible = computed({
  get: () => Boolean(selectedUser.value),
  set: (value: boolean) => {
    if (!value) {
      selectedUser.value = null
      selectedRole.value = 'user'
    }
  },
})

// 打开抽屉时初始化角色选择
watch(selectedUser, (user) => {
  if (user) {
    selectedRole.value = user.role === 'super_admin' || user.role === 'admin' ? 'admin' : 'user'
  }
})

onMounted(async () => {
  loading.value = true
  try {
    tableData.value = await fetchAdminUsers()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '获取用户列表失败')
  } finally {
    loading.value = false
  }
})

function detailUser(item: UserInfo) {
  overviewTitle.value = `${item.username} 账号详情`
  overviewItems.value = [`邮箱：${item.email}`, `电话：${item.phone}`, `角色：${roleText(item.role)}`]
  overviewVisible.value = true
}

async function saveRole() {
  if (!selectedUser.value) return
  roleSaving.value = true
  try {
    const newRole = selectedRole.value === 'admin' ? 1 : 0
    const newRoleLabel = selectedRole.value === 'admin' ? 'admin' : 'user'
    await request.put('/admin/users/role', {
      userId: selectedUser.value.id,
      role: newRole,
    })
    ElMessage.success(`已将 ${selectedUser.value.username} 的角色修改为 ${selectedRole.value === 'admin' ? '管理员' : '普通用户'}`)
    // 先关闭抽屉，再本地更新数据避免闪烁
    const updated = selectedUser.value
    selectedUser.value = null
    selectedRole.value = 'user'
    const idx = tableData.value.findIndex(u => u.id === updated.id)
    if (idx !== -1) {
      tableData.value[idx] = { ...tableData.value[idx], role: newRoleLabel as UserRole }
    }
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '修改角色失败')
  } finally {
    roleSaving.value = false
  }
}

function roleText(role: UserRole) {
  const map: Record<UserRole, string> = {
    user: '普通用户',
    admin: '管理员',
    super_admin: '超级管理员',
  }
  return map[role] || '未知'
}
</script>

<style scoped lang="scss">
.admin-hero {
  position: relative; display: grid; grid-template-columns: 1.2fr 0.8fr; gap: 20px;
  padding: 32px; border-radius: 30px; color: #fff;
  background: linear-gradient(135deg, #0f172a, #132949 55%, #7c3aed);
  box-shadow: var(--shadow-card); overflow: hidden;
}
.admin-hero::before {
  content:""; position:absolute; inset:0;
  background: radial-gradient(circle at 18% 20%, rgba(255,255,255,.12), transparent 18%),
              linear-gradient(140deg, transparent 14%, rgba(255,255,255,.08) 42%, transparent 72%);
}
.admin-hero::after {
  content:""; position:absolute; inset:-30% -6% auto auto; width:280px; height:280px; border-radius:50%;
  background: radial-gradient(circle, rgba(139,92,246,.24), rgba(139,92,246,0));
  animation: adminGlow 8s ease-in-out infinite; pointer-events:none;
}
.admin-hero__main, .admin-hero__signal { position:relative; z-index:1; }
.hero-chip { display:inline-flex; padding:6px 12px; border-radius:999px; font-size:12px; letter-spacing:.08em; background:rgba(255,255,255,.12); margin-bottom:14px; }
.admin-hero h1 { margin:12px 0 10px; font-size:36px; line-height:1.18; }
.admin-hero p { max-width:740px; margin:0; line-height:1.8; color:rgba(255,255,255,.82); }
.admin-hero__signal { display:grid; gap:12px; }
.signal-card { display:grid; gap:4px; padding:16px 18px; border-radius:16px; background:rgba(255,255,255,.08); border:1px solid rgba(255,255,255,.1); cursor:pointer; transition:background .2s; }
.signal-card:hover { background:rgba(255,255,255,.14); }
.signal-card span { font-size:13px; color:rgba(255,255,255,.64); }
.signal-card strong { font-size:26px; font-weight:700; }
.signal-card small { font-size:12px; color:rgba(255,255,255,.5); }

@keyframes adminGlow { 0%,100%{ transform:translate3d(0,0,0) scale(1); } 50%{ transform:translate3d(-16px,-8px,0) scale(1.06); } }
.user-stack, .dialog-list, .drawer-stack { display: grid; gap: 14px; }
.user-item { display: grid; grid-template-columns: auto 1fr auto; gap: 16px; padding: 18px; border-radius: 20px; border: 1px solid var(--border-soft); background: linear-gradient(180deg, #fff, #fbf9ff); transition: transform .24s ease, box-shadow .24s ease, border-color .24s ease; }
.user-item:hover { transform: translateY(-4px); box-shadow: 0 18px 28px rgba(20,33,61,.1); border-color: rgba(124,58,237,.14); }
.user-item__avatar { width: 56px; height: 56px; display: grid; place-items: center; border-radius: 18px; background: linear-gradient(135deg, #7c3aed, #a78bfa); color: #fff; font-size: 22px; font-weight: 700; }
.user-item__main { display: grid; gap: 10px; }
.user-item__head { display: flex; justify-content: space-between; gap: 12px; }
.user-item__head p { margin: 4px 0 0; color: var(--text-tertiary); font-size: 12px; }
.user-item__meta { display: flex; flex-wrap: wrap; gap: 12px; color: var(--text-secondary); font-size: 13px; }
.user-item__action { display: flex; align-items: center; gap: 10px; }
.dialog-card { padding: 16px; border-radius: 18px; background: linear-gradient(180deg, #fff, #fbf9ff); border: 1px solid var(--border-soft); }
@media (max-width: 960px) { .admin-hero { grid-template-columns: 1fr; } .user-item, .user-item__head, .user-item__action { display: flex; flex-direction: column; align-items: stretch; } }
</style>

<template>
  <div class="page-shell">
    <section class="admin-hero">
      <div class="admin-hero__main">
        <p class="hz-eyebrow">
          Users & roles · HZNU
        </p>
        <h1>用户与权限</h1>
        <p class="admin-hero__desc">
          管理校园账号、角色与访问权限，保持身份体系清晰可靠。
        </p>
      </div>
      <div class="admin-hero__signal">
        <div class="signal-card">
          <span>用户总数</span><strong>{{ totalUsers }}</strong><small>所有已注册账号</small>
        </div>
        <div class="signal-card">
          <span>管理员</span><strong>{{ adminTotal }}</strong><small>含超级管理员</small>
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
            placeholder="搜索用户名"
            clearable
            style="width: 280px"
            @keyup.enter="onSearch"
            @clear="onSearch"
          />
        </div>
      </template>

      <div class="user-stack">
        <article
          v-for="item in tableData"
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

        <el-empty
          v-if="!loading && tableData.length === 0"
          description="没有符合条件的用户"
        />
      </div>

      <el-pagination
        v-if="total > 0"
        class="list-pagination"
        background
        :current-page="pageNo"
        :page-size="pageSize"
        :page-sizes="[5, 10, 20]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @current-change="onPageChange"
        @size-change="onPageSizeChange"
      />
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
              label="教师（咨询师账号）"
              value="teacher"
            />
            <el-option
              label="管理员"
              value="admin"
            />
          </el-select>
          <p
            v-if="selectedRole === 'teacher'"
            class="field-tip"
          >
            设为教师后，需把该账号绑定到对应咨询师（consultant.user_id）才会出现在“待我审核”里。
          </p>
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
import { fetchAdminUsersPage, fetchAdminUserCounts } from '@/common/campus'
import type { UserInfo, UserRole } from '@/common/types'

const keyword = ref('')
const overviewVisible = ref(false)
const overviewTitle = ref('')
const overviewItems = ref<string[]>([])
const selectedUser = ref<UserInfo | null>(null)
const selectedRole = ref<UserRole>('user')
// 当前服务端返回的一页用户（不再前端过滤/切片）
const tableData = ref<UserInfo[]>([])
// 当前查询（含搜索）命中总数，用于分页
const total = ref(0)
// 顶部两张统计卡：与当前筛选页解耦
const totalUsers = ref(0)
const adminTotal = ref(0)
const loading = ref(false)
const roleSaving = ref(false)

// 列表分页：默认每页 5 条，可切换 5 / 10 / 20
const pageNo = ref(1)
const pageSize = ref(5)

// 2.2.3：分页 + 用户名搜索全部下沉到服务端
async function loadUsers() {
  loading.value = true
  try {
    const page = await fetchAdminUsersPage({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      name: keyword.value,
    })
    tableData.value = page.records
    total.value = page.total
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '获取用户列表失败')
  } finally {
    loading.value = false
  }
}

async function loadCounts() {
  try {
    const counts = await fetchAdminUserCounts()
    totalUsers.value = counts.total
    adminTotal.value = counts.admin
  } catch {
    // 统计卡失败不阻塞列表
  }
}

async function onPageChange(page: number) {
  pageNo.value = page
  await loadUsers()
}
async function onPageSizeChange(size: number) {
  pageSize.value = size
  pageNo.value = 1
  await loadUsers()
}
async function onSearch() {
  pageNo.value = 1
  await loadUsers()
}
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
    selectedRole.value =
      user.role === 'super_admin' || user.role === 'admin' ? 'admin' : user.role === 'teacher' ? 'teacher' : 'user'
  }
})

onMounted(async () => {
  await Promise.all([loadUsers(), loadCounts()])
})

// 修改角色后：重取当前页与统计卡（角色变化会影响管理员计数）
async function reloadAfterRoleChange() {
  await Promise.all([loadUsers(), loadCounts()])
}

function detailUser(item: UserInfo) {
  overviewTitle.value = `${item.username} 账号详情`
  overviewItems.value = [`邮箱：${item.email}`, `电话：${item.phone}`, `角色：${roleText(item.role)}`]
  overviewVisible.value = true
}

async function saveRole() {
  if (!selectedUser.value) return
  roleSaving.value = true
  try {
    const newRole = selectedRole.value === 'admin' ? 1 : selectedRole.value === 'teacher' ? 3 : 0
    await request.put('/admin/users/role', {
      userId: selectedUser.value.id,
      role: newRole,
    })
    const label = selectedRole.value === 'admin' ? '管理员' : selectedRole.value === 'teacher' ? '教师' : '普通用户'
    ElMessage.success(`已将 ${selectedUser.value.username} 的角色修改为 ${label}`)
    // 先关闭抽屉，再从服务端重取当前页与统计卡
    selectedUser.value = null
    selectedRole.value = 'user'
    await reloadAfterRoleChange()
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
    teacher: '教师',
    admin: '管理员',
    super_admin: '超级管理员',
  }
  return map[role] || '未知'
}
</script>

<style scoped lang="scss">
.field-tip {
  margin-top: 6px;
  font-size: 11px;
  line-height: 1.6;
  color: var(--ink-4);
}

.user-stack,
.dialog-list,
.drawer-stack {
  display: grid;
  gap: 12px;
}

.list-pagination {
  display: flex;
  justify-content: flex-start;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--line);
}

.user-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 16px;
  padding: 16px 18px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface-2);
  transition:
    transform 0.18s var(--ease-out),
    border-color 0.18s ease,
    background-color 0.18s ease,
    box-shadow 0.18s ease;
}

.user-item:hover {
  transform: translateY(-2px);
  border-color: rgba(176, 138, 69, 0.42);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.user-item__avatar {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--paper-2);
  color: var(--lake);
  font-family: var(--font-serif);
  font-size: 20px;
  font-weight: 600;
}

.user-item__main {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.user-item__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.user-item__head strong {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}

.user-item__head p {
  margin: 4px 0 0;
  color: var(--ink-4);
  font-size: 11.5px;
}

.user-item__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: var(--ink-3);
  font-size: 12px;
}

.user-item__action {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dialog-card {
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface-2);
}

@media (max-width: 960px) {
  .user-item,
  .user-item__head,
  .user-item__action {
    display: flex;
    flex-direction: column;
    align-items: stretch;
  }
}
</style>

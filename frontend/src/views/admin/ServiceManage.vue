<template>
  <div class="page-shell">
    <section class="admin-hero">
      <div class="admin-hero__main">
        <h1>服务治理</h1>
        <div class="hero-actions">
          <el-button
            type="primary"
            size="large"
            @click="createDrawer = true"
          >
            新增服务
          </el-button>
        </div>
      </div>
      <div class="admin-hero__signal">
        <div class="signal-card">
          <span>服务总数</span><strong>{{ services.length }}</strong><small>已纳入统一管理</small>
        </div>
        <div class="signal-card">
          <span>可用服务</span><strong>{{ availableCount }}</strong><small>当前可开放预约</small>
        </div>
      </div>
    </section>

    <el-card class="panel-card">
      <template #header>
        <div class="section-head">
          <h3 class="section-head__title">
            服务列表
          </h3>
          <div class="toolbar">
            <el-input
              v-model="keyword"
              placeholder="搜索服务名称或分类"
              clearable
            />
            <el-select
              v-model="statusFilter"
              style="width: 150px"
            >
              <el-option
                label="全部状态"
                value=""
              />
              <el-option
                label="可用"
                value="available"
              />
              <el-option
                label="维护中"
                value="maintenance"
              />
            </el-select>
          </div>
        </div>
      </template>

      <div class="service-stack">
        <article
          v-for="item in filteredServices"
          :key="item.id"
          class="service-item"
        >
          <div
            class="service-item__cover"
            :class="item.image"
          >
            {{ item.code }}
          </div>
          <div class="service-item__main">
            <div class="service-item__head">
              <div>
                <strong>{{ item.name }}</strong>
                <p>{{ item.category }} / {{ item.location }}</p>
              </div>
              <el-tag :type="item.status === 'available' ? 'success' : 'warning'">
                {{ item.status === 'available' ? '可用' : '维护中' }}
              </el-tag>
            </div>
            <div class="service-item__meta">
              <span>{{ item.priceLabel }}</span>
              <span>{{ item.description }}</span>
            </div>
          </div>
          <div class="service-item__action">
            <el-button
              plain
              @click="selectedService = item"
            >
              编辑
            </el-button>
            <el-button
              type="primary"
              @click="openAccess(item)"
            >
              查看接入
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
      v-model="serviceDrawerVisible"
      title="服务编辑"
      size="460px"
    >
      <template v-if="selectedService">
        <el-form label-position="top">
          <el-form-item label="服务名称">
            <el-input v-model="editForm.name" />
          </el-form-item>
          <el-form-item label="服务分类">
            <el-input v-model="editForm.category" />
          </el-form-item>
          <el-form-item label="服务说明">
            <el-input
              v-model="editForm.description"
              type="textarea"
              :rows="5"
            />
          </el-form-item>
        </el-form>
        <el-button
          type="primary"
          style="width: 100%"
          :loading="saving"
          @click="saveEdit"
        >
          保存修改
        </el-button>
      </template>
    </el-drawer>

    <el-drawer
      v-model="createDrawer"
      title="新增服务"
      size="480px"
    >
      <el-form label-position="top">
        <el-form-item label="服务名称">
          <el-input
            v-model="createForm.name"
            placeholder="请输入服务名称"
          />
        </el-form-item>
        <el-form-item label="服务分类">
          <el-input
            v-model="createForm.category"
            placeholder="如：空间资源、设备资源、咨询服务"
          />
        </el-form-item>
        <el-form-item label="服务说明">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="5"
            placeholder="请输入服务说明"
          />
        </el-form-item>
        <el-form-item label="开放范围">
          <el-input
            v-model="createForm.location"
            placeholder="如：全校师生"
          />
        </el-form-item>
      </el-form>
      <el-button
        type="primary"
        style="width: 100%"
        :loading="saving"
        @click="saveCreate"
      >
        保存
      </el-button>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/common/utils/request'
import { fetchServiceCards } from '@/services/campus'
import type { ServiceCard } from '@/types'

const createDrawer = ref(false)
const keyword = ref('')
const statusFilter = ref('')
const overviewVisible = ref(false)
const overviewTitle = ref('')
const overviewItems = ref<string[]>([])
const selectedService = ref<ServiceCard | null>(null)
const services = ref<ServiceCard[]>([])
const loading = ref(false)
const saving = ref(false)

const editForm = reactive({ name: '', category: '', description: '' })
const createForm = reactive({ name: '', category: '', description: '', location: '' })

const filteredServices = computed(() =>
  services.value.filter((item) => {
    const matchKeyword = !keyword.value || [item.name, item.category, item.location].join('|').toLowerCase().includes(keyword.value.toLowerCase())
    const matchStatus = !statusFilter.value || item.status === statusFilter.value
    return matchKeyword && matchStatus
  }),
)
const availableCount = computed(() => services.value.filter((item) => item.status === 'available').length)
const serviceDrawerVisible = computed({
  get: () => Boolean(selectedService.value),
  set: (value: boolean) => {
    if (!value) selectedService.value = null
  },
})

// 打开编辑抽屉时自动填充当前服务数据
watch(selectedService, (item) => {
  if (item) {
    editForm.name = item.name
    editForm.category = item.category
    editForm.description = item.description
  }
})

onMounted(async () => {
  loading.value = true
  try {
    services.value = await fetchServiceCards()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '获取服务列表失败')
  } finally {
    loading.value = false
  }
})

function openAccess(item: ServiceCard) {
  overviewTitle.value = `${item.name} 接入说明`
  overviewItems.value = [
    `业务分类：${item.category}`,
    `开放范围：${item.location}`,
  ]
  overviewVisible.value = true
}

async function saveEdit() {
  if (!selectedService.value) return
  saving.value = true
  try {
    await request.put(`/admin/services/${selectedService.value.id}`, {
      serviceName: editForm.name,
      serviceDescribe: editForm.description,
    })
    ElMessage.success('服务修改成功')
    // 先关闭抽屉，再本地更新数据避免闪烁
    const updated = selectedService.value
    selectedService.value = null
    const idx = services.value.findIndex(s => s.id === updated.id)
    if (idx !== -1) {
      services.value[idx] = { ...services.value[idx], name: editForm.name, description: editForm.description }
    }
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '修改失败')
  } finally {
    saving.value = false
  }
}

async function saveCreate() {
  if (!createForm.name) {
    ElMessage.warning('请输入服务名称')
    return
  }
  saving.value = true
  try {
    await request.post('/admin/services', {
      serviceName: createForm.name,
      serviceDescribe: createForm.description,
    })
    ElMessage.success('服务创建成功')
    createDrawer.value = false
    createForm.name = ''
    createForm.category = ''
    createForm.description = ''
    createForm.location = ''
    services.value = await fetchServiceCards()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '创建失败')
  } finally {
    saving.value = false
  }
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
.hero-actions { display:flex; gap:12px; margin-top:22px; }
.admin-hero__signal { display:grid; gap:12px; }
.signal-card { display:grid; gap:4px; padding:16px 18px; border-radius:16px; background:rgba(255,255,255,.08); border:1px solid rgba(255,255,255,.1); cursor:pointer; transition:background .2s; }
.signal-card:hover { background:rgba(255,255,255,.14); }
.signal-card span { font-size:13px; color:rgba(255,255,255,.64); }
.signal-card strong { font-size:26px; font-weight:700; }
.signal-card small { font-size:12px; color:rgba(255,255,255,.5); }

@keyframes adminGlow { 0%,100%{ transform:translate3d(0,0,0) scale(1); } 50%{ transform:translate3d(-16px,-8px,0) scale(1.06); } }
.toolbar { display: flex; gap: 12px; }
.service-stack, .dialog-list { display: grid; gap: 14px; }
.service-item { display: grid; grid-template-columns: auto 1fr auto; gap: 16px; padding: 18px; border-radius: 20px; border: 1px solid var(--border-soft); background: linear-gradient(180deg, #fff, #fbf9ff); transition: transform .24s ease, box-shadow .24s ease, border-color .24s ease; }
.service-item:hover { transform: translateY(-4px); box-shadow: 0 18px 28px rgba(20,33,61,.1); border-color: rgba(124,58,237,.14); }
.service-item__cover { width: 72px; min-height: 72px; display: grid; place-items: center; border-radius: 18px; color: #fff; font-weight: 700; }
.service-item__main { display: grid; gap: 10px; }
.service-item__head { display: flex; justify-content: space-between; gap: 12px; }
.service-item__head p { margin: 4px 0 0; color: var(--text-tertiary); font-size: 12px; }
.service-item__meta { display: grid; gap: 6px; color: var(--text-secondary); font-size: 13px; }
.service-item__action { display: flex; align-items: center; gap: 10px; }
.dialog-card { padding: 16px; border-radius: 18px; background: linear-gradient(180deg, #fff, #fbf9ff); border: 1px solid var(--border-soft); }
@media (max-width: 960px) { .admin-hero { grid-template-columns: 1fr; } .toolbar, .service-item, .service-item__head, .service-item__action { display: flex; flex-direction: column; align-items: stretch; } }
</style>

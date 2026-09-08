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
          <img
            v-if="item.imageUrl"
            :src="assetUrl(item.imageUrl)"
            class="service-item__cover-img"
            alt="封面"
          >
          <div
            v-else
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
          </div>
        </article>
      </div>
    </el-card>

    <el-drawer
      v-model="serviceDrawerVisible"
      title="服务编辑"
      size="460px"
      class="svc-drawer"
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
          <el-form-item label="服务封面">
            <el-upload
              :show-file-list="false"
              accept="image/*"
              :http-request="(o: any) => uploadImage(o.file, 'edit')"
            >
              <img
                v-if="editForm.image"
                :src="assetUrl(editForm.image)"
                class="cover-prev"
                alt="封面"
              >
              <el-button
                v-else
                size="small"
              >上传封面图片</el-button>
            </el-upload>
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
      class="svc-drawer"
    >
      <el-form label-position="top">
        <el-form-item label="服务名称">
          <el-input
            v-model="createForm.name"
            placeholder="请输入服务名称"
          />
        </el-form-item>
        <el-form-item label="服务分类">
          <el-select
            v-model="createForm.categoryId"
            style="width: 100%"
            placeholder="请选择业务分类"
          >
            <el-option
              v-for="opt in categories"
              :key="opt.id"
              :label="opt.name"
              :value="opt.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所属校区">
          <el-select
            v-model="createForm.campus"
            style="width: 100%"
          >
            <el-option
              label="仓前校区"
              value="cq"
            />
            <el-option
              label="下沙校区"
              value="xs"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="预约容量">
          <el-input-number
            v-model="createForm.capacity"
            :min="-1"
            :step="1"
            style="width: 100%"
          />
          <div class="field-tip">
            -1 表示不限名额；活动/教室等先到先得类建议设具体人数。
          </div>
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
        <el-form-item label="服务封面">
          <el-upload
            :show-file-list="false"
            accept="image/*"
            :http-request="(o: any) => uploadImage(o.file, 'create')"
          >
            <img
              v-if="createForm.image"
              :src="assetUrl(createForm.image)"
              class="cover-prev"
              alt="封面"
            >
            <el-button
              v-else
              size="small"
            >上传封面图片</el-button>
          </el-upload>
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
import { fetchServiceCards, fetchServiceCategories, type ServiceCategoryOption } from '@/services/campus'
import type { ServiceCard } from '@/types'

const createDrawer = ref(false)
const keyword = ref('')
const statusFilter = ref('')
const selectedService = ref<ServiceCard | null>(null)
const services = ref<ServiceCard[]>([])
const categories = ref<ServiceCategoryOption[]>([])
const loading = ref(false)
const saving = ref(false)

const editForm = reactive({ name: '', category: '', categoryId: 0, description: '', image: '' })
const createForm = reactive({ name: '', categoryId: 0, campus: 'cq', capacity: -1, description: '', location: '', image: '' })

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
    editForm.categoryId = item.categoryId ?? 0
    editForm.description = item.description
    editForm.image = item.imageUrl || ''
  }
})

/** /uploads/xx → /api/uploads/xx（走 vite 代理到网关） */
function assetUrl(path?: string) {
  if (!path) return ''
  if (/^https?:/.test(path)) return path
  if (path.startsWith('/uploads')) return `/api${path}`
  return path
}

/** 封面上传：POST /admin/files，返回相对 URL */
async function uploadImage(file: File, kind: 'edit' | 'create') {
  if (!file) return
  try {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('subDir', 'service')
    const url = await request.post('/admin/files', fd) as string
    if (kind === 'edit') editForm.image = url
    else createForm.image = url
    ElMessage.success('封面上传成功')
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '封面上传失败')
  }
}

onMounted(async () => {
  loading.value = true
  try {
    // 分类字典来自后端 service_category，默认选中「教室空间」
    categories.value = await fetchServiceCategories()
    const space = categories.value.find((c) => c.code === 'space')
    createForm.categoryId = space?.id ?? categories.value[0]?.id ?? 0
    services.value = await fetchServiceCards()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '获取服务列表失败')
  } finally {
    loading.value = false
  }
})

async function saveEdit() {
  if (!selectedService.value) return
  saving.value = true
  try {
    await request.put(`/admin/services/${selectedService.value.id}`, {
      serviceName: editForm.name,
      serviceDescribe: editForm.description,
      imageUrl: editForm.image || null,
      categoryId: editForm.categoryId || selectedService.value.categoryId || 0,
    })
    ElMessage.success('服务修改成功')
    // 先关闭抽屉，再本地更新数据避免闪烁
    const updated = selectedService.value
    selectedService.value = null
    const idx = services.value.findIndex(s => s.id === updated.id)
    if (idx !== -1) {
      services.value[idx] = {
        ...services.value[idx],
        name: editForm.name,
        description: editForm.description,
        imageUrl: editForm.image || services.value[idx].imageUrl || '',
      }
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
  if (!createForm.categoryId) {
    ElMessage.warning('请选择业务分类')
    return
  }
  saving.value = true
  try {
    await request.post('/admin/services', {
      serviceName: createForm.name,
      serviceDescribe: createForm.description,
      imageUrl: createForm.image || null,
      categoryId: createForm.categoryId,
      campus: createForm.campus,
      capacity: createForm.capacity,
    })
    ElMessage.success('服务创建成功')
    createDrawer.value = false
    const space = categories.value.find((c) => c.code === 'space')
    createForm.name = ''
    createForm.categoryId = space?.id ?? categories.value[0]?.id ?? 0
    createForm.campus = 'cq'
    createForm.capacity = -1
    createForm.description = ''
    createForm.location = ''
    createForm.image = ''
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
  background: linear-gradient(135deg, #0f172a, #132949 55%, #3FB6FF);
  box-shadow: var(--shadow-card); overflow: hidden;
}
.admin-hero::before {
  content:""; position:absolute; inset:0;
  background: radial-gradient(circle at 18% 20%, rgba(255,255,255,.12), transparent 18%),
              linear-gradient(140deg, transparent 14%, rgba(255,255,255,.08) 42%, transparent 72%);
}
.admin-hero::after {
  content:""; position:absolute; inset:-30% -6% auto auto; width:280px; height:280px; border-radius:50%;
  background: radial-gradient(circle, rgba(123,208,255,.24), rgba(123,208,255,0));
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
.cover-prev { display: block; width: 100%; max-height: 150px; object-fit: cover; border-radius: 12px; border: 1px solid var(--border-soft); }
.field-tip { margin-top: 4px; font-size: 12px; line-height: 1.6; color: var(--text-tertiary); }
.service-stack, .dialog-list { display: grid; gap: 14px; }
.service-item { display: grid; grid-template-columns: auto 1fr auto; gap: 16px; padding: 18px; border-radius: 20px; border: 1px solid var(--border-soft); background: linear-gradient(180deg, #fff, #F9FCFF); transition: transform .24s ease, box-shadow .24s ease, border-color .24s ease; }
.service-item:hover { transform: translateY(-4px); box-shadow: 0 18px 28px rgba(20,33,61,.1); border-color: rgba(63,182,255,.14); }
.service-item__cover { width: 72px; min-height: 72px; display: grid; place-items: center; border-radius: 18px; color: #fff; font-weight: 700; }
.service-item__cover-img { width: 72px; height: 72px; object-fit: cover; border-radius: 18px; border: 1px solid var(--border-soft); flex-shrink: 0; }
.service-item__main { display: grid; gap: 10px; }
.service-item__head { display: flex; justify-content: space-between; gap: 12px; }
.service-item__head p { margin: 4px 0 0; color: var(--text-tertiary); font-size: 12px; }
.service-item__meta { display: grid; gap: 6px; color: var(--text-secondary); font-size: 13px; }
.service-item__action { display: flex; align-items: center; gap: 10px; }
.dialog-card { padding: 16px; border-radius: 18px; background: linear-gradient(180deg, #fff, #F9FCFF); border: 1px solid var(--border-soft); }
@media (max-width: 960px) { .admin-hero { grid-template-columns: 1fr; } .toolbar, .service-item, .service-item__head, .service-item__action { display: flex; flex-direction: column; align-items: stretch; } }
</style>

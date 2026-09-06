<template>
  <div class="page-shell">
    <section class="dashboard-hero">
      <div class="dashboard-hero__main">
        <h1>服务中心</h1>
        <div class="hero-actions">
          <el-input
            v-model="keyword"
            class="hero-search__input"
            size="large"
            placeholder="搜索服务、类型或标签"
            clearable
          >
            <template #prefix>
              <el-icon>
                <Search />
              </el-icon>
            </template>
          </el-input>
          <el-select
            v-model="status"
            class="hero-search__select"
            size="large"
          >
            <el-option
              label="全部状态"
              value=""
            />
            <el-option
              label="可预约"
              value="available"
            />
            <el-option
              label="维护中"
              value="maintenance"
            />
          </el-select>
        </div>
      </div>
      <div class="dashboard-hero__panel">
        <div class="hero-panel__label">
          服务概览
        </div>
        <div class="hero-panel__item">
          <strong>{{ services.length }}</strong><span>目录规模</span>
        </div>
        <div class="hero-panel__item">
          <strong>{{ availableCount }}</strong><span>可用服务</span>
        </div>
        <div class="hero-panel__item">
          <strong>{{ categoryCount }}</strong><span>业务分类</span>
        </div>
      </div>
    </section>

    <section
      v-if="!loading"
      class="cat-bar"
    >
      <div class="campus-toggle">
        <button
          v-for="c in campusOptions"
          :key="c.value"
          type="button"
          class="campus-card"
          :class="{ 'is-active': activeCampus === c.value }"
          :style="campusBg(c.value)"
          @click="activeCampus = c.value"
        >
          <span
            v-if="activeCampus === c.value"
            class="campus-check"
          >当前校区</span>
          <strong>{{ c.label }}</strong>
          <span>{{ c.desc }}</span>
        </button>
      </div>
      <div class="cat-row">
        <el-segmented
          v-model="activeCat"
          :options="catOptions"
          @change="syncCategoryRoute"
        />
      </div>
    </section>

    <section
      v-if="loading"
      class="loading-state"
    >
      <el-icon class="is-loading">
        <Loading />
      </el-icon>
      <span>加载中...</span>
    </section>
    <section
      v-else
      class="resource-grid"
    >
      <article
        v-for="item in filteredServices"
        :key="item.id"
        class="resource-card"
      >
        <div class="resource-card__pulse" />
        <img
          v-if="item.imageUrl"
          :src="assetUrl(item.imageUrl)"
          class="service-cover"
          alt="封面"
        >
        <div
          v-else
          class="cover-badge"
          :class="item.image"
        >
          {{ item.code }}
        </div>
        <div
          class="section-head"
          style="margin-bottom: 8px;"
        >
          <h3 class="section-head__title">
            {{ item.name }}
          </h3>
          <el-tag :type="item.status === 'available' ? 'success' : 'warning'">
            {{ item.status === 'available' ? '可预约' : '维护中' }}
          </el-tag>
        </div>
        <p class="muted">
          {{ item.description }}
        </p>
        <div class="tag-wrap">
          <el-tag
            v-for="tag in item.tags"
            :key="tag"
            round
          >
            {{ tag }}
          </el-tag>
        </div>
        <div class="info-list">
          <div class="info-row">
            <span>业务类别</span><strong>{{ item.category }}</strong>
          </div>
        </div>
        <div class="button-row">
          <el-button
            type="primary"
            @click="goService(item.id)"
          >
            查看详情
          </el-button>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import cqImg from '@/assets/images/campus/cq.jpg'
import xsImg from '@/assets/images/campus/xs.jpg'
import { fetchServiceCards, fetchServiceCategories, type ServiceCategoryOption } from '@/common/campus'
import type { ServiceCard } from '@/common/types'

const router = useRouter()
const route = useRoute()
const keyword = ref('')
const status = ref('')
const activeCampus = ref('cq')
const activeCat = ref('')
const services = ref<ServiceCard[]>([])
const loading = ref(false)

const campusOptions = [
  { value: 'cq', label: '仓前校区', desc: '勤园 · 恕园教学楼' },
  { value: 'xs', label: '下沙校区', desc: 'A–E 教学楼' },
]

/** 左蓝右图的校区卡：左侧蓝底保证文字清晰，右侧实景图，向右渐淡 */
const campusBgs: Record<string, string> = { cq: cqImg, xs: xsImg }
function campusBg(value: string) {
  return {
    backgroundImage: `linear-gradient(90deg, rgba(12, 63, 150, 0.92) 0%, rgba(20, 106, 214, 0.78) 34%, rgba(63, 182, 255, 0.25) 62%, rgba(63, 182, 255, 0) 100%), url(${campusBgs[value]})`,
    backgroundSize: 'cover',
    backgroundPosition: 'center right',
  }
}

/** 当前校区下的服务 */
const campusServices = computed(() => services.value.filter(s => !s.campus || s.campus === activeCampus.value))

/** 业务分类字典（后端 service_category 固定 4 类，展示名库驱动） */
const catDict = ref<ServiceCategoryOption[]>([])

/** 分类筛选选项：全部 + 当前校区真实存在且在后端字典里的分类（中文名来自库） */
const catOptions = computed(() => {
  const present = new Set(campusServices.value.map(s => s.catKey).filter((k): k is string => Boolean(k)))
  const byCode = new Map(catDict.value.map(c => [c.code, c.name]))
  const listed = [...present].filter(k => byCode.has(k)).map(k => ({ label: byCode.get(k)!, value: k }))
  // 字典里没有但旧数据真实存在的分类也保留（兜底展示 code）
  const extra = [...present].filter(k => !byCode.has(k)).map(k => ({ label: k, value: k }))
  return [{ label: '全部', value: '' }, ...listed, ...extra]
})

const filteredServices = computed(() =>
  campusServices.value.filter((item) => {
    const searchTarget = [item.name, item.category, item.description, ...item.tags].join('|')
    const matchKeyword = !keyword.value || searchTarget.toLowerCase().includes(keyword.value.toLowerCase())
    const matchStatus = !status.value || item.status === status.value
    const matchCat = !activeCat.value || item.catKey === activeCat.value
    return matchKeyword && matchStatus && matchCat
  }),
)

const availableCount = computed(() => campusServices.value.filter((item) => item.status === 'available').length)
const categoryCount = computed(() => new Set(campusServices.value.map(s => s.catKey).filter(Boolean)).size)

onMounted(async () => {
  loading.value = true
  try {
    const [svc, cats] = await Promise.all([fetchServiceCards(), fetchServiceCategories()])
    services.value = svc
    catDict.value = cats
    // 支持从分类磁贴 /services?category=xxx 直达
    const q = String(route.query.category || '')
    if (q && catOptions.value.some(o => o.value === q)) {
      activeCat.value = q
    }
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '获取服务列表失败')
  } finally {
    loading.value = false
  }
})

function syncCategoryRoute(value: string | number | boolean) {
  const cat = String(value)
  router.replace({ query: cat ? { category: cat } : {} })
}

function goService(id: number) {
  router.push(`/service/${id}`)
}

/** /uploads/xx → /api/uploads/xx（走 vite 代理） */
function assetUrl(path?: string) {
  if (!path) return ''
  if (/^https?:/.test(path)) return path
  if (path.startsWith('/uploads')) return `/api${path}`
  return path
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
.dashboard-hero__main h1 { margin: 12px 0 10px; font-size: 36px; line-height: 1.18; }
.dashboard-hero__main p { max-width: 720px; margin: 0; line-height: 1.8; color: rgba(255,255,255,0.84); }
.hero-actions { display: flex; align-items: center; gap: 14px; margin-top: 24px; }
.hero-actions .hero-search__input { flex: 1 1 280px; min-width: 0; max-width: 460px; }
.hero-actions .hero-search__select { width: 152px; flex-shrink: 0; }

/* 深色 hero 上的毛玻璃搜索/筛选：半透明白 + 白字，聚焦高亮 */
.dashboard-hero :deep(.el-input__wrapper),
.dashboard-hero :deep(.el-select__wrapper) {
  background: rgba(255, 255, 255, 0.13);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.26) inset;
  border-radius: 12px;
}
.dashboard-hero :deep(.el-input__wrapper:hover),
.dashboard-hero :deep(.el-select__wrapper:hover) {
  background: rgba(255, 255, 255, 0.18);
}
.dashboard-hero :deep(.el-input__wrapper.is-focus),
.dashboard-hero :deep(.el-select__wrapper.is-focused) {
  background: rgba(255, 255, 255, 0.2);
  box-shadow: 0 0 0 1.5px rgba(255, 255, 255, 0.85) inset, 0 10px 24px rgba(46, 12, 92, 0.28);
}
.dashboard-hero :deep(.el-input__inner),
.dashboard-hero :deep(.el-select__placeholder),
.dashboard-hero :deep(.el-select__selected-item),
.dashboard-hero :deep(.el-input__prefix),
.dashboard-hero :deep(.el-input__suffix),
.dashboard-hero :deep(.el-select__caret) {
  color: #fff;
}
.dashboard-hero :deep(.el-input__wrapper) {
  --el-input-placeholder-color: rgba(255, 255, 255, 0.6);
}
.dashboard-hero :deep(.el-input__clear) {
  color: rgba(255, 255, 255, 0.75);
}
.dashboard-hero :deep(.el-input__clear:hover) {
  color: #fff;
}
.dashboard-hero__panel { display: grid; gap: 12px; padding: 22px; border-radius: 22px; background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.12); backdrop-filter: blur(10px); }
.hero-panel__label { font-size: 13px; color: rgba(255,255,255,0.64); margin-bottom: 2px; }
.hero-panel__item { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.1); }
.hero-panel__item:last-child { border-bottom: none; }
.hero-panel__item strong { font-size: 20px; font-weight: 700; }
.hero-panel__item span { font-size: 13px; color: rgba(255,255,255,0.7); }

.loading-state { display: flex; align-items: center; justify-content: center; gap: 8px; padding: 40px; color: var(--text-secondary); }
.resource-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 20px; }
.resource-card { position: relative; padding: 22px; border-radius: 22px; background: rgba(255,255,255,.92); border: 1px solid var(--border-soft); box-shadow: var(--shadow-card); }
.resource-card__pulse { position: absolute; top: 0; left: 0; right: 0; height: 3px; border-radius: 22px 22px 0 0; background: linear-gradient(90deg, #ADE2FF, #7BD0FF); }
.service-cover { width: 100%; height: 150px; object-fit: cover; border-radius: 14px; border: 1px solid var(--border-soft); }
.cat-bar { display: grid; gap: 18px; margin: 22px 0 0; }
.campus-toggle { display: grid; grid-template-columns: repeat(auto-fit, minmax(230px, 1fr)); gap: 16px; }
.campus-card {
  position: relative;
  display: flex; flex-direction: column; justify-content: flex-end; gap: 4px; align-items: flex-start;
  min-height: 138px; padding: 22px; border-radius: 18px; text-align: left; cursor: pointer; overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.35); color: #fff;
  box-shadow: var(--shadow-card); transition: transform .2s ease, box-shadow .2s ease, outline-color .2s ease;
}
.campus-card::after {
  content: ''; position: absolute; inset: 0; pointer-events: none;
  background: linear-gradient(180deg, rgba(7, 41, 102, 0.05), rgba(7, 41, 102, 0.28));
}
.campus-card strong { position: relative; font-size: 19px; font-weight: 800; text-shadow: 0 1px 6px rgba(0, 0, 0, 0.18); }
.campus-card span { position: relative; font-size: 12px; color: rgba(255, 255, 255, 0.92); }
.campus-card:not(.is-active) { filter: saturate(0.72) brightness(0.82); }
.campus-card:not(.is-active):hover { filter: saturate(0.9) brightness(0.92); transform: translateY(-2px); }
.campus-card.is-active {
  outline: 3px solid #fff; outline-offset: -3px;
  box-shadow: 0 0 0 4px rgba(63, 182, 255, 0.9), 0 14px 30px rgba(30, 152, 242, 0.42);
  transform: translateY(-2px);
}
.campus-card .campus-check {
  position: absolute; top: 10px; right: 12px;
  background: rgba(255, 255, 255, 0.94); color: #1560C4;
  font-size: 11px; font-weight: 700; border-radius: 999px; padding: 3px 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}
.cat-row .el-segmented { background: #ECF8FF; border: 1px solid #D9F1FF; border-radius: 12px; }

@keyframes dashHalo { 0%,100% { transform: translate3d(0,0,0) scale(1); } 50% { transform: translate3d(-20px,-10px,0) scale(1.08); } }
@media (max-width: 900px) { .dashboard-hero { grid-template-columns: 1fr; } }
</style>
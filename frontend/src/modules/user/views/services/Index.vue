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
        <div
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
          <div class="info-row">
            <span>服务范围</span><strong>{{ item.location }}</strong>
          </div>
          <div class="info-row">
            <span>说明</span><strong>{{ item.priceLabel }}</strong>
          </div>
        </div>
        <div class="button-row">
          <el-button
            plain
            @click="router.push(`/service/${item.id}`)"
          >
            查看详情
          </el-button>
          <el-button
            type="primary"
            @click="goService(item.id)"
          >
            进入服务
          </el-button>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { fetchServiceCards } from '@/common/campus'
import type { ServiceCard } from '@/common/types'

const router = useRouter()
const keyword = ref('')
const status = ref('')
const services = ref<ServiceCard[]>([])
const loading = ref(false)

const filteredServices = computed(() =>
  services.value.filter((item) => {
    const searchTarget = [item.name, item.category, item.description, ...item.tags].join('|')
    const matchKeyword = !keyword.value || searchTarget.toLowerCase().includes(keyword.value.toLowerCase())
    const matchStatus = !status.value || item.status === status.value
    return matchKeyword && matchStatus
  }),
)

const availableCount = computed(() => services.value.filter((item) => item.status === 'available').length)
const categoryCount = computed(() => new Set(services.value.map(item => item.type)).size)

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

function goService(id: number) {
  router.push(`/service/${id}`)
}
</script>

<style scoped lang="scss">
.dashboard-hero {
  position: relative; display: grid; grid-template-columns: 1.2fr 0.8fr; gap: 20px;
  padding: 32px; border-radius: 30px; color: #fff;
  background: linear-gradient(135deg, #4c1d95, #7c3aed 62%, #a78bfa);
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
.resource-card__pulse { position: absolute; top: 0; left: 0; right: 0; height: 3px; border-radius: 22px 22px 0 0; background: linear-gradient(90deg, #a78bfa, #8b5cf6); }

@keyframes dashHalo { 0%,100% { transform: translate3d(0,0,0) scale(1); } 50% { transform: translate3d(-20px,-10px,0) scale(1.08); } }
@media (max-width: 900px) { .dashboard-hero { grid-template-columns: 1fr; } }
</style>
<template>
  <div class="page-shell">
    <section class="service-hero">
      <div class="service-hero__main">
        <p class="hz-eyebrow">
          Service Index · HZNU
        </p>
        <h1>服务索引</h1>
        <p class="service-hero__desc">
          按校区与类别，找到适合此刻的校园资源。教室、设备、咨询与活动，皆可在此预约。
        </p>

        <div class="service-hero__search">
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

        <div class="service-hero__note">
          <span class="hz-seal">杭</span>
          <span>仓前 · 下沙 双校区服务目录</span>
        </div>
      </div>

      <aside class="service-hero__ledger">
        <div class="ledger__head">
          <span>目录概览</span>
          <span>{{ currentYear }}</span>
        </div>
        <div class="ledger__row">
          <span>目录规模</span>
          <strong>{{ services.length }}</strong>
        </div>
        <div class="ledger__row">
          <span>可用服务</span>
          <strong>{{ availableCount }}</strong>
        </div>
        <div class="ledger__row">
          <span>业务分类</span>
          <strong>{{ categoryCount }}</strong>
        </div>
        <div class="ledger__foot">
          HZNU · SERVICE DIRECTORY
        </div>
      </aside>
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
      <span>正在整理服务目录…</span>
    </section>

    <section
      v-else-if="filteredServices.length"
      class="resource-grid"
    >
      <article
        v-for="(item, index) in filteredServices"
        :key="item.id"
        class="resource-card service-index-card"
      >
        <span class="service-index-card__no">{{ String(index + 1).padStart(2, '0') }}</span>
        <img
          v-if="item.imageUrl"
          :src="assetUrl(item.imageUrl)"
          class="service-cover"
          alt="封面"
        >
        <div
          v-else
          class="cover-badge service-cover"
          :class="item.image"
        >
          {{ item.code }}
        </div>
        <div class="section-head">
          <h3 class="section-head__title">
            {{ item.name }}
          </h3>
          <el-tag :type="item.status === 'available' ? 'success' : 'warning'">
            {{ item.status === 'available' ? '可预约' : '维护中' }}
          </el-tag>
        </div>
        <p class="muted service-index-card__desc">
          {{ item.description }}
        </p>
        <div class="tag-wrap">
          <el-tag
            v-for="tag in item.tags"
            :key="tag"
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

    <section
      v-else
      class="empty-state service-empty"
    >
      <span class="service-empty__mark">空</span>
      <p>这个筛选条件下暂时没有服务。<br>换个分类，或切换校区再看看。</p>
      <el-button
        @click="resetFilters"
      >
        重置筛选
      </el-button>
    </section>
    <CampusStory compact />

  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Loading } from '@element-plus/icons-vue'
import CampusStory from '@/components/CampusStory.vue'
import cqImg from '@/assets/images/campus/cq-library.jpg'
import xsImg from '@/assets/images/campus/xs-lake.jpg'
import { fetchServiceCards, fetchServiceCategories, type ServiceCategoryOption } from '@/common/campus'
import type { ServiceCard } from '@/common/types'
import { assetUrl } from '@/common/utils/asset'

const router = useRouter()
const route = useRoute()
const keyword = ref('')
const status = ref('')
const activeCampus = ref('cq')
const activeCat = ref('')
const services = ref<ServiceCard[]>([])
const loading = ref(false)

const currentYear = new Date().getFullYear()

const campusOptions = [
  { value: 'cq', label: '仓前校区', desc: '叠书成塔 · 余杭塘河' },
  { value: 'xs', label: '下沙校区', desc: '蕙风湖 · 钱塘江畔' },
]

/** 墨色遮罩 + 实景图：左侧压暗保证文字清晰，右侧保留校园细节 */
const campusBgs: Record<string, string> = { cq: cqImg, xs: xsImg }
function campusBg(value: string) {
  return {
    backgroundImage: `linear-gradient(90deg, rgba(13, 32, 39, 0.92) 0%, rgba(21, 50, 60, 0.7) 38%, rgba(21, 50, 60, 0.2) 72%, rgba(21, 50, 60, 0.04) 100%), url(${campusBgs[value]})`,
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

function resetFilters() {
  keyword.value = ''
  status.value = ''
  activeCat.value = ''
  router.replace({ query: {} })
}
</script>

<style scoped lang="scss">
.service-hero {
  position: relative;
  display: grid;
  grid-template-columns: 1.35fr 0.65fr;
  gap: 34px;
  padding: 40px 42px;
  border-radius: var(--radius-2xl);
  border: 1px solid var(--line);
  background: var(--surface);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.service-hero::after {
  content: "索";
  position: absolute;
  right: 31%;
  bottom: -94px;
  font-family: var(--font-serif);
  font-size: 280px;
  line-height: 1;
  color: rgba(24, 49, 59, 0.028);
  pointer-events: none;
  user-select: none;
}

.service-hero__main {
  position: relative;
  z-index: 1;
}

.service-hero h1 {
  margin: 12px 0 10px;
  font-family: var(--font-serif);
  font-size: clamp(38px, 4vw, 54px);
  font-weight: 600;
  line-height: 1.2;
  letter-spacing: 0.05em;
  color: var(--ink);
}

.service-hero__desc {
  max-width: 560px;
  margin: 0;
  color: var(--ink-3);
  font-size: 13.5px;
  line-height: 1.9;
}

.service-hero__search {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  max-width: 660px;
  margin-top: 26px;
}

.service-hero__search .hero-search__input {
  flex: 1 1 300px;
  min-width: 0;
}

.service-hero__search .hero-search__select {
  width: 148px;
  flex-shrink: 0;
}

.service-hero__search :deep(.el-input__wrapper),
.service-hero__search :deep(.el-select__wrapper) {
  min-height: 44px;
  background: var(--surface-2);
}

.service-hero__note {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 26px;
  font-size: 11.5px;
  letter-spacing: 0.05em;
  color: var(--ink-3);
}

.service-hero__note .hz-seal {
  width: 30px;
  height: 30px;
  font-size: 15px;
}

.service-hero__ledger {
  position: relative;
  z-index: 1;
  align-self: center;
  padding: 22px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: var(--surface-2);
}

.ledger__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--line);
  font-family: var(--font-latin);
  font-size: 9px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--gold);
}

.ledger__row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid var(--line);
}

.ledger__row span {
  font-size: 12px;
  color: var(--ink-3);
}

.ledger__row strong {
  font-family: var(--font-serif);
  font-size: 26px;
  font-weight: 600;
  line-height: 1;
  color: var(--ink);
}

.ledger__foot {
  padding-top: 14px;
  font-family: var(--font-latin);
  font-size: 8px;
  letter-spacing: 0.16em;
  color: var(--ink-4);
}

.cat-bar {
  display: grid;
  gap: 18px;
}

.campus-toggle {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.campus-card {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 4px;
  min-height: 150px;
  padding: 22px;
  border-radius: 14px;
  border: 1px solid var(--line);
  font-family: var(--font-sans);
  text-align: left;
  color: #f7f1e4;
  cursor: pointer;
  overflow: hidden;
  box-shadow: var(--shadow-soft);
  transition:
    transform 0.18s var(--ease-out),
    filter 0.18s ease,
    box-shadow 0.18s ease;
}

.campus-card::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(180deg, rgba(13, 32, 39, 0.02), rgba(13, 32, 39, 0.28));
}

.campus-card strong {
  position: relative;
  z-index: 1;
  font-family: var(--font-serif);
  font-size: 21px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.2);
}

.campus-card > span:not(.campus-check) {
  position: relative;
  z-index: 1;
  font-size: 11.5px;
  color: rgba(247, 241, 228, 0.78);
}

.campus-card:not(.is-active) {
  filter: saturate(0.62) brightness(0.82);
}

.campus-card:not(.is-active):hover {
  filter: saturate(0.82) brightness(0.94);
  transform: translateY(-2px);
}

.campus-card.is-active {
  outline: 2px solid var(--gold-soft);
  outline-offset: -2px;
  box-shadow: 0 18px 34px -22px rgba(24, 49, 59, 0.6);
  transform: translateY(-2px);
}

.campus-card .campus-check {
  position: absolute;
  top: 12px;
  right: 13px;
  z-index: 2;
  padding: 3px 9px;
  border-radius: 3px;
  background: rgba(247, 241, 228, 0.92);
  color: var(--lake);
  font-size: 9.5px;
  font-weight: 600;
  letter-spacing: 0.08em;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.cat-row {
  display: flex;
  overflow-x: auto;
  padding-bottom: 2px;
  scrollbar-width: none;
}

.cat-row::-webkit-scrollbar {
  display: none;
}

.cat-row :deep(.el-segmented) {
  background: var(--surface-2);
  border: 1px solid var(--line);
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  padding: 56px;
  color: var(--ink-3);
  font-size: 13px;
}

.loading-state :deep(.el-icon) {
  color: var(--lake);
}

.resource-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(300px, 100%), 1fr));
  gap: 20px;
}

.service-index-card {
  position: relative;
  padding: 18px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  background: var(--surface);
  box-shadow: var(--shadow-card);
  display: flex;
  flex-direction: column;
  transition:
    transform 0.18s var(--ease-out),
    box-shadow 0.18s var(--ease-out),
    border-color 0.18s ease;
}

.service-index-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-card-hover);
  border-color: rgba(176, 138, 69, 0.42);
}

.service-index-card__no {
  position: absolute;
  top: 26px;
  right: 26px;
  z-index: 2;
  padding: 3px 7px;
  border-radius: 3px;
  background: rgba(24, 49, 59, 0.42);
  color: rgba(247, 241, 228, 0.9);
  font-family: var(--font-latin);
  font-size: 9px;
  letter-spacing: 0.1em;
  backdrop-filter: blur(6px);
}

.service-cover {
  width: 100%;
  height: 156px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid var(--line);
  margin-bottom: 16px;
}

.cover-badge.service-cover {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #f7f1e4;
  font-family: var(--font-serif);
  font-size: 26px;
  letter-spacing: 0.12em;
}

.service-index-card .section-head {
  margin-bottom: 8px;
}

.service-index-card .section-head__title {
  font-size: 17px;
}

.service-index-card__desc {
  margin: 0;
  flex: 1;
  font-size: 12.5px;
  line-height: 1.8;
}

.tag-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.tag-wrap :deep(.el-tag) {
  font-size: 10px;
  padding: 0 8px;
  height: 22px;
}

.service-index-card .info-list {
  margin-top: 14px;
}

.service-index-card .info-row {
  padding-top: 10px;
}

.service-index-card .button-row {
  margin-top: auto;
  padding-top: 14px;
}

.service-index-card .button-row :deep(.el-button) {
  width: 100%;
}

.service-empty {
  padding: 64px 20px;
  border: 1px dashed var(--line-strong);
  border-radius: var(--radius-lg);
  background: rgba(255, 253, 247, 0.5);
}

.service-empty__mark {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border: 1px solid var(--line-strong);
  border-radius: 50%;
  font-family: var(--font-serif);
  font-size: 18px;
  color: var(--ink-4);
}

.service-empty p {
  margin: 0;
  font-size: 13px;
  line-height: 1.9;
  color: var(--ink-3);
  text-align: center;
}

@media (max-width: 980px) {
  .service-hero {
    grid-template-columns: 1fr;
    gap: 26px;
    padding: 30px;
  }

  .service-hero__ledger {
    align-self: stretch;
  }

  .service-hero::after {
    right: -20px;
    font-size: 220px;
  }
}

@media (max-width: 600px) {
  .service-hero {
    padding: 24px 22px;
    border-radius: var(--radius-xl);
  }

  .service-hero__search .hero-search__select {
    width: 100%;
  }

  .campus-card {
    min-height: 132px;
  }
}
</style>

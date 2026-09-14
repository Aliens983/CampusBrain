import { ref } from 'vue'
import { fetchServiceCategories } from '@/common/campus'

/**
 * 校区 / 业务分类字典。
 * <p>
 * 1.3.1：业务分类（teacher/equipment/space/activity）统一以后端
 * {@code GET /app/service-categories} 下发的字典为准，前端不再在各页面
 * 各写一份硬编码；校区目前无独立字典接口，保留前端常量兜底（与后端
 * AppointmentAssistantServiceImpl 的兜底一致）。
 */

/** 校区编码 → 中文名（后端暂无校区字典接口，前端兜底） */
export const CAMPUS_LABEL: Record<string, string> = {
  cq: '仓前校区',
  xs: '下沙校区',
}

export function campusLabel(code?: string): string {
  if (!code) return ''
  return CAMPUS_LABEL[code] || code
}

/** 分类兜底字典：接口未返回 / 请求失败时使用，避免界面出现裸 code */
const FALLBACK_CATEGORY_LABEL: Record<string, string> = {
  teacher: '教师咨询',
  equipment: '设备借用',
  space: '教室空间',
  activity: '活动报名',
}

let inflight: Promise<Record<string, string>> | null = null

// 响应式字典：接口返回后替换 map，依赖它的计算属性会自动刷新
const categoryMapRef = ref<Record<string, string>>({ ...FALLBACK_CATEGORY_LABEL })

/**
 * 拉取后端分类字典（全局只请求一次，多处复用同一个 Promise）。
 * 失败时静默沿用兜底字典，不阻塞页面渲染。
 */
export function loadCategoryDictionary(): Promise<Record<string, string>> {
  if (inflight) return inflight
  inflight = fetchServiceCategories()
    .then((list) => {
      const map: Record<string, string> = { ...FALLBACK_CATEGORY_LABEL }
      for (const item of list) {
        if (item?.code) map[item.code] = item.name
      }
      categoryMapRef.value = map
      return map
    })
    .catch(() => categoryMapRef.value)
  return inflight
}

export function categoryLabel(code?: string): string {
  if (!code) return ''
  return categoryMapRef.value[code] || FALLBACK_CATEGORY_LABEL[code] || code
}

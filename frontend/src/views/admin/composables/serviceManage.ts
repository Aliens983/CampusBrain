import { computed, reactive, ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/common/utils/request'
import { fetchAdminServicesPage, fetchServiceCategories, type ServiceCategoryOption } from '@/common/campus'
import type { ServiceCard } from '@/common/types'
import { assetUrl } from '@/common/utils/asset'

/** 校区 Tab 选项：'' 全部 / cq 仓前 / xs 下沙 */
const CAMPUS_OPTIONS = [
  { label: '全部', value: '' },
  { label: '仓前校区', value: 'cq' },
  { label: '下沙校区', value: 'xs' }
]

/**
 * 服务治理页逻辑：列表查询（服务端分页/筛选）、统计计数、编辑与新增。
 * <p>
 * 原集中在 ServiceManage.vue（580 行）内，页面既管布局又管四个请求，
 * 这里收敛为单一 composable，页面只负责组装。
 */
export function useServiceManage() {
  // ===== 列表与筛选 =====
  /** 当前服务端返回的一页服务（不再前端过滤/切片） */
  const services = ref<ServiceCard[]>([])
  /** 当前查询命中总数（分页用） */
  const total = ref(0)
  const keyword = ref('')
  const statusFilter = ref('')
  const campusFilter = ref('')
  const pageNo = ref(1)
  const pageSize = ref(5)
  const loading = ref(false)

  // ===== 顶部统计与校区 Tab 计数（与列表筛选解耦） =====
  const availableTotal = ref(0)
  const campusCounts = ref<Record<string, number>>({ '': 0, cq: 0, xs: 0 })

  // ===== 分类字典（来自后端 service_category） =====
  const categories = ref<ServiceCategoryOption[]>([])

  // ===== 抽屉与表单 =====
  const createDrawer = ref(false)
  const selectedService = ref<ServiceCard | null>(null)
  const saving = ref(false)
  const editForm = reactive({ name: '', category: '', categoryId: 0, description: '', image: '' })
  const createForm = reactive({
    name: '', categoryId: 0, campus: 'cq', capacity: -1,
    description: '', location: '', image: ''
  })

  // 由选中项推导抽屉开关，关闭时清空选中
  const serviceDrawerVisible = computed({
    get: () => Boolean(selectedService.value),
    set: (value: boolean) => { if (!value) selectedService.value = null }
  })

  /** 各校区 Tab 计数取独立统计（不受当前名称/状态筛选影响） */
  const campusCount = (value: string) => campusCounts.value[value] ?? 0

  /** 分页、名称搜索、状态、校区全部下沉到服务端 */
  async function loadServices() {
    loading.value = true
    try {
      const page = await fetchAdminServicesPage({
        pageNo: pageNo.value,
        pageSize: pageSize.value,
        serviceName: keyword.value,
        // 可用=上架(1) / 维护中=下架(0)；'' 不传
        serviceState: statusFilter.value === 'available' ? 1
          : statusFilter.value === 'maintenance' ? 0 : undefined,
        campus: campusFilter.value || undefined
      })
      services.value = page.records
      total.value = page.total
    } catch (error: unknown) {
      ElMessage.error((error as { message?: string }).message || '获取服务列表失败')
    } finally {
      loading.value = false
    }
  }

  /** 统计卡与校区 Tab 计数：只取各条件下的 total（pageSize=1），不随列表筛选变化 */
  async function loadCounts() {
    try {
      const [all, available, cq, xs] = await Promise.all([
        fetchAdminServicesPage({ pageNo: 1, pageSize: 1 }),
        fetchAdminServicesPage({ pageNo: 1, pageSize: 1, serviceState: 1 }),
        fetchAdminServicesPage({ pageNo: 1, pageSize: 1, campus: 'cq' }),
        fetchAdminServicesPage({ pageNo: 1, pageSize: 1, campus: 'xs' })
      ])
      total.value = all.total
      availableTotal.value = available.total
      campusCounts.value = { '': all.total, cq: cq.total, xs: xs.total }
    } catch (error) {
      // 计数失败不阻塞列表，但保留可观测性日志
      console.error('[serviceManage] 统计计数加载失败', error)
    }
  }

  async function onPageChange(page: number) {
    pageNo.value = page
    await loadServices()
  }
  async function onPageSizeChange(size: number) {
    pageSize.value = size
    pageNo.value = 1
    await loadServices()
  }
  async function onSearch() {
    pageNo.value = 1
    await loadServices()
  }
  async function onCampusChange(value: string) {
    campusFilter.value = value
    pageNo.value = 1
    await loadServices()
  }

  // 状态下拉切换：回到第一页并重新查询
  watch(statusFilter, async () => {
    pageNo.value = 1
    await loadServices()
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
      ElMessage.error((error as { message?: string }).message || '封面上传失败')
    }
  }

  async function saveEdit() {
    if (!selectedService.value) return
    saving.value = true
    try {
      await request.put(`/admin/services/${selectedService.value.id}`, {
        serviceName: editForm.name,
        serviceDescribe: editForm.description,
        imageUrl: editForm.image || null,
        categoryId: editForm.categoryId || selectedService.value.categoryId || 0
      })
      ElMessage.success('服务修改成功')
      // 先关闭抽屉，再从服务端重取当前页与计数
      selectedService.value = null
      await Promise.all([loadServices(), loadCounts()])
    } catch (error: unknown) {
      ElMessage.error((error as { message?: string }).message || '修改失败')
    } finally {
      saving.value = false
    }
  }

  function resetCreateForm() {
    const space = categories.value.find((c) => c.code === 'space')
    createForm.name = ''
    createForm.categoryId = space?.id ?? categories.value[0]?.id ?? 0
    createForm.campus = 'cq'
    createForm.capacity = -1
    createForm.description = ''
    createForm.location = ''
    createForm.image = ''
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
        capacity: createForm.capacity
      })
      ElMessage.success('服务创建成功')
      createDrawer.value = false
      resetCreateForm()
      // 新增后回到第一页，从服务端重取列表与计数
      pageNo.value = 1
      await Promise.all([loadServices(), loadCounts()])
    } catch (error: unknown) {
      ElMessage.error((error as { message?: string }).message || '创建失败')
    } finally {
      saving.value = false
    }
  }

  onMounted(async () => {
    try {
      // 分类字典来自后端 service_category，默认选中「教室空间」
      categories.value = await fetchServiceCategories()
      const space = categories.value.find((c) => c.code === 'space')
      createForm.categoryId = space?.id ?? categories.value[0]?.id ?? 0
      await Promise.all([loadServices(), loadCounts()])
    } catch (error: unknown) {
      ElMessage.error((error as { message?: string }).message || '获取服务列表失败')
    }
  })

  return {
    services, total, keyword, statusFilter, campusFilter, pageNo, pageSize, loading,
    availableTotal, campusCounts, categories,
    createDrawer, selectedService, saving, editForm, createForm, serviceDrawerVisible,
    campusCount, onPageChange, onPageSizeChange, onSearch, onCampusChange,
    assetUrl, uploadImage, saveEdit, saveCreate,
    campusOptions: CAMPUS_OPTIONS
  }
}
